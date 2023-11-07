package com.bbr.commerz.sales.http.transaction

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import com.bbr.commerz.organization.suite.HttpTestSuite
import com.bbr.commerz.sales.domain.order.OrderPayloads.OrderId
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads._
import com.bbr.commerz.sales.domain.transaction.{TransactionAlgebra, TransactionService}
import com.bbr.commerz.sales.http.utils.json._
import com.bbr.commerz.sales.suite.SaleGenerators
import com.bbr.platform.UuidOpts
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{PhoneNumber, StaffAuth, StaffId, StaffRole}
import com.bbr.platform.domain.Transaction.TransactionId
import eu.timepit.refined.api.Refined
import io.circe.syntax._
import org.http4s.Method._
import org.http4s.client.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.server.AuthMiddleware
import org.http4s.{MediaType, Status, Uri}

import java.util.UUID

object TransactionRoutesSpec extends HttpTestSuite with SaleGenerators {

  private def authMiddleware(staffAuth: StaffAuth): AuthMiddleware[IO, StaffAuth] =
    AuthMiddleware(Kleisli.pure(staffAuth))

  private val staffAuth: StaffAuth = StaffAuth(
    id = UUID.randomUUID().toStaffId,
    branchId = UUID.randomUUID().toBranchId.some,
    phoneNumber = PhoneNumber("+998907412586"),
    role = StaffRole.OWNER
  )

  def buildTransactionRequest(transaction: Transaction): TransactionRequest =
    TransactionRequest(
      orderId = transaction.orderId,
      branchId = transaction.branchId,
      deadline = transaction.deadline,
      paymentType = transaction.paymentType,
      totalAmount = transaction.amount,
      discount = transaction.discount.map(d => DiscountParam(Refined.unsafeApply(d.value)))
    )

  def createTransaction(newTransaction: Transaction): TransactionService[IO] = new TransactionService[IO](
    new TestTransactionRepository {
      override def create(transaction: Transaction): IO[Transaction]         = IO.pure(newTransaction)
      override def checkTransactionExistence(id: TransactionId): IO[Boolean] = false.pure[IO]
    }
  )
  def failingCreateTransaction(): TransactionService[IO]                     = new TransactionService[IO](
    new TestTransactionRepository {
      override def create(transaction: Transaction): IO[Transaction] =
        IO.raiseError(DummyError) *> IO.pure(transaction)

      override def checkTransactionExistence(id: TransactionId): IO[Boolean] = false.pure[IO]
    }
  )

  def getTransactionById(transaction: Transaction): TransactionService[IO] = new TransactionService[IO](
    new TestTransactionRepository {
      override def getById(orgId: OrgId, id: TransactionId): IO[Transaction] = IO.pure(transaction)
    }
  )
  def failingGetTransactionById(): TransactionService[IO]                  = new TransactionService[IO](
    new TestTransactionRepository {
      override def getById(orgId: OrgId, id: TransactionId): IO[Transaction] = IO.raiseError(DummyError)
    }
  )

  def getTransactions(transactions: List[Transaction]) = new TransactionService[IO](
    new TestTransactionRepository {
      override def getAll(
        orgId: OrgId,
        orderId: Option[OrderId],
        limit: Option[Int],
        offset: Option[Int]
      ): IO[List[Transaction]] = IO.pure(transactions)
    }
  )

  def failingGetTransactions(transactions: List[Transaction]) = new TransactionService[IO](
    new TestTransactionRepository {
      override def getAll(
        orgId: OrgId,
        orderId: Option[OrderId],
        limit: Option[Int],
        offset: Option[Int]
      ): IO[List[Transaction]] = IO.raiseError(DummyError) *> IO.pure(transactions)
    }
  )

  def deleteTransaction() = new TransactionService[IO](
    new TestTransactionRepository {
      override def deleteById(orgId: OrgId, transactionId: TransactionId): IO[String] =
        "Successfully deleted.".pure[IO]
    }
  )

  def failingDeleteTransaction() = new TransactionService[IO](
    new TestTransactionRepository {
      override def deleteById(orgId: OrgId, transactionId: TransactionId): IO[String] =
        IO.raiseError(DummyError) *> IO.pure("Successfully deleted")
    }
  )

  private val orgId: UUID = genOrgId.sample.get.value

  test("GET transaction by ID [OK]") {
    forall(genTransaction) { transaction =>
      val req    =
        GET(Uri.unsafeFromString(s"/org/$orgId/transaction/${transaction.id.value}"))
      val routes = new TransactionRoutes[IO](getTransactionById(transaction)).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(transaction, Status.Ok)
    }
  }

  test("GET transaction by ID [FAILURE]") {
    forall(genTransactionId) { id =>
      val req    =
        GET(Uri.unsafeFromString(s"/org/$orgId/transaction/${id.value}"))
      val routes = new TransactionRoutes[IO](failingGetTransactionById()).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("GET transactions by params [OK]") {
    forall(genTransactions) { transactions =>
      val req    = GET(Uri.unsafeFromString(s"/org/$orgId/transaction"))
      val routes = new TransactionRoutes[IO](getTransactions(transactions)).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(transactions, Status.Ok)
    }
  }

  test("GET transactions by params [FAILURE]") {
    forall(genTransactions) { transactions =>
      val req    = GET(Uri.unsafeFromString(s"/org/$orgId/transaction"))
      val routes = new TransactionRoutes[IO](failingGetTransactions(transactions))
        .routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("POST create transaction [OK]") {
    forall(genTransaction) { transaction =>
      val transactionRequest = buildTransactionRequest(transaction)
      val req                = POST(Uri.unsafeFromString(s"/org/$orgId/transaction"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(transactionRequest.asJson)
      val routes             = new TransactionRoutes[IO](createTransaction(transaction)).routes(authMiddleware(staffAuth))
      expectHttpBodyAndStatus(routes, req)(transaction, Status.Created)
    }
  }

  test("POST create transaction [FAILURE]") {
    forall(genTransaction) { transaction =>
      val transactionRequest = buildTransactionRequest(transaction)
      val req                = POST(Uri.unsafeFromString(s"/org/$orgId/transaction"))
        .withContentType(`Content-Type`(MediaType.application.json))
        .withEntity(transactionRequest.asJson)
      val routes             = new TransactionRoutes[IO](failingCreateTransaction()).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }

  test("DELETE transaction by ID [OK]") {
    forall(genTransactionId) { id =>
      val req    = DELETE(Uri.unsafeFromString(s"/org/$orgId/transaction/${id.value}"))
      val routes = new TransactionRoutes[IO](deleteTransaction()).routes(authMiddleware(staffAuth))
      expectHttpStatus(routes, req)(Status.Ok)
    }
  }

  test("DELETE transaction by ID [FAILURE]") {
    forall(genTransactionId) { id =>
      val req    = DELETE(Uri.unsafeFromString(s"/org/$orgId/transaction/${id.value}"))
      val routes = new TransactionRoutes[IO](failingDeleteTransaction()).routes(authMiddleware(staffAuth))
      expectHttpFailure(routes, req)
    }
  }
}

protected class TestTransactionRepository extends TransactionAlgebra[IO] {

  override def create(transaction: Transaction): IO[Transaction] = IO.pure(transaction)

  override def getById(orgId: OrgId, transactionId: TransactionId): IO[Transaction] = IO.pure(null)

  override def getAll(
    orgId: OrgId,
    orderId: Option[OrderId],
    limit: Option[Int],
    offset: Option[Int]
  ): IO[List[Transaction]] = IO.pure(List.empty)

  override def deleteById(orgId: OrgId, transactionId: TransactionId): IO[String] = "Deleted".pure[IO]

  override def checkTransactionExistence(transactionId: TransactionId): IO[Boolean] = true.pure[IO]

  override def checkOrganizationExistence(orgId: OrgId, staffId: StaffId): IO[Boolean] = true.pure[IO]
}
