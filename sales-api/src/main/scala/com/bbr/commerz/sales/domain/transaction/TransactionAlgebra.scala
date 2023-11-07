package com.bbr.commerz.sales.domain.transaction

import com.bbr.commerz.sales.domain.order.OrderPayloads.OrderId
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads.Transaction
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.domain.Transaction.TransactionId

trait TransactionAlgebra[F[_]] {
  def create(transaction: Transaction): F[Transaction]
  def getById(orgId: OrgId, transactionId: TransactionId): F[Transaction]
  def getAll(
    orgId: OrgId,
    orderId: Option[OrderId],
    limit: Option[Int] = None,
    offset: Option[Int] = None
  ): F[List[Transaction]]
  def deleteById(orgId: OrgId, transactionId: TransactionId): F[String]
  def checkOrganizationExistence(orgId: OrgId, staffId: StaffId): F[Boolean]
  def checkTransactionExistence(transactionId: TransactionId): F[Boolean]
}
