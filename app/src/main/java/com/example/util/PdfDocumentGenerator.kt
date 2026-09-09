package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.UnderlineSpan
import android.text.SpannableString
import android.text.style.StyleSpan
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PdfDocumentGenerator(private val context: Context) {

    private val doc = PdfDocument()
    private var currentPage: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var pageNumber = 1
    private var currentY = 0f

    // Dimensions in postscript points (72 points per inch)
    // A4 is 210mm x 297mm
    private val pageWidth = 595.27f
    private val pageHeight = 841.89f
    
    // Margins (20mm, 10mm, 20mm)
    private val leftMargin = mmToPoints(20f)
    private val rightMargin = mmToPoints(20f)
    private val topMargin = mmToPoints(10f)
    private val bottomMargin = mmToPoints(15f)
    private val printableWidth = pageWidth - leftMargin - rightMargin

    private fun mmToPoints(mm: Float): Float = mm * 2.8346457f

    private fun startNewPage() {
        if (currentPage != null) {
            doc.finishPage(currentPage)
        }
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber++).create()
        currentPage = doc.startPage(pageInfo)
        canvas = currentPage!!.canvas
        currentY = topMargin
    }

    private fun ensureSpace(neededHeight: Float) {
        if (currentPage == null || currentY + neededHeight > pageHeight - bottomMargin) {
            startNewPage()
        }
    }

    fun generateLetterPdf(
        header: String,
        title: String,
        nomor: String,
        lampiran: String,
        perihal: String,
        alignment: String,
        bodyText: String,
        ketuaNama: String,
        sekretarisNama: String,
        usePj: Boolean,
        pjNama: String,
        footerText: String,
        logoPathOrUri: String?,
        sigKetuaPath: String?,
        sigSekretarisPath: String?,
        sigPjPath: String?,
        fileName: String
    ): Uri? {
        try {
            // Start the very first page
            startNewPage()

            // 1. Draw Logo if present
            val logoBitmap = loadBitmap(logoPathOrUri)
            if (logoBitmap != null) {
                // Place at x=20mm, y=4mm, width=20mm, height scaled proportionally
                val logoX = mmToPoints(20f)
                val logoY = mmToPoints(4f)
                val logoW = mmToPoints(20f)
                val logoH = logoW * (logoBitmap.height.toFloat() / logoBitmap.width.toFloat())
                val src = Rect(0, 0, logoBitmap.width, logoBitmap.height)
                val dst = RectF(logoX, logoY, logoX + logoW, logoY + logoH)
                canvas?.drawBitmap(logoBitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            }

            // 2. Draw Header
            val headerPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            
            // Draw header text centered. Starts at y=10mm
            currentY = mmToPoints(10f)
            val headerLines = header.uppercase().split("\n")
            for (line in headerLines) {
                if (line.isNotBlank()) {
                    drawCenteredText(line, headerPaint)
                    currentY += headerPaint.textSize * 1.25f
                }
            }
            currentY += mmToPoints(1f)

            // Draw line below header: pdf.line(20, pdf.get_y(), 190, pdf.get_y())
            // (Note: x coordinates in FPDF 20 to 190mm)
            ensureSpace(2f)
            val linePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 1.5f
                style = Paint.Style.STROKE
            }
            canvas?.drawLine(mmToPoints(20f), currentY, mmToPoints(190f), currentY, linePaint)
            currentY += mmToPoints(3f)

            // 3. Draw Title
            if (title.isNotBlank()) {
                val titlePaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 14f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                val titleLines = title.uppercase().split("\n")
                for (line in titleLines) {
                    if (line.isNotBlank()) {
                        drawCenteredText(line, titlePaint)
                        currentY += titlePaint.textSize * 1.3f
                    }
                }
                currentY += mmToPoints(3f)
            }

            // 4. Draw Metadata: Nomor, Lampiran, Perihal
            val metaPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val metaFields = listOf(
                "Nomor" to nomor,
                "Lampiran" to lampiran,
                "Perihal" to perihal
            )

            for ((label, value) in metaFields) {
                drawMetaRow(label, value, metaPaint)
                currentY += mmToPoints(1f)
            }
            currentY += mmToPoints(3f)

            // 5. Draw Body Content (Isi Surat)
            val bodyPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val bodyLines = bodyText.split("\n")
            for (line in bodyLines) {
                if (line.trim().isEmpty()) {
                    currentY += mmToPoints(2f)
                    continue
                }

                val colonIdx = line.indexOf(":")
                // Rule: if colon is present and left part of colon is short (e.g. less than 35 characters), align it nicely
                if (colonIdx != -1 && colonIdx < 35) {
                    val label = line.substring(0, colonIdx).trim()
                    val value = line.substring(colonIdx + 1).trim()
                    
                    // Starts at 25mm as per Kivy script (pdf.set_x(25))
                    val labelStartX = mmToPoints(25f)
                    val labelW = mmToPoints(35f)
                    val colonW = mmToPoints(5f)
                    val valueW = (pageWidth - rightMargin) - (labelStartX + labelW + colonW)

                    val labelLayout = StaticLayout.Builder.obtain(label, 0, label.length, bodyPaint, labelW.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false)
                        .build()

                    val valueLayout = StaticLayout.Builder.obtain(value, 0, value.length, bodyPaint, valueW.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false)
                        .build()

                    val height = maxOf(labelLayout.height, valueLayout.height).toFloat()
                    ensureSpace(height)

                    canvas?.save()
                    canvas?.translate(labelStartX, currentY)
                    labelLayout.draw(canvas!!)
                    canvas?.restore()

                    canvas?.save()
                    canvas?.translate(labelStartX + labelW, currentY)
                    canvas?.drawText(":", 0f, bodyPaint.textSize, bodyPaint)
                    canvas?.restore()

                    canvas?.save()
                    canvas?.translate(labelStartX + labelW + colonW, currentY)
                    valueLayout.draw(canvas!!)
                    canvas?.restore()

                    currentY += height
                } else {
                    // Normal text line
                    val align = when (alignment) {
                        "Center" -> Layout.Alignment.ALIGN_CENTER
                        "Right" -> Layout.Alignment.ALIGN_OPPOSITE
                        else -> Layout.Alignment.ALIGN_NORMAL
                    }
                    val isJustified = alignment == "Justify"

                    val builder = StaticLayout.Builder.obtain(line, 0, line.length, bodyPaint, printableWidth.toInt())
                        .setAlignment(align)
                        .setLineSpacing(0f, 1.15f)
                        .setIncludePad(false)

                    if (isJustified && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
                    }

                    val layout = builder.build()
                    val height = layout.height.toFloat()
                    ensureSpace(height)

                    canvas?.save()
                    canvas?.translate(leftMargin, currentY)
                    layout.draw(canvas!!)
                    canvas?.restore()

                    currentY += height
                }
                currentY += mmToPoints(1f) // small paragraph gap
            }
            currentY += mmToPoints(5f)

            // 6. Signatures (Ketua & Sekretaris)
            // If we are near the bottom (y > 230mm, which is ~652 points), add a page
            if (currentY > mmToPoints(230f)) {
                startNewPage()
                currentY = mmToPoints(20f)
            } else {
                ensureSpace(mmToPoints(40f))
            }

            val sigLabelPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val col1Center = leftMargin + mmToPoints(42.5f) // Center of first 85mm column
            val col2Center = leftMargin + mmToPoints(85f) + mmToPoints(42.5f) // Center of second 85mm column

            // Draw Column Headers
            canvas?.drawText("Ketua Pelaksana,", col1Center, currentY + sigLabelPaint.textSize, sigLabelPaint)
            canvas?.drawText("Sekretaris,", col2Center, currentY + sigLabelPaint.textSize, sigLabelPaint)
            currentY += mmToPoints(5f)

            val sigImgY = currentY
            val sigW = mmToPoints(40f)
            val sigH = mmToPoints(14f)

            // Draw Ketua Signature
            val ketuaSigBitmap = loadBitmap(sigKetuaPath)
            if (ketuaSigBitmap != null) {
                // centered in column 1 (center is col1Center, so left = col1Center - sigW / 2)
                val sigX = col1Center - (sigW / 2)
                val src = Rect(0, 0, ketuaSigBitmap.width, ketuaSigBitmap.height)
                val dst = RectF(sigX, sigImgY, sigX + sigW, sigImgY + sigH)
                canvas?.drawBitmap(ketuaSigBitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            }

            // Draw Sekretaris Signature
            val sekretarisSigBitmap = loadBitmap(sigSekretarisPath)
            if (sekretarisSigBitmap != null) {
                // centered in column 2 (center is col2Center, so left = col2Center - sigW / 2)
                val sigX = col2Center - (sigW / 2)
                val src = Rect(0, 0, sekretarisSigBitmap.width, sekretarisSigBitmap.height)
                val dst = RectF(sigX, sigImgY, sigX + sigW, sigImgY + sigH)
                canvas?.drawBitmap(sekretarisSigBitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            }

            currentY += sigH + mmToPoints(2f)

            // Draw Column Names
            val namePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas?.drawText("( $ketuaNama )", col1Center, currentY + namePaint.textSize, namePaint)
            canvas?.drawText("( $sekretarisNama )", col2Center, currentY + namePaint.textSize, namePaint)
            currentY += mmToPoints(5f)

            // 7. Penanggung Jawab
            if (usePj) {
                ensureSpace(mmToPoints(35f))
                
                canvas?.drawText("Mengetahui,", pageWidth / 2f, currentY + namePaint.textSize, namePaint)
                currentY += namePaint.textSize * 1.2f
                canvas?.drawText("Penanggung Jawab,", pageWidth / 2f, currentY + namePaint.textSize, namePaint)
                currentY += mmToPoints(2f)

                val pjSigImgY = currentY
                val pjSigBitmap = loadBitmap(sigPjPath)
                if (pjSigBitmap != null) {
                    val sigX = (pageWidth / 2f) - (sigW / 2f)
                    val src = Rect(0, 0, pjSigBitmap.width, pjSigBitmap.height)
                    val dst = RectF(sigX, pjSigImgY, sigX + sigW, pjSigImgY + sigH)
                    canvas?.drawBitmap(pjSigBitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                }
                currentY += sigH + mmToPoints(2f)

                val pjNamePaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    isUnderlineText = true // Underlined as 'BU'
                }
                canvas?.drawText(pjNama, pageWidth / 2f, currentY + pjNamePaint.textSize, pjNamePaint)
                currentY += mmToPoints(5f)
            }

            // 8. Footer
            if (footerText.isNotBlank()) {
                // FPDF forces footer at y = -40mm from bottom
                val footerY = pageHeight - mmToPoints(40f)
                
                // If currentY is already below footerY - 10mm, let's draw footer on a new page!
                if (currentY > footerY - mmToPoints(10f)) {
                    startNewPage()
                }
                
                currentY = footerY
                ensureSpace(mmToPoints(20f))

                // Draw horizontal divider line
                canvas?.drawLine(leftMargin, currentY, pageWidth - rightMargin, currentY, linePaint)
                currentY += mmToPoints(2f)

                // Draw footer text (Helvetica Italic 8)
                val footerPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 8f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                    isAntiAlias = true
                }

                val builder = StaticLayout.Builder.obtain(footerText.trim(), 0, footerText.trim().length, footerPaint, printableWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(false)
                val layout = builder.build()
                
                canvas?.save()
                canvas?.translate(leftMargin, currentY)
                layout.draw(canvas!!)
                canvas?.restore()
            }

            // Finish the final page
            if (currentPage != null) {
                doc.finishPage(currentPage)
            }

            // Save PDF document using MediaStore or Legacy Storage
            return savePdfToDisk(fileName)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            doc.close()
        }
    }

    private fun drawCenteredText(text: String, paint: TextPaint) {
        val width = paint.measureText(text)
        val x = (pageWidth - width) / 2f
        ensureSpace(paint.textSize * 1.3f)
        canvas?.drawText(text, x, currentY + paint.textSize, paint)
    }

    private fun drawMetaRow(label: String, value: String, paint: TextPaint) {
        val labelW = mmToPoints(30f)
        val colonW = mmToPoints(5f)
        val valW = printableWidth - labelW - colonW

        val labelLayout = StaticLayout.Builder.obtain(label, 0, label.length, paint, labelW.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()

        val valueLayout = StaticLayout.Builder.obtain(value, 0, value.length, paint, valW.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()

        val height = maxOf(labelLayout.height, valueLayout.height).toFloat()
        ensureSpace(height)

        // Draw label
        canvas?.save()
        canvas?.translate(leftMargin, currentY)
        labelLayout.draw(canvas!!)
        canvas?.restore()

        // Draw colon
        canvas?.save()
        canvas?.translate(leftMargin + labelW, currentY)
        canvas?.drawText(":", 0f, paint.textSize, paint)
        canvas?.restore()

        // Draw value
        canvas?.save()
        canvas?.translate(leftMargin + labelW + colonW, currentY)
        valueLayout.draw(canvas!!)
        canvas?.restore()

        currentY += height
    }

    private fun loadBitmap(pathOrUri: String?): Bitmap? {
        if (pathOrUri.isNullOrEmpty()) return null
        return try {
            if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                val uri = Uri.parse(pathOrUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                BitmapFactory.decodeFile(pathOrUri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun savePdfToDisk(fileName: String): Uri? {
        val finalFileName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, finalFileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Hasil_Surat")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    doc.writeTo(out)
                }
            }
            return uri
        } else {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val folder = File(downloadDir, "Hasil_Surat")
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val file = File(folder, finalFileName)
            FileOutputStream(file).use { out ->
                doc.writeTo(out)
            }
            return Uri.fromFile(file)
        }
    }
}
