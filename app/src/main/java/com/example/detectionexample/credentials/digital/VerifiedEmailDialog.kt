package com.example.detectionexample.credentials.digital

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Dialog facilitating OTP-less Verified Email retrieval via Google Verifiable Credentials
 * and Credential Manager Digital Credentials API.
 */
@Composable
fun VerifiedEmailDialog(
    onDismissRequest: () -> Unit,
    onVerificationSuccess: (VerifiedUserInfo) -> Unit,
    viewModel: VerifiedEmailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val verifiedUser = uiState.verifiedUser

    AlertDialog(
        onDismissRequest = {
            if (!uiState.isLoading) {
                viewModel.resetState()
                onDismissRequest()
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (verifiedUser != null) "Verified Identity" else "Instant Email Verification",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (verifiedUser == null) {
                    Text(
                        text = "Verify your email seamlessly with Google Digital Credentials—no manual OTP or confirmation link needed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cryptographically Secured",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Backed by OpenID4VP & W3C Verifiable Credentials issued directly by Google Accounts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (uiState.isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Requesting Digital Credential...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    // Verified State
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = verifiedUser.displayName.ifEmpty { "Verified User" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = verifiedUser.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E7D32)
                            )
                            if (verifiedUser.isEmailVerified) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "✓ Google Cryptographically Verified",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }

                    // Passkey Creation Option
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Enable Passkey", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = if (uiState.isPasskeyCreated) "Passkey created successfully!" else "Fast, passwordless biometric sign-in",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isPasskeyCreated) {
                                        Color(
                                            0xFF2E7D32
                                        )
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.markPasskeyCreated()
                                    Toast.makeText(
                                        context,
                                        "Passkey configured for ${verifiedUser.email}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                enabled = !uiState.isPasskeyCreated
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (uiState.isPasskeyCreated) "Added" else "Add")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (verifiedUser == null) {
                Button(
                    onClick = { viewModel.verifyEmail(context) },
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Verify with Google")
                }
            } else {
                Button(
                    onClick = {
                        onVerificationSuccess(verifiedUser)
                        viewModel.resetState()
                        onDismissRequest()
                    }
                ) {
                    Text("Continue")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.resetState()
                    onDismissRequest()
                },
                enabled = !uiState.isLoading
            ) {
                Text(if (verifiedUser == null) "Cancel" else "Close")
            }
        }
    )
}
