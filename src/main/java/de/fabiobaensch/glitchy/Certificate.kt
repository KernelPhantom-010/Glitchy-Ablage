package de.fabiobaensch.glitchy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Erzeugt das PDF-Zertifikat (A4 quer) lokal auf dem Gerät. */
object Certificate {
    private val violet = 0xFF7C3AED.toInt()
    private val dark = 0xFF160A33.toInt()
    private val gray = 0xFF555066.toInt()

    fun create(ctx: Context, fullName: String, moduleTitle: String, score: Int, certId: String): File {
        val w = 842
        val h = 595
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, 1).create())
        val c: Canvas = page.canvas
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        c.drawColor(Color.WHITE)
        p.style = Paint.Style.STROKE
        p.color = violet
        p.strokeWidth = 6f
        c.drawRect(24f, 24f, w - 24f, h - 24f, p)
        p.strokeWidth = 1.5f
        c.drawRect(36f, 36f, w - 36f, h - 36f, p)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER

        fun text(s: String, y: Float, size: Float, color: Int, bold: Boolean = false) {
            p.color = color
            p.typeface = Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            p.textSize = size
            while (p.measureText(s) > w - 120f && p.textSize > 10f) p.textSize -= 1f
            c.drawText(s, w / 2f, y, p)
        }

        text("GLITCHY", 92f, 20f, violet, true)
        text("Certificate of Completion", 146f, 42f, dark, true)
        text("This is to certify that", 196f, 16f, gray)
        text(fullName, 256f, 40f, dark, true)
        p.color = violet; p.strokeWidth = 1f
        c.drawLine(180f, 270f, w - 180f, 270f, p)
        text("has successfully completed the module", 305f, 16f, gray)
        text(moduleTitle, 348f, 28f, violet, true)
        text("with a final test result of $score %", 382f, 16f, gray)

        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        text("Issued on $date", 430f, 14f, gray)
        text("Fabio Baensch", 485f, 22f, dark, true)
        text("Creator of Glitchy", 505f, 13f, gray)
        text("Certificate ID: $certId", 548f, 10f, gray)
        text("Issued within the Glitchy app. This is a self-study certificate, not an accredited qualification.", 563f, 9f, gray)

        doc.finishPage(page)
        val dir = File(ctx.cacheDir, "certs").apply { mkdirs() }
        val file = File(dir, "Glitchy-Certificate.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(i, "Certificate").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
