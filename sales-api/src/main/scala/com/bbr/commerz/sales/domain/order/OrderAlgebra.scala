package com.bbr.commerz.sales.domain.order

import com.bbr.commerz.sales.domain.order.OrderPayloads.{Order, OrderId, OrderStatus}
import com.bbr.platform.domain.Organization.OrgId

trait OrderAlgebra[F[_]] {
  def create(order: Order): F[Order]
  def setReturnedProducts(order: Order): F[Order]
  def updateById(order: Order): F[Order]
  def updateStatus(orgId: OrgId, orderId: OrderId, status: OrderStatus): F[Unit]
  def getById(orgId: OrgId, orderId: OrderId): F[Order]
  def getAll(orgId: OrgId, limit: Int, offset: Int): F[List[Order]]
  def deleteById(orgId: OrgId, orderId: OrderId): F[Unit]
}
