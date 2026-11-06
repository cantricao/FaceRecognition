package com.example.detectionexample.credentials.digital

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class SdJwtParserTest {

    private fun base64Url(str: String): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(str.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun parse_decodesIssuerPayloadAndDisclosures() {
        val header = base64Url("""{"alg":"ES256","typ":"vc+sd-jwt"}""")
        val payload = base64Url("""{"iss":"https://verifiablecredentials-pa.googleapis.com","vct":"UserInfoCredential","directClaim":"value"}""")
        val issuerJwt = "$header.$payload.dummySignature"

        val disclosureEmail = base64Url("""["salt1","email","jane.doe@gmail.com"]""")
        val disclosureName = base64Url("""["salt2","name","Jane Doe"]""")
        val disclosureVerified = base64Url("""["salt3","email_verified",true]""")
        val kbJwt = "${base64Url("""{"alg":"ES256"}""")}.${base64Url("""{"nonce":"testNonce"}""")}.dummySig"

        val rawSdJwt = "$issuerJwt~$disclosureEmail~$disclosureName~$disclosureVerified~$kbJwt"

        val claims = SdJwtParser.parse(rawSdJwt)

        assertEquals("https://verifiablecredentials-pa.googleapis.com", claims.getString("iss"))
        assertEquals("UserInfoCredential", claims.getString("vct"))
        assertEquals("value", claims.getString("directClaim"))
        assertEquals("jane.doe@gmail.com", claims.getString("email"))
        assertEquals("Jane Doe", claims.getString("name"))
        assertTrue(claims.getBoolean("email_verified"))
    }

    @Test
    fun parse_handlesEmptyStringGracefully() {
        val claims = SdJwtParser.parse("")
        assertEquals(0, claims.length())
    }

    @Test
    fun parse_handlesMalformedDisclosuresGracefully() {
        val header = base64Url("""{"alg":"ES256"}""")
        val payload = base64Url("""{"iss":"testIssuer"}""")
        val issuerJwt = "$header.$payload.sig"

        val invalidDisclosure = "not-valid-base64-json@@@"
        val rawSdJwt = "$issuerJwt~$invalidDisclosure"

        val claims = SdJwtParser.parse(rawSdJwt)
        assertEquals("testIssuer", claims.getString("iss"))
    }
}
