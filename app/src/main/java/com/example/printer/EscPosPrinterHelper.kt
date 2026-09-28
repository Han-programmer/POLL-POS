package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.data.model.StoreSettings
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object EscPosPrinterHelper {
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Standard ESC/POS commands
    private val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @
    private val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
    private val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
    private val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2
    private val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1
    private val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0
    private val CMD_FEED_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 66 0

    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun formatRupiah(amount: Double): String {
        val formatted = String.format(Locale.GERMANY, "%,.0f", amount)
        return "Rp$formatted"
    }

    /**
     * Generates a realistic plain text receipt for 58mm (32 chars) or 80mm (48 chars).
     */
    fun buildReceiptText(
        settings: StoreSettings,
        transaction: Transaction,
        items: List<TransactionItem>,
        payments: List<TransactionPayment>
    ): String {
        val width = if (settings.paperSize == "80mm") 48 else 32
        val sb = StringBuilder()
        val separator = "-".repeat(width)
        val doubleSeparator = "=".repeat(width)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(transaction.timestamp))

        fun center(text: String): String {
            if (text.length >= width) return text
            val pad = (width - text.length) / 2
            return " ".repeat(pad) + text
        }

        fun twoCols(left: String, right: String): String {
            val space = width - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right else "$left $right"
        }

        // Header
        val storeDisplayName = if (settings.customReceiptStoreName.isNotBlank()) settings.customReceiptStoreName else settings.storeName
        sb.appendLine(center(storeDisplayName.uppercase()))
        if (settings.address.isNotBlank()) sb.appendLine(center(settings.address))
        if (settings.phone.isNotBlank()) sb.appendLine(center("Telp: ${settings.phone}"))
        if (settings.receiptHeader.isNotBlank()) {
            settings.receiptHeader.split("\n").forEach { line ->
                sb.appendLine(center(line.trim()))
            }
        }
        sb.appendLine(separator)

        // Meta info
        sb.appendLine(twoCols("No: ${transaction.transactionNo}", ""))
        sb.appendLine(twoCols("Tgl: $dateStr", "Kasir: ${transaction.cashierName}"))
        if (transaction.customerName.isNotBlank() && transaction.customerName != "Pelanggan Umum") {
            sb.appendLine(twoCols("Plg: ${transaction.customerName}", if (transaction.tableNo.isNotBlank()) "Meja: ${transaction.tableNo}" else ""))
        } else if (transaction.tableNo.isNotBlank()) {
            sb.appendLine(twoCols("Meja: ${transaction.tableNo}", ""))
        }
        sb.appendLine(separator)

        // Items
        for (item in items) {
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            val priceStr = formatRupiah(item.price)
            val totalStr = formatRupiah(item.total)

            sb.appendLine(item.productName)
            if (item.variantNote.isNotBlank()) {
                sb.appendLine("  (${item.variantNote})")
            }
            sb.appendLine(twoCols("  $qtyStr x $priceStr", totalStr))
        }
        sb.appendLine(separator)

        // Subtotal, Discount, Tax, Total
        sb.appendLine(twoCols("Subtotal", formatRupiah(transaction.subtotal)))
        if (transaction.discountAmount > 0) {
            val note = if (transaction.discountNote.isNotBlank()) " (${transaction.discountNote})" else ""
            sb.appendLine(twoCols("Diskon$note", "-${formatRupiah(transaction.discountAmount)}"))
        }
        if (transaction.taxAmount > 0) {
            sb.appendLine(twoCols("Pajak (${transaction.taxRate}%)", formatRupiah(transaction.taxAmount)))
        }
        if (transaction.serviceCharge > 0) {
            sb.appendLine(twoCols("Service Charge", formatRupiah(transaction.serviceCharge)))
        }
        sb.appendLine(doubleSeparator)
        sb.appendLine(twoCols("TOTAL", formatRupiah(transaction.total)))

        // Payments
        for (p in payments) {
            sb.appendLine(twoCols("Bayar (${p.paymentMethod})", formatRupiah(p.amount)))
        }
        if (transaction.changeAmount > 0) {
            sb.appendLine(twoCols("Kembalian", formatRupiah(transaction.changeAmount)))
        }
        sb.appendLine(doubleSeparator)

        // Footer
        if (settings.receiptFooter.isNotBlank()) {
            settings.receiptFooter.split("\n").forEach { line ->
                sb.appendLine(center(line.trim()))
            }
        }
        sb.appendLine(center("=== POLL POS OFFLINE ==="))

        return sb.toString()
    }

    /**
     * Generates ESC/POS byte array for hardware thermal printer.
     */
    fun buildEscPosBytes(
        settings: StoreSettings,
        transaction: Transaction,
        items: List<TransactionItem>,
        payments: List<TransactionPayment>
    ): ByteArray {
        val text = buildReceiptText(settings, transaction, items, payments)
        val textBytes = text.toByteArray(charset("GBK"))

        val out = mutableListOf<Byte>()
        CMD_INIT.forEach { out.add(it) }
        CMD_ALIGN_LEFT.forEach { out.add(it) }
        textBytes.forEach { out.add(it) }
        // Line feeds and paper cut
        out.add(0x0A)
        out.add(0x0A)
        out.add(0x0A)
        CMD_FEED_CUT.forEach { out.add(it) }

        return out.toByteArray()
    }

    /**
     * Prints ESC/POS byte array to Bluetooth thermal printer.
     */
    @SuppressLint("MissingPermission")
    suspend fun printToBluetoothDevice(deviceAddress: String, data: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outStream: OutputStream? = null
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia di perangkat ini"))
            val device = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery()
            socket.connect()
            outStream = socket.outputStream
            outStream.write(data)
            outStream.flush()
            Result.success("Struk berhasil dicetak ke ${device.name ?: deviceAddress}")
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mencetak: ${e.localizedMessage ?: "Koneksi terputus"}"))
        } finally {
            try { outStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Test print command.
     */
    fun buildTestPrintBytes(settings: StoreSettings): ByteArray {
        val width = if (settings.paperSize == "80mm") 48 else 32
        val sb = StringBuilder()
        val sep = "=".repeat(width)
        sb.appendLine(sep)
        sb.appendLine("     POLL POS TEST PRINT")
        sb.appendLine("Printer Model: ${settings.paperSize} ESC/POS")
        sb.appendLine("Toko: ${settings.storeName}")
        sb.appendLine("Status: 100% Siap Digunakan")
        sb.appendLine(sep)
        sb.appendLine()
        val textBytes = sb.toString().toByteArray(charset("GBK"))
        val out = mutableListOf<Byte>()
        CMD_INIT.forEach { out.add(it) }
        textBytes.forEach { out.add(it) }
        out.add(0x0A)
        out.add(0x0A)
        CMD_FEED_CUT.forEach { out.add(it) }
        return out.toByteArray()
    }
}
