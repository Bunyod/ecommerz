package com.bbr.commerz.sales.suite

import cats.implicits._
import com.bbr.commerz.inventory.domain.product.ProductPayloads.{CartProduct, Quantity}
import com.bbr.commerz.inventory.suite.InventoryGenerators
import com.bbr.commerz.sales.domain.agent.AgentPayloads._
import com.bbr.commerz.sales. domain.cart.CartPayloads.{Cart, CartTotal}
import com.bbr.commerz.sales.domain.order.OrderPayloads._
import com.bbr.commerz.sales.domain.transaction.TransactionPayloads._
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.StaffId
import com.bbr.platform.domain.Transaction.TransactionId
import eu.timepit.refined.api.Refined
import org.scalacheck.Gen
import squants.market.USD

trait SaleGenerators extends InventoryGenerators {

  def genTransactionId: Gen[TransactionId]        = Gen.uuid.map(TransactionId.apply)
  def genOrderId: Gen[OrderId]                    = Gen.uuid.map(OrderId.apply)
  def genAgentClientId: Gen[ClientId]             = Gen.uuid.map(ClientId.apply)
  def genTransactionIds: Gen[List[TransactionId]] = Gen.listOfN(10, genTransactionId)

  def genPaymentType: Gen[PaymentType] = Gen.oneOf(PaymentType.values)

  def genTotalAmount: Gen[TotalAmount] =
    genDoubleRange(1000000).map(n => USD(BigDecimal(n))).map(TotalAmount.apply)

  def genRefundedAmount: Gen[RefundedAmount] =
    genDoubleRange(1000000).map(n => USD(BigDecimal(n))).map(RefundedAmount.apply)

  def genPaidAmount: Gen[PaidAmount] =
    genDoubleRange(1000000).map(n => USD(BigDecimal(n))).map(PaidAmount.apply)

  def genUnpaidAmount: Gen[UnpaidAmount] =
    genDoubleRange(1000000).map(n => USD(BigDecimal(n))).map(UnpaidAmount.apply)

  def genDiscountParam: Gen[DiscountParam] =
    for {
      value <- Gen.choose(1, 100)
    } yield DiscountParam(Refined.unsafeApply(value))

  def genTransaction: Gen[Transaction] =
    for {
      transactionId <- genTransactionId
      orgId         <- genOrgId
      staffId       <- genStaffId
      orderId       <- genOrderId
      branchId      <- Gen.option(genBranchId)
      products <- Gen.mapOfN(3, genProductQuantityPair)
      paymentType   <- genPaymentType
      amount        <- genTotalAmount
      discount      <- Gen.option(genDiscountParam)
      deadline      <- genLocalDateTime
      createdAt     <- genLocalDateTime
      updatedAt     <- genLocalDateTime
    } yield Transaction(
      id = transactionId,
      orgId = orgId,
      staffId = staffId,
      orderId = orderId,
      branchId = branchId,
      products = products,
      paymentType = paymentType,
      amount = amount,
      discount = discount.map(_.toDomain),
      deadline = deadline.some,
      createdAt = createdAt.some,
      updatedAt = updatedAt.some
    )

  def genCartProduct: Gen[CartProduct] =
    for {
      p <- genProduct
      q <- genQuantity
    } yield CartProduct(p, q)

  def genCartProducts: Gen[List[CartProduct]] = Gen.listOfN(3, genCartProduct)

  def genTransactionRequest: Gen[TransactionRequest] =
    for {
      orderId     <- genOrderId
      branchId    <- Gen.option(genBranchId)
      deadline    <- Gen.option(genLocalDateTime)
      paymentType <- genPaymentType
      totalAmount <- genMoney.map(TotalAmount.apply)
      discount    <- Gen.option(genDiscountParam)
    } yield TransactionRequest(orderId, branchId, deadline, paymentType, totalAmount, discount)

  def genCartTotal: Gen[CartTotal] =
    for {
      cartProduct <- Gen.listOfN(4, genCartProduct)
      money       <- Gen.posNum[Long].map(n => USD(BigDecimal(n)))
    } yield CartTotal(cartProduct, money)

  def genCart: Gen[Cart] = Gen.mapOfN(3, genProductQuantityPair).map(Cart.apply)

  def genCheckoutProps: Gen[(StaffId, OrderId, CartTotal, PaymentType)] = for {
    staffId     <- genStaffId
    orderId     <- Gen.uuid.map(OrderId.apply)
    cartTotal   <- genCartTotal
    paymentType <- genPaymentType
  } yield (staffId, orderId, cartTotal, paymentType)

  def genProductQuantityPair: Gen[(ProductId, Quantity)] =
    for {
      productId <- genProductId
      quantity  <- genQuantity
    } yield (productId, quantity)

  def genQuantityWithReason: Gen[QuantityWithReason] =
    for {
      quantity <- genQuantity
      reason   <- Gen.oneOf(ReturnReason.values)
    } yield QuantityWithReason(quantity, reason)

  def genProductReasonPair: Gen[(ProductId, QuantityWithReason)] =
    for {
      productId <- genProductId
      pair      <- genQuantityWithReason
    } yield (productId, pair)

  def genOrderStatus: Gen[OrderStatus]     = Gen.oneOf(OrderStatus.values)
  def genPaymentStatus: Gen[PaymentStatus] = Gen.oneOf(PaymentStatus.values)

  def genOrderReturn: Gen[OrderReturn] =
    for {
      amount       <- Gen.option(genRefundedAmount)
      items        <- Gen.option(Gen.mapOfN(3, genProductReasonPair))
      deliveryTime <- Gen.option(genLocalDateTime)
    } yield OrderReturn(
      refundedAmount = amount,
      returnedItems = items,
      deliveryTime = deliveryTime
    )

  def genOrderUpdate: Gen[OrderUpdate] =
    for {
      products     <- Gen.option(Gen.mapOfN(3, genProductQuantityPair))
      paidAmount   <- Gen.option(genPaidAmount)
      amount       <- Gen.option(genRefundedAmount)
      deliveryTime <- Gen.option(genLocalDateTime)
    } yield OrderUpdate(
      products = products,
      paidAmount = paidAmount,
      refundedAmount = amount,
      deliveryTime = deliveryTime
    )

  def genOrder: Gen[Order] =
    for {
      id             <- genOrderId
      staffId        <- genStaffId
      orgId          <- genOrgId
      branchId       <- Gen.option(genBranchId)
      paymentStatus  <- genPaymentStatus
      orderStatus    <- genOrderStatus
      products       <- Gen.mapOfN(3, genProductQuantityPair)
      totalAmount    <- genMoney.map(TotalAmount.apply)
      paidAmount     <- genMoney.map(PaidAmount.apply)
      unpaidAmount   <- genMoney.map(UnpaidAmount.apply)
      refundedAmount <- Gen.option(genMoney.map(RefundedAmount.apply))
      returnedItems  <- Gen.option(Gen.mapOfN(3, genProductReasonPair))
      createdBy      <- genStaffId
      updatedBy      <- Gen.option(genStaffId)
      createdAt      <- Gen.option(genLocalDateTime)
      updatedAt      <- Gen.option(genLocalDateTime)
      deliveryTime   <- Gen.option(genLocalDateTime)
    } yield Order(
      id = id,
      staffId = staffId,
      orgId = orgId,
      branchId = branchId,
      paymentStatus = paymentStatus,
      orderStatus = orderStatus,
      products = products,
      totalAmount = totalAmount,
      paidAmount = paidAmount,
      unpaidAmount = unpaidAmount,
      refundedAmount = refundedAmount,
      returnedItems = returnedItems,
      createdBy = createdBy,
      updatedBy = updatedBy,
      createdAt = createdAt,
      updatedAt = updatedAt,
      deliveryTime = deliveryTime
    )

  def genAgentClientRequest: Gen[AgentClientRequest] =
    for {
      phoneNumber <- genPhoneNumberParam
      firstName   <- genFirstNameParam
      lastName    <- genLastNameParam
      address     <- genAddress
    } yield AgentClientRequest(phoneNumber, firstName, lastName, address)

  def genClientStatus: Gen[ClientStatus] = Gen.oneOf(ClientStatus.values)

  def genAgentClient: Gen[AgentClient] =
    for {
      id          <- genAgentClientId
      agentId     <- genStaffId
      orgId       <- genOrgId
      phoneNumber <- genPhoneNumber
      firstName   <- genFirstName
      lastName    <- genLastName
      address     <- genAddress
      status      <- genClientStatus
      createdBy   <- genStaffId
      updatedBy   <- Gen.option(genStaffId)
      createdAt   <- Gen.option(genLocalDateTime)
      updatedAt   <- Gen.option(genLocalDateTime)
    } yield AgentClient(
      id = id,
      agentId = agentId,
      orgId = orgId,
      phoneNumber = phoneNumber,
      firstName = firstName,
      lastName = lastName,
      address = address,
      status = status,
      createdBy = createdBy,
      updatedBy = updatedBy,
      createdAt = createdAt,
      updatedAt = updatedAt
    )

  def genAgentClientUpdate: Gen[AgentClientUpdate] =
    for {
      phoneNumber <- Gen.option(genPhoneNumberParam)
      firstName   <- Gen.option(genFirstNameParam)
      lastName    <- Gen.option(genLastNameParam)
      address     <- Gen.option(genAddress)
    } yield AgentClientUpdate(phoneNumber, firstName, lastName, address)

  def genOrderRequest: Gen[OrderRequest] =
    for {
      totalAmount  <- genMoney.map(TotalAmount.apply)
      products     <- Gen.mapOfN(3, genProductQuantityPair)
      branchId     <- Gen.option(genBranchId)
      deliveryTime <- Gen.option(genLocalDateTime)

    } yield OrderRequest(totalAmount, products, branchId, deliveryTime)

  def genOrders: Gen[List[Order]]             = Gen.listOfN(3, genOrder)
  def genAgentClients: Gen[List[AgentClient]] = Gen.listOfN(3, genAgentClient)
  def genTransactions: Gen[List[Transaction]] = Gen.listOfN(3, genTransaction)

}
