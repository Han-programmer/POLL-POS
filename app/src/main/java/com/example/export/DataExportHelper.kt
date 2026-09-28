package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DataExportHelper {

    private val sdfDate = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    private val fileSdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    private fun formatRupiah(num: Double): String {
        return String.format(Locale.GERMANY, "%,.0f", num)
    }

    /**
     * Exports transactions list to a CSV file compatible with Excel.
     */
    suspend fun exportTransactionsToCsv(
        context: Context,
        transactions: List<Transaction>
    ): File = withContext(Dispatchers.IO) {
        val fileName = "Laporan_Transaksi_${fileSdf.format(Date())}.csv"
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM so Excel opens it with correct encoding
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = fos.bufferedWriter()
            writer.write("No Transaksi,Tanggal,Kasir,Pelanggan,No Meja,Subtotal,Diskon,Pajak,Service Charge,Total,Status,Metode Pembayaran,Catatan\n")
            for (tx in transactions) {
                val line = listOf(
                    tx.transactionNo,
                    sdfDate.format(Date(tx.timestamp)),
                    tx.cashierName.replace(",", " "),
                    tx.customerName.replace(",", " "),
                    tx.tableNo,
                    tx.subtotal.toLong().toString(),
                    tx.discountAmount.toLong().toString(),
                    tx.taxAmount.toLong().toString(),
                    tx.serviceCharge.toLong().toString(),
                    tx.total.toLong().toString(),
                    tx.paymentStatus,
                    "\"${tx.paymentMethodsSummary.replace("\"", "\"\"")}\"",
                    "\"${tx.notes.replace("\"", "\"\"")}\""
                ).joinToString(",")
                writer.write(line + "\n")
            }
            writer.flush()
        }
        file
    }

    /**
     * Exports products inventory list to CSV.
     */
    suspend fun exportProductsToCsv(
        context: Context,
        products: List<Product>,
        categoriesMap: Map<Long, String>
    ): File = withContext(Dispatchers.IO) {
        val fileName = "Data_Produk_${fileSdf.format(Date())}.csv"
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)

        FileOutputStream(file).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = fos.bufferedWriter()
            writer.write("ID,Nama Produk,Kategori,SKU,Barcode,Harga Jual,Harga Modal,Stok,Min Stok,Satuan,Status\n")
            for (p in products) {
                val catName = categoriesMap[p.categoryId] ?: "Umum"
                val line = listOf(
                    p.id.toString(),
                    "\"${p.name.replace("\"", "\"\"")}\"",
                    "\"${catName.replace("\"", "\"\"")}\"",
                    p.sku,
                    p.barcode,
                    p.price.toLong().toString(),
                    p.costPrice.toLong().toString(),
                    p.stock.toString(),
                    p.minStock.toString(),
                    p.unit,
                    if (p.isActive) "Aktif" else "Nonaktif"
                ).joinToString(",")
                writer.write(line + "\n")
            }
            writer.flush()
        }
        file
    }

    /**
     * Exports customer list to CSV.
     */
    suspend fun exportCustomersToCsv(
        context: Context,
        customers: List<Customer>
    ): File = withContext(Dispatchers.IO) {
        val fileName = "Data_Pelanggan_${fileSdf.format(Date())}.csv"
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)

        FileOutputStream(file).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = fos.bufferedWriter()
            writer.write("ID,Nama Pelanggan,No HP,Alamat,Total Belanja,Jumlah Transaksi,Diskon Khusus (%),Catatan\n")
            for (c in customers) {
                val line = listOf(
                    c.id.toString(),
                    "\"${c.name.replace("\"", "\"\"")}\"",
                    c.phone,
                    "\"${c.address.replace("\"", "\"\"")}\"",
                    c.totalSpent.toLong().toString(),
                    c.totalTransactions.toString(),
                    c.discountPercent.toString(),
                    "\"${c.notes.replace("\"", "\"\"")}\""
                ).joinToString(",")
                writer.write(line + "\n")
            }
            writer.flush()
        }
        file
    }

    /**
     * Exports Cash flow / Shifts to CSV.
     */
    suspend fun exportCashShiftsToCsv(
        context: Context,
        shifts: List<CashShift>
    ): File = withContext(Dispatchers.IO) {
        val fileName = "Laporan_Kas_${fileSdf.format(Date())}.csv"
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)

        FileOutputStream(file).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = fos.bufferedWriter()
            writer.write("ID,Kasir,Waktu Buka,Waktu Tutup,Modal Awal,Kas Masuk,Kas Keluar,Penjualan Tunai,Kas Seharusnya,Kas Aktual,Selisih,Status\n")
            for (s in shifts) {
                val openStr = sdfDate.format(Date(s.openedAt))
                val closeStr = if (s.closedAt != null) sdfDate.format(Date(s.closedAt)) else "-"
                val line = listOf(
                    s.id.toString(),
                    s.staffName.replace(",", " "),
                    openStr,
                    closeStr,
                    s.initialCash.toLong().toString(),
                    s.totalPayIn.toLong().toString(),
                    s.totalPayOut.toLong().toString(),
                    s.totalCashSales.toLong().toString(),
                    s.expectedCash.toLong().toString(),
                    (s.actualCash?.toLong() ?: 0L).toString(),
                    s.difference.toLong().toString(),
                    if (s.isOpen) "Buka" else "Ditutup"
                ).joinToString(",")
                writer.write(line + "\n")
            }
            writer.flush()
        }
        file
    }

    /**
     * Builds clean HTML printable report that can be viewed or printed as PDF.
     */
    fun buildSalesHtmlReport(
        storeName: String,
        periodTitle: String,
        transactions: List<Transaction>,
        totalRevenue: Double,
        totalTransactions: Int,
        totalDiscount: Double,
        totalTax: Double
    ): String {
        val rowsHtml = StringBuilder()
        for (tx in transactions) {
            rowsHtml.append("""
                <tr>
                    <td>${tx.transactionNo}</td>
                    <td>${sdfDate.format(Date(tx.timestamp))}</td>
                    <td>${tx.customerName}</td>
                    <td>${tx.paymentMethodsSummary}</td>
                    <td style="text-align:right">Rp ${formatRupiah(tx.total)}</td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Laporan Penjualan - $storeName</title>
                <style>
                    body { font-family: sans-serif; margin: 20px; color: #1a1a1a; }
                    .header { text-align: center; border-bottom: 2px solid #C62828; padding-bottom: 12px; margin-bottom: 20px; }
                    .header h1 { margin: 0; color: #C62828; }
                    .header p { margin: 4px 0; color: #666; font-size: 14px; }
                    .summary { display: flex; gap: 16px; margin-bottom: 24px; }
                    .card { flex: 1; border: 1px solid #ddd; border-radius: 8px; padding: 12px; background: #fafafa; }
                    .card .label { font-size: 12px; color: #666; }
                    .card .val { font-size: 18px; font-weight: bold; color: #C62828; margin-top: 4px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 13px; }
                    th { background: #C62828; color: white; padding: 10px; text-align: left; }
                    td { padding: 8px 10px; border-bottom: 1px solid #eee; }
                    tr:nth-child(even) { background: #fdfdfd; }
                    .footer { margin-top: 30px; font-size: 11px; text-align: center; color: #888; border-top: 1px solid #eee; padding-top: 12px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>$storeName</h1>
                    <p>Laporan Penjualan ($periodTitle)</p>
                    <p>Dicetak pada: ${sdfDate.format(Date())}</p>
                </div>
                <div class="summary">
                    <div class="card"><div class="label">Total Omset</div><div class="val">Rp ${formatRupiah(totalRevenue)}</div></div>
                    <div class="card"><div class="label">Total Transaksi</div><div class="val">$totalTransactions</div></div>
                    <div class="card"><div class="label">Total Diskon</div><div class="val">Rp ${formatRupiah(totalDiscount)}</div></div>
                    <div class="card"><div class="label">Total Pajak</div><div class="val">Rp ${formatRupiah(totalTax)}</div></div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>No Transaksi</th>
                            <th>Tanggal</th>
                            <th>Pelanggan</th>
                            <th>Metode Bayar</th>
                            <th style="text-align:right">Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rowsHtml
                    </tbody>
                </table>
                <div class="footer">
                    POLL POS Offline-First &bull; Solusi Kasir Modern Indonesia &bull; www.pollpos.id
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Shares an exported file via Android Intent.
     */
    fun shareFile(context: Context, file: File, mimeType: String = "text/csv") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan / Simpan Laporan"))
    }
}
