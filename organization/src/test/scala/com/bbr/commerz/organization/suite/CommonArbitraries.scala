package com.bbr.commerz.organization.suite

import com.bbr.platform.domain.Address.{Address, District, OrgAddress, Region}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.PhoneNumber
import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime}

trait CommonArbitraries {

  def genOrgId: Gen[OrgId] = Gen.uuid.map(OrgId.apply)

  def genPhoneNumber: Gen[PhoneNumber] = for {
    countryCode  <- Gen.choose(1, 99)    // Customize as needed for your country codes
    areaCode     <- Gen.choose(100, 999) // Customize as needed for your area codes
    exchangeCode <- Gen.choose(100, 999)
    lineNumber   <- Gen.choose(1000, 9999)
  } yield PhoneNumber(s"+$countryCode$areaCode$exchangeCode$lineNumber")

  def genString(n: Int): Gen[String] = Gen.listOfN(n, Gen.alphaChar).map(_.mkString)

  def genBoolean: Gen[Boolean] = Gen.oneOf(true, false)

  def genStringNoShorterThan(n: Int): Gen[String] =
    for {
      delta  <- Gen.choose(0, 10)
      result <- genString(n + delta)
    } yield result

  def genStringNoLongerThan(n: Int): Gen[String] =
    for {
      length <- Gen.choose(0, n - 1)
      result <- if (length == 0) Gen.const("") else genString(length)
    } yield result

  def genNonNegativeInt: Gen[Int] = arbitrary[Int].map(_.abs)

  def genNonNegativeLong: Gen[Long] = arbitrary[Long].map(_.abs)

  def genNonNegativeDouble: Gen[Double] = arbitrary[Double].map(_.abs)

  def genNonNegativeBigDecimal: Gen[BigDecimal] = genNonNegativeInt.map(v => BigDecimal(s"$v.00"))

  def genSmallInt: Gen[Int] = Gen.choose(1, 64)

  def genSmallLong: Gen[Long] = Gen.choose[Long](1, 64)
  
  def genDoubleRange(max: Double): Gen[Double] = Gen.choose[Double](1, max)

  def genShortString: Gen[String] =
    for {
      n   <- Gen.choose(1, 16)
      str <- genString(n)
    } yield str

  def genRefinedNEString: Gen[NonEmptyString] = genStringNoShorterThan(15).map(Refined.unsafeApply)

  def genLocalDateTime: Gen[LocalDateTime] =
    Gen
      .choose(
        LocalDateTime.now().minusYears(10),
        LocalDateTime.now()
      )
      .map(_.truncatedTo(ChronoUnit.MILLIS))

  def genLocalDate: Gen[LocalDate] =
    Gen.choose(
      LocalDate.now().minusYears(10),
      LocalDate.now()
    )

  def genURL: Gen[String] =
    for {
      http       <- Gen.oneOf(Seq("http", "https"))
      domain     <- genShortString
      domainType <- Gen.oneOf(Seq("com", "org"))
      path       <- Gen.listOfN(4, genShortString).map(_.mkString("/"))
    } yield http + "://" + domain + "." + domainType + "/" + path

  def genRegion: Gen[Region]         = Gen.oneOf(Region.values)
  def genDistrict: Gen[District]     = Gen.oneOf(District.values)
  def genOrgAddress: Gen[OrgAddress] = genStringNoShorterThan(4).map(v => OrgAddress.apply(Refined.unsafeApply(v)))

  def genAddress: Gen[Address] =
    for {
      r  <- genRegion
      d  <- genDistrict
      oa <- genOrgAddress
      gl <- Gen.option(genString(6))
    } yield Address(
      region = r,
      district = d,
      orgAddress = oa,
      guideLine = gl
    )

}
