package com.bbr.commerz.sales.domain.cart

import com.bbr.commerz.inventory.domain.product.ProductPayloads.Quantity
import com.bbr.commerz.inventory.domain.product.ProductPayloads._
import com.bbr.platform.domain.Product.ProductId
import com.bbr.platform.domain.Staff.StaffId
import squants.market.Money

import scala.util.control.NoStackTrace

object CartPayloads {

  case class Cart(products: Map[ProductId, Quantity])
  case class CartTotal(products: List[CartProduct], total: Money)
  case class CartNotFound(staffId: StaffId) extends NoStackTrace

}
