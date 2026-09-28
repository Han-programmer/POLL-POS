package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PinLockDialog(
    staffName: String,
    onPinSubmit: (String) -> Boolean,
    onCancel: (() -> Unit)? = null
) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (onCancel != null) onCancel() },
        properties = DialogProperties(dismissOnBackPress = onCancel != null, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PIN Lock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "POLL POS Keamanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Masukkan PIN Kasir ($staffName)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val filled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isError) Color.Red
                                    else if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PIN salah, silakan coba lagi",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Keypad Numpad
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (key == "C" || key == "DEL") MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        isError = false
                                        when (key) {
                                            "C" -> pinInput = ""
                                            "DEL" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                            else -> {
                                                if (pinInput.length < 6) {
                                                    val newPin = pinInput + key
                                                    pinInput = newPin
                                                    if (newPin.length >= 4) {
                                                        val ok = onPinSubmit(newPin)
                                                        if (!ok && newPin.length == 4) {
                                                            isError = true
                                                            pinInput = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onCancel) {
                        Text("Batal")
                    }
                }
            }
        }
    }
}
