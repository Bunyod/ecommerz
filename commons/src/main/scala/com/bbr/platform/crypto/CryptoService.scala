package com.bbr.platform.crypto

import cats.effect.Sync
import cats.implicits._
import com.bbr.platform.config.Configuration.PasswordSalt
import com.bbr.platform.crypto.Ciphers.{DecryptCipher, EncryptCipher}
import com.bbr.platform.domain.Staff.{EncryptedPassword, Password}

import java.util.Base64
import javax.crypto._
import javax.crypto.spec._

object CryptoService {
  def make[F[_]: Sync](passwordSalt: PasswordSalt): F[CryptoAlgebra] =
    Sync[F]
      .delay {
        val KEY      = "CastleOfPrivacy!"
        val salt     = passwordSalt.value.value.getBytes("UTF-8")
        val keySpec  = new PBEKeySpec(KEY.toCharArray, salt, 65536, 256)
        val factory  = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val bytes    = factory.generateSecret(keySpec).getEncoded
        val sKeySpec = new SecretKeySpec(bytes, "AES")
        val eCipher  = EncryptCipher(Cipher.getInstance("AES"))
        eCipher.value.init(Cipher.ENCRYPT_MODE, sKeySpec)
        val dCipher  = DecryptCipher(Cipher.getInstance("AES"))
        dCipher.value.init(Cipher.DECRYPT_MODE, sKeySpec)
        (eCipher, dCipher)
      }
      .map { case (ec, dc) =>
        new CryptoService(ec, dc)
      }
}

final class CryptoService private (
  eCipher: EncryptCipher,
  dCipher: DecryptCipher
) extends CryptoAlgebra {

  override def encrypt(password: Password): EncryptedPassword = {
    val bytes          = password.value.getBytes("UTF-8")
    val encryptedBytes = eCipher.value.doFinal(bytes)
    val encryptedValue = Base64.getEncoder.encodeToString(encryptedBytes)
    EncryptedPassword(encryptedValue)
  }

  override def decrypt(password: EncryptedPassword): Password = {
    val encryptedBytes = Base64.getDecoder.decode(password.value)
    val decryptedBytes = dCipher.value.doFinal(encryptedBytes)
    val decryptedValue = new String(decryptedBytes, "UTF-8")
    Password(decryptedValue)
  }

}
