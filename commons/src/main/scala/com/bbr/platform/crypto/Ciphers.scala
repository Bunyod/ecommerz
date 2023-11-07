package com.bbr.platform.crypto

import javax.crypto.Cipher

object Ciphers {
  case class EncryptCipher(value: Cipher)
  case class DecryptCipher(value: Cipher)
}
