package com.bbr.commerz.utils

import cats.effect._
import cats.effect.unsafe.implicits.global
import ciris.ConfigValue
import com.bbr.commerz.organization.domain.owner.OwnerPayloads._
import com.bbr.commerz.organization.domain.staff.StaffPayloads.{Email, FirstName, LastName, StaffStatus}
import com.bbr.commerz.organization.infrastructure.postgres.Drivers._
import com.bbr.commerz.utils.services.{Repositories, Services}
import com.bbr.platform.config.Configuration.ServiceConfig
import com.bbr.platform.crypto.{CryptoAlgebra, CryptoService}
import com.bbr.platform.domain.Organization.OrgId
import com.bbr.platform.domain.Staff.{Password, PhoneNumber, StaffRole}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._             // DON'T REMOVE IT
import doobie.implicits.javasql._              // DON'T REMOVE IT
import doobie.postgres.circe.jsonb.implicits._ // DON'T REMOVE IT
import doobie.util.transactor.Transactor.Aux
import doobie.util.update.Update0
import eu.timepit.refined.auto._
import eu.timepit.refined.pureconfig._         // DON'T REMOVE IT
import io.circe.syntax._
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

object ItUtils {

  val config: ServiceConfig =
    ConfigValue.default[ServiceConfig](ConfigSource.default.loadOrThrow[ServiceConfig]).load[IO].unsafeRunSync()

  val transactor: Aux[IO, Unit] =
    Transactor.fromDriverManager[IO](
      driver = config.postgres.driver,
      url = config.postgres.jdbcUrl.value,
      user = config.postgres.user.value,
      password = config.postgres.password.value,
      logHandler = None
    )

  val (repositories, services, cryptoService): (Repositories[IO], Services[IO], CryptoAlgebra) = {
    val cryptoService = CryptoService.make[IO](config.passwordSalt).unsafeRunSync()
    val repositories  = Repositories.make[IO](transactor)
    val services      = Services.make[IO](repositories, cryptoService)
    (repositories, services, cryptoService)
  }

  def createOwner(
    phoneNumber: PhoneNumber,
    email: Option[Email] = None,
    firstName: Option[FirstName] = None,
    lastName: Option[LastName] = None,
    birthDate: Option[LocalDate] = None,
    createdAt: Option[LocalDateTime] = None
  ): Owner = {
    val id: UUID        = UUID.randomUUID()
    val insert: Update0 =
      sql"""
         INSERT INTO OWNER (
           ID,
           ORGANIZATIONS,
           ROLE,
           PASSWORD,
           EMAIL,
           PHONE_NUMBER,
           FIRSTNAME,
           LASTNAME,
           BIRTHDATE,
           STATUS,
           CREATED_AT
        ) VALUES (
          $id,
          ${List.empty[OrgId].asJson},
          ${StaffRole.OWNER.entryName},
          ${cryptoService.encrypt(Password("password"))},
          ${email.map(_.value)},
          ${phoneNumber.value},
          ${firstName.map(_.value)},
          ${lastName.map(_.value)},
          ${birthDate.map(bd => java.sql.Date.valueOf(bd))},
          ${StaffStatus.ACTIVE.entryName},
          ${createdAt.map(Timestamp.valueOf)}
        )
      """.update

    val select: Query0[Owner] = sql"""SELECT * FROM OWNER WHERE ID = $id""".query

    insert.run.flatMap(_ => select.unique).transact(ItUtils.transactor).unsafeRunSync()
  }

}
