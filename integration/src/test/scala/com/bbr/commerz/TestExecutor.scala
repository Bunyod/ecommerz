package com.bbr.commerz

import com.bbr.commerz.auth.domain.auth.AuthServiceItSpec
import com.bbr.commerz.auth.domain.staff.StaffServiceItSpec
import com.bbr.commerz.inventory.domain.category.CategoryServiceItSpec
import com.bbr.commerz.inventory.domain.product.ProductServiceItSpec
import com.bbr.commerz.inventory.domain.unit.UnitServiceItSpec
import com.bbr.commerz.organization.domain.branch.BranchServiceItSpec
import com.bbr.commerz.organization.domain.organization.OrganizationServiceItSpec
import com.bbr.commerz.sales.domain.agent.client.AgentClientServiceItSpec
import com.bbr.commerz.sales.domain.order.OrderServiceItSpec
import com.bbr.commerz.sales.domain.transaction.TransactionServiceItSpec

object TestExecutor
  extends AuthServiceItSpec
  with StaffServiceItSpec
  with CategoryServiceItSpec
  with ProductServiceItSpec
  with UnitServiceItSpec
  with BranchServiceItSpec
  with OrganizationServiceItSpec
  with AgentClientServiceItSpec
  with OrderServiceItSpec
  with TransactionServiceItSpec {}
