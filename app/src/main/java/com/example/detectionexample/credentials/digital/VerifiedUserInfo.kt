package com.example.detectionexample.credentials.digital

/**
 * Data class representing cryptographically verified user attributes retrieved via
 * Digital Credentials (OpenID4VP / UserInfoCredential).
 */
data class VerifiedUserInfo(
    val email: String,
    val displayName: String,
    val givenName: String? = null,
    val familyName: String? = null,
    val pictureUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val hostedDomain: String? = null,
    val nonce: String? = null,
    val rawSdJwt: String? = null,
    val rawResponseJson: String? = null,
)
