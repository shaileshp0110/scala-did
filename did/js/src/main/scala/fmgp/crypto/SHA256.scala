package fmgp.crypto

import java.security.MessageDigest
import scala.scalajs.js.typedarray.Uint8Array
import fmgp.util.bytes2Hex

import typings.jsSha256.mod

object SHA256 {
  def digestToHex(str: String): String = bytes2Hex(digest(str))
  def digestToHex(data: Array[Byte]): String = bytes2Hex(digest(data))

  def digest(str: String): Array[Byte] = mod.sha256.array(str).map(_.toByte).toArray
  def digest(data: Array[Byte]): Array[Byte] = digest(String(data.map(_.toChar)))
}
