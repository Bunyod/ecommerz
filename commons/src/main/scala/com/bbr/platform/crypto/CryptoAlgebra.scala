package com.bbr.platform.crypto

import com.bbr.platform.domain.Staff.{EncryptedPassword, Password}

trait CryptoAlgebra {
  def encrypt(value: Password): EncryptedPassword
  def decrypt(value: EncryptedPassword): Password
}
