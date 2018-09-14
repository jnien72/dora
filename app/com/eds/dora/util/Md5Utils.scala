package com.eds.dora.util

object Md5Utils {

  import java.math.BigInteger
  import java.security.{MessageDigest, NoSuchAlgorithmException}

  def getMD5(input: String): String = try {
    val md = MessageDigest.getInstance("MD5")
    val messageDigest = md.digest(input.getBytes)
    val number = new BigInteger(1, messageDigest)
    var hashtext = number.toString(16)
    while ( {
      hashtext.length < 32
    }) hashtext = "0" + hashtext
    hashtext
  } catch {
    case e: NoSuchAlgorithmException =>
      throw new RuntimeException(e)
  }

}
