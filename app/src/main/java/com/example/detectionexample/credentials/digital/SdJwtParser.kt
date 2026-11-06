package com.example.detectionexample.credentials.digital

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Client-side parser for Selective Disclosure JSON Web Tokens (SD-JWT, RFC 9901).
 *
 * Deconstructs the SD-JWT into its Issuer JWT payload and individual disclosure objects,
 * merging selectively disclosed claims into a unified [JSONObject].
 *
 * NOTE: Client-side parsing is for preliminary UI presentation only. Full cryptographic
 * validation (issuer trust, signature verification, nonce freshness, and key binding)
 * MUST be completed on the relying party server.
 */
object SdJwtParser {

    /**
     * Parses a raw SD-JWT string and returns a [JSONObject] of all disclosed claims.
     *
     * Format: `<issuer-jwt>~<disclosure 1>~<disclosure 2>~...~<optional-key-binding-jwt>`
     */
    fun parse(rawSdJwt: String): JSONObject {
        val parts = rawSdJwt.split("~")
        if (parts.isEmpty()) {
            return JSONObject()
        }

        val issuerJwt = parts[0]
        val resultClaims = JSONObject()

        // 1. Decode Issuer JWT payload
        val jwtSegments = issuerJwt.split(".")
        if (jwtSegments.size >= 2) {
            val payloadJsonString = decodeBase64Url(jwtSegments[1])
            try {
                val payloadJson = JSONObject(payloadJsonString)
                val keys = payloadJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key != "_sd" && key != "_sd_alg") {
                        resultClaims.put(key, payloadJson.get(key))
                    }
                }
            } catch (_: Exception) {
                // Invalid issuer payload; proceed with empty claims
            }
        }

        // 2. Decode Disclosures (skip first item which is issuer-jwt, and skip trailing empty strings)
        for (i in 1 until parts.size) {
            val disclosurePart = parts[i].trim()
            if (disclosurePart.isEmpty()) continue

            // A Key Binding JWT contains dots (header.payload.sig); disclosures do not contain dots
            if (disclosurePart.contains(".")) {
                continue
            }

            try {
                val decodedDisclosure = decodeBase64Url(disclosurePart)
                val disclosureArray = JSONArray(decodedDisclosure)

                // Object property disclosure format: [salt, claim_name, claim_value]
                if (disclosureArray.length() == 3) {
                    val claimName = disclosureArray.optString(1)
                    val claimValue = disclosureArray.get(2)
                    if (claimName.isNotEmpty()) {
                        resultClaims.put(claimName, claimValue)
                    }
                }
            } catch (_: Exception) {
                // Ignore malformed disclosures
            }
        }

        return resultClaims
    }

    private fun decodeBase64Url(base64UrlString: String): String {
        return try {
            val clean = base64UrlString.replace("-", "+").replace("_", "/")
            val padded = when (clean.length % 4) {
                2 -> "$clean=="
                3 -> "$clean="
                else -> clean
            }
            val bytes = java.util.Base64.getDecoder().decode(padded)
            String(bytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            try {
                val bytes = Base64.decode(base64UrlString, Base64.URL_SAFE or Base64.NO_WRAP)
                if (bytes != null) String(bytes, StandardCharsets.UTF_8) else ""
            } catch (_: Exception) {
                ""
            }
        }
    }
}
