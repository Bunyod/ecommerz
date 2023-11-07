package com.bbr.commerz.organization.http.utils

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.W

object refined {

  type EmailPred       = String Refined MatchesRegex[W.`""".*@.*[a-zA-Z0-9].*\\..*"""`.T]
  type Name            = String Refined MatchesRegex["""[A-Z][a-z]+"""]
  type PhoneNumberPred = String Refined MatchesRegex["^\\+?\\d{6,18}$"]

}
