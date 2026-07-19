package com.hjed.ottnavigator

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/**
 * Mandatory activation dialog shown once the 15-day trial ends.
 * Non-dismissable (like the update dialog) and rendered over a darkened scrim
 * so the underlying UI is dimmed while activation is required.
 */
@Composable
fun ActivationDialog(
    context: Context,
    onActivated: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val deviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Darkened scrim behind the dialog.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
    )

    AlertDialog(
        onDismissRequest = { /* Mandatory activation: do not dismiss. */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = "Activation Required",
                color = MotifFocusNeon
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your 15-day free trial has ended. Enter your activation key to continue using StreamNav.",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        error = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("Activation Key") },
                    singleLine = true,
                    enabled = !verifying,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { /* handled by button */ }
                    )
                )

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                if (verifying) {
                    CircularProgressIndicator(
                        color = MotifFocusNeon,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Text(
                    text = "Keys look like: STN-XXXX-XXXX-XXXX-XXXX",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !verifying,
                onClick = {
                    val normalized = LicenseManager.normalizeKey(keyInput)
                    scope.launch {
                        verifying = true
                        error = null
                        val result = LicenseManager.validateKeyWithServer(normalized, deviceId)
                        verifying = false
                        if (result.success) {
                            LicenseManager.activate(context, normalized)
                            onActivated()
                        } else {
                            error = when (result.reason) {
                                "invalid-format" -> "Enter the complete 24-character activation key."
                                "invalid-key" -> "This activation key is invalid."
                                "revoked" -> "This activation key has been revoked."
                                "device-limit" -> "This key has reached its device limit."
                                "server-misconfig" -> "The activation server is not configured yet."
                                "network-error" -> "Could not reach the activation server. Check your connection."
                                else -> "Activation failed. Please try again later."
                            }
                        }
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MotifFocusNeon,
                    contentColor = Color.Black
                )
            ) {
                Text(if (verifying) "Verifying..." else "Activate")
            }
        }
    )
}
