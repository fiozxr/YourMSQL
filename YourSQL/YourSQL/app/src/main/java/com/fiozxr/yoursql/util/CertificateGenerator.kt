package com.fiozxr.yoursql.util

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CertificateGenerator @Inject constructor() {

    companion object {
        const val KEY_ALIAS = "yoursql"
        const val KEYSTORE_TYPE = "PKCS12"
        const val KEYSTORE_FILE = "yoursql.p12"
        const val CERT_ALIAS = "yoursql_cert"
    }

    fun generateSelfSignedCertificate(outputDir: File): KeyStoreData {
        // Generate key pair
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        // Generate certificate
        val cert = generateCertificate(keyPair)

        // Create keystore
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
        keyStore.load(null, null)
        keyStore.setKeyEntry(
            KEY_ALIAS,
            keyPair.private,
            KEYSTORE_PASSWORD.toCharArray(),
            arrayOf(cert)
        )

        // Save keystore
        val keystoreFile = File(outputDir, KEYSTORE_FILE)
        FileOutputStream(keystoreFile).use { fos ->
            keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray())
        }

        // Export public certificate
        val certFile = File(outputDir, "yoursql.crt")
        certFile.writeText(
            "-----BEGIN CERTIFICATE-----\n" +
                    Base64.getEncoder().encodeToString(cert.encoded).chunked(64).joinToString("\n") +
                    "\n-----END CERTIFICATE-----"
        )

        return KeyStoreData(
            keystoreFile = keystoreFile,
            certificateFile = certFile,
            password = KEYSTORE_PASSWORD,
            alias = KEY_ALIAS
        )
    }

    fun loadKeyStore(keystoreFile: File): KeyStore? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
            FileInputStream(keystoreFile).use { fis ->
                keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray())
            }
            keyStore
        } catch (e: Exception) {
            null
        }
    }

    private fun generateCertificate(keyPair: KeyPair): X509Certificate {
        val issuer = X500Name("CN=YourSQL Self-Signed, O=YourSQL, L=Local, C=US")
        val serialNumber = BigInteger(64, SecureRandom())
        val notBefore = Date()
        val notAfter = Date(notBefore.time + 365 * 24 * 60 * 60 * 1000L) // 1 year

        val certBuilder = X509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            issuer,
            SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSA")
            .build(keyPair.private)

        return JcaX509CertificateConverter()
            .getCertificate(certBuilder.build(signer))
    }

    data class KeyStoreData(
        val keystoreFile: File,
        val certificateFile: File,
        val password: String,
        val alias: String
    )
}

private const val KEYSTORE_PASSWORD = "yoursql123"
