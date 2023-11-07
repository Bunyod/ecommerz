package com.bbr.commerz.inventory.http.utils

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.string.MatchesRegex

object refined {

  type Email       = String Refined MatchesRegex[W.`"""(?=[^\\s]+)(?=(\\w+)@([\\w\\.]+))"""`.T]
  type Name        = String Refined MatchesRegex["""[A-Z][a-z]+"""]
  type Age         = Int Refined NonNegative
  type PhoneNumber = String Refined MatchesRegex["^((00|\\+)33|0)([0-9]{5}|[0-9]{9})$"]

}
