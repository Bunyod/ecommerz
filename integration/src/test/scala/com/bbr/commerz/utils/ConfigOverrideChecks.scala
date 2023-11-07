package com.bbr.commerz.utils

import com.bbr.commerz.sales.suite.SaleGenerators
import org.scalatest.EitherValues
import weaver.IOSuite
import weaver.scalacheck.{CheckConfig, Checkers}

trait MetaSuite extends IOSuite with Checkers with EitherValues with SaleGenerators

trait ConfigOverrideChecks extends MetaSuite {

  override def checkConfig: CheckConfig = CheckConfig.default.copy(minimumSuccessful = 1, perPropertyParallelism = 1)

  def fakeTest(cls: Class[_]): Unit =
    pureTest(s"${"-" * 5} ${cls.getName} ${"-" * 5}")(expect(true))

}
