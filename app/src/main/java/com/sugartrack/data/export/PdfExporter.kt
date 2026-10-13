package com.sugartrack.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.appcompat.app.AppCompatDelegate
import com.sugartrack.R
import com.sugartrack.data.GlucoseReading
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Creates a printer-friendly report without adding a PDF dependency. */
object PdfExporter {
    fun write(context: Context, readings: List<GlucoseReading>, userName: String, output: OutputStream) {
        val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.ENGLISH
        val arabic = locale.language == "ar"
        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
        val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
        val title = context.getString(R.string.app_name)
        val exported = dateFormat.format(Date())
        val headers = if (arabic) listOf("التاريخ", "الوقت", "نتيجة فحص السكر", "نوع الفحص", "الملاحظة")
        else listOf("Date", "Time", "Glucose Result", "Measurement Type", "Note")
        val rows = readings.sortedBy { it.measuredAt }.map { reading ->
            listOf(
                dateFormat.format(Date(reading.measuredAt)),
                timeFormat.format(Date(reading.measuredAt)),
                reading.glucoseValue.toString(),
                context.getString(when (reading.measurementType.name) {
                    "FASTING" -> R.string.fasting
                    "BEFORE_MEAL" -> R.string.before_meal
                    "AFTER_MEAL" -> R.string.after_meal
                    "RANDOM" -> R.string.random
                    else -> R.string.other
                }),
                reading.writtenNote.orEmpty()
            )
        }

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = 0xFF122033.toInt() }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; isFakeBoldText = true; color = 0xFF2563EB.toInt() }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = 0xFF5B6B7E.toInt() }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD9E2ED.toInt(); strokeWidth = 1f }
        val columnWidths = floatArrayOf(92f, 72f, 92f, 130f, 129f)
        val rowHeight = 28f
        var pageNumber = 1
        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = 0f

        fun startPage() {
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page!!.canvas
            y = margin
            canvas!!.drawText(title, margin, y, headerPaint)
            y += 24f
            canvas!!.drawText(if (arabic) "اسم المستخدم: $userName" else "User: $userName", margin, y, bodyPaint)
            y += 17f
            canvas!!.drawText(if (arabic) "تاريخ التقرير: $exported" else "Report date: $exported", margin, y, smallPaint)
            y += 25f
            var x = margin
            headers.forEachIndexed { index, header ->
                canvas!!.drawText(header.take(20), x, y, bodyPaint)
                x += columnWidths[index]
            }
            y += 8f
            canvas!!.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 20f
        }

        fun finishPage() {
            canvas?.drawText("$pageNumber", pageWidth - margin - 12f, pageHeight - 22f, smallPaint)
            page?.let { document.finishPage(it) }
            page = null
            canvas = null
            pageNumber++
        }

        startPage()
        rows.forEach { row ->
            if (y > pageHeight - 55f) {
                finishPage()
                startPage()
            }
            var x = margin
            row.forEachIndexed { index, value ->
                canvas!!.drawText(value.replace("\n", " ").take(22), x, y, bodyPaint)
                x += columnWidths[index]
            }
            canvas!!.drawLine(margin, y + 8f, pageWidth - margin, y + 8f, linePaint)
            y += rowHeight
        }
        finishPage()
        document.writeTo(output)
        document.close()
    }
}
