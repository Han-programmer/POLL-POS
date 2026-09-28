package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.StoreSettings
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionPayment
import com.example.printer.EscPosPrinterHelper
import java.io.File

@Composable
fun ReceiptDialog(
    transaction: Transaction,
    items: List<TransactionItem>,
    payments: List<TransactionPayment>,
    settings: StoreSettings,
    onDismiss: () -> Unit,
    onPrintBluetooth: () -> Unit
) {
    val context = LocalContext.current
    val receiptText = EscPosPrinterHelper.buildReceiptText(settings, transaction, items, payments)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Struk Pembayaran",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Realistic Monospace Thermal Paper
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    color = Color(0xFFFAFAFA),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val activeReceiptLogo = if (!settings.receiptCustomLogoUri.isNullOrBlank()) {
                            settings.receiptCustomLogoUri
                        } else if (settings.showLogoOnReceipt) {
                            settings.logoUri
                        } else null

                        if (settings.showLogoOnReceipt && !activeReceiptLogo.isNullOrBlank()) {
                            val logoData: Any = if (activeReceiptLogo.startsWith("/")) File(activeReceiptLogo) else activeReceiptLogo
                            AsyncImage(
                                model = logoData,
                                contentDescription = "Logo Struk",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = receiptText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (settings.paperSize == "80mm") 12.sp else 13.sp,
                            color = Color(0xFF212121),
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Struk Transaksi ${transaction.transactionNo}")
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Kirim Struk via WhatsApp / Pesan"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan")
                    }

                    Button(
                        onClick = onPrintBluetooth,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Struk")
                    }
                }
            }
        }
    }
}
