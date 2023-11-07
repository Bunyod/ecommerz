package com.bbr.commerz.auth.infrastructure

import com.bbr.commerz.auth.domain.auth.AuthPayloads.RecoveryData
import com.bbr.platform.domain.Staff._
import doobie._

object Drivers {

  implicit val readRecoveryData: Read[RecoveryData] =
    Read[(String, Int)].map { case (phoneNumber, code) => RecoveryData(PhoneNumber(phoneNumber), code) }

  implicit val writeRecoveryData: Write[RecoveryData] =
    Write[(String, Int)].contramap(data => (data.phoneNumber.value, data.recoveryCode))
}
