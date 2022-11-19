package fmgp.crypto

import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.Payload
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.UnprotectedHeader
import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.StandardCharset
import com.nimbusds.jose.util.Pair
import com.nimbusds.jose.jwk.{Curve => JWKCurve}
import com.nimbusds.jose.jwk.{ECKey => JWKECKey}
import com.nimbusds.jose.jwk.gen.ECKeyGenerator

import fmgp.did.VerificationMethodReferenced
import fmgp.did.comm._
import fmgp.crypto.UtilsJVM.toJWKCurve
import fmgp.crypto.UtilsJVM.toJWK

import zio.json._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.chaining._
import scala.collection.convert._
import scala.collection.JavaConverters._

import java.util.Collections
import com.nimbusds.jose.crypto.impl.ECDH1PUCryptoProvider
import com.nimbusds.jose.crypto.impl.ECDH1PU
import javax.crypto.SecretKey

class MyECEncrypter(
    ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
    header: JWEHeader,
    // alg: JWEAlgorithm = JWEAlgorithm.ECDH_ES_A256KW,
    // enc: EncryptionMethod = EncryptionMethod.A256CBC_HS512
) {

  val curve = ecRecipientsKeys.collect(_._2.getCurve).toSet match {
    case theCurve if theCurve.size == 1 =>
      assert(Curve.ecCurveSet.contains(theCurve.head), "Curve not expected") // FIXME ERROR
      theCurve.head.toJWKCurve
    case _ => ??? // FIXME ERROR
  }

  val myECDHCryptoProvider = new MyECDHCryptoProvider(curve)

  /** TODO return errors:
    *   - com.nimbusds.jose.JOSEException: Invalid ephemeral public EC key: Point(s) not on the expected curve
    *   - com.nimbusds.jose.JOSEException: Couldn't unwrap AES key: Integrity check failed
    */
  def encrypt(clearText: Array[Byte]): EncryptedMessageGeneric = { // FIXME

    // Generate ephemeral EC key pair
    val ephemeralKeyPair: JWKECKey = new ECKeyGenerator(curve).generate()
    val ephemeralPublicKey = ephemeralKeyPair.toECPublicKey()
    val ephemeralPrivateKey = ephemeralKeyPair.toECPrivateKey()

    val updatedHeader: JWEHeader = new JWEHeader.Builder(header)
      .ephemeralPublicKey(new JWKECKey.Builder(curve, ephemeralPublicKey).build())
      .build()

    val sharedSecrets = ecRecipientsKeys.map { case (vmr, key) =>
      val use_the_defualt_JCA_Provider = null
      (vmr, ECDH.deriveSharedSecret(key.toJWK.toECPublicKey(), ephemeralPrivateKey, use_the_defualt_JCA_Provider))
    }

    myECDHCryptoProvider.encryptAUX(updatedHeader, sharedSecrets, clearText)
  }

}

class MyECDH1PUEncrypterMulti(
    sender: ECKey,
    ecRecipientsKeys: Seq[(VerificationMethodReferenced, ECKey)],
    header: JWEHeader,
) {

  val curve = ecRecipientsKeys.collect(_._2.getCurve).toSet match {
    case theCurve if theCurve.size == 1 =>
      assert(Curve.ecCurveSet.contains(theCurve.head), "Curve not expected") // FIXME ERROR
      theCurve.head.toJWKCurve
    case _ => ??? // FIXME ERROR
  }

  val myProvider = new MyECDH1PUCryptoProvider(curve)

  def encrypt(clearText: Array[Byte]): EncryptedMessageGeneric = {
    // Generate ephemeral EC key pair on the same curve as the consumer's public key
    val ephemeralKeyPair: JWKECKey = new ECKeyGenerator(curve).generate()
    val ephemeralPublicKey = ephemeralKeyPair.toECPublicKey()
    val ephemeralPrivateKey = ephemeralKeyPair.toECPrivateKey()

    // Add the ephemeral public EC key to the header
    val updatedHeader: JWEHeader =
      new JWEHeader.Builder(header).ephemeralPublicKey(new JWKECKey.Builder(curve, ephemeralPublicKey).build()).build()

    val sharedSecrets = ecRecipientsKeys.map { case (vmr, key) =>
      val use_the_defualt_JCA_Provider = null
      (
        vmr,
        ECDH1PU.deriveSenderZ(
          sender.toJWK.toECPrivateKey(),
          key.toJWK.toECPublicKey(),
          ephemeralPrivateKey,
          myProvider.getJCAContext().getKeyEncryptionProvider()
        )
      )
    }

    myProvider.encryptAUX(updatedHeader, sharedSecrets, clearText)
  }
}