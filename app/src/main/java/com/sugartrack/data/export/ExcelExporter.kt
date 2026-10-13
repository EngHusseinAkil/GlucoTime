package com.sugartrack.data.export

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.sugartrack.R
import com.sugartrack.data.GlucoseReading
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {
    fun write(context: Context, readings: List<GlucoseReading>, output: OutputStream) {
        val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.ENGLISH
        val arabic = locale.language == "ar"
        val headers = if (arabic) listOf("التاريخ", "الوقت", "نتيجة فحص السكر", "نوع الفحص", "الملاحظة")
        else listOf("Date", "Time", "Glucose Result", "Measurement Type", "Written Note")
        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
        val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
        val rows = readings.sortedBy { it.measuredAt }.map {
            listOf(dateFormat.format(Date(it.measuredAt)), timeFormat.format(Date(it.measuredAt)), it.glucoseValue.toString(), context.getString(
                when (it.measurementType.name) {
                    "FASTING" -> R.string.fasting
                    "BEFORE_MEAL" -> R.string.before_meal
                    "AFTER_MEAL" -> R.string.after_meal
                    "RANDOM" -> R.string.random
                    else -> R.string.other
                }
            ), it.writtenNote.orEmpty())
        }
        ZipOutputStream(output).use { zip ->
            put(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>")
            put(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            put(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>")
            put(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"${if (arabic) "سجل الفحوصات" else "Readings"}\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
            val allRows = listOf(headers) + rows
            val sheet = buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
                allRows.forEachIndexed { rowIndex, row -> append("<row r=\"${rowIndex + 1}\">"); row.forEachIndexed { column, value -> append("<c r=\"${columnName(column)}${rowIndex + 1}\" t=\"inlineStr\"><is><t>${xml(value)}</t></is></c>") }; append("</row>") }
                append("</sheetData></worksheet>")
            }
            put(zip, "xl/worksheets/sheet1.xml", sheet)
        }
    }

    private fun put(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
    }
    private fun columnName(index: Int) = ('A'.code + index).toChar().toString()
    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
