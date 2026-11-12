package com.sudharo.provider.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import com.sudharo.provider.domain.model.Quotation

/** Real quotation PDF via android.graphics.pdf.PdfDocument (no extra dependency). */
object PdfGenerator {
    fun saveQuotationPdf(context: Context, q: Quotation, providerName: String): String {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas
        val title = Paint().apply { textSize = 22f; isFakeBoldText = true }
        val body = Paint().apply { textSize = 13f }
        val small = Paint().apply { textSize = 11f }
        var y = 70f
        c.drawText("SUDHARO — Quotation ${q.id}", 40f, y, title); y += 30
        c.drawText("Provider: $providerName", 40f, y, body); y += 22
        c.drawText("Job: ${q.title}  •  Version ${q.version}  •  Status: ${q.status}", 40f, y, body); y += 30
        c.drawText("Labour charges:  Rs ${q.labour}", 40f, y, body); y += 22
        c.drawText("Material cost:    Rs ${q.material}", 40f, y, body); y += 22
        c.drawText("Other charges:   Rs ${q.other}", 40f, y, body); y += 26
        c.drawText("Total amount:    Rs ${q.total}", 40f, y, title.apply { textSize = 17f }); y += 28
        c.drawText("Timeline: ${q.timelineDays} day(s)", 40f, y, body); y += 22
        c.drawText("Terms: ${q.terms}", 40f, y, small)
        doc.finishPage(page)
        val name = "Sudharo-Quotation-${q.id}-v${q.version}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create PDF in Downloads")
        context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        doc.close()
        return name
    }
}
