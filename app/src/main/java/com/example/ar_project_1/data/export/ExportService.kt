package com.example.ar_project_1.data.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.ar_project_1.data.model.PatientDto
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Jedna sekcja eksportu: tytuł, nagłówki kolumn i wiersze. */
data class ExportSection(
    val title: String,
    val columns: List<String>,
    val rows: List<List<String>>,
)

data class ExportData(
    val patient: PatientDto?,
    val sections: List<ExportSection>,
)

/**
 * Buduje pliki CSV i PDF z danych karty pacjenta.
 *
 * Wszystko dzieje się lokalnie na urządzeniu — plik nie opuszcza telefonu,
 * dopóki użytkownik sam go nie zapisze lub nie wyśle.
 */
class ExportService {

    fun csv(data: ExportData): ByteArray {
        val builder = StringBuilder()
        // BOM, żeby Excel poprawnie odczytał polskie znaki.
        builder.append('\uFEFF')
        builder.appendLine("Karta Pacjenta — eksport")
        builder.appendLine("Wygenerowano;${timestamp()}")
        data.patient?.let {
            builder.appendLine("Pacjent;${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trimEnd())
        }
        data.sections.forEach { section ->
            builder.appendLine()
            builder.appendLine(csvRow(listOf(section.title)))
            builder.appendLine(csvRow(section.columns))
            section.rows.forEach { builder.appendLine(csvRow(it)) }
        }
        return builder.toString().toByteArray(Charsets.UTF_8)
    }

    fun pdf(data: ExportData): ByteArray {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - 2 * margin

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            isFakeBoldText = true
            color = 0xFF1B1D1F.toInt()
        }
        val metaPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = 0xFF5F6368.toInt()
        }
        val sectionPaint = Paint().apply {
            isAntiAlias = true
            textSize = 13f
            isFakeBoldText = true
            color = 0xFFC62828.toInt()
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = 0xFF1B1D1F.toInt()
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 10f

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin + 10f
        }

        fun ensureSpace(height: Float) {
            if (y + height > pageHeight - margin) newPage()
        }

        fun drawWrapped(text: String, paint: Paint, lineHeight: Float, indent: Float = 0f) {
            val maxWidth = contentWidth - indent
            val words = text.split(' ')
            var line = StringBuilder()
            words.forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                    ensureSpace(lineHeight)
                    canvas.drawText(line.toString(), margin + indent, y, paint)
                    y += lineHeight
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(candidate)
                }
            }
            if (line.isNotEmpty()) {
                ensureSpace(lineHeight)
                canvas.drawText(line.toString(), margin + indent, y, paint)
                y += lineHeight
            }
        }

        canvas.drawText("Karta Pacjenta", margin, y, titlePaint)
        y += 24f
        val patientName = listOfNotNull(data.patient?.firstName, data.patient?.lastName)
            .joinToString(" ")
            .ifBlank { "Pacjent" }
        canvas.drawText(patientName, margin, y, bodyPaint)
        y += 14f
        canvas.drawText("Wygenerowano: ${timestamp()}", margin, y, metaPaint)
        y += 10f
        canvas.drawText(
            "Projekt studencki — nie jest dokumentacją medyczną.",
            margin,
            y,
            metaPaint,
        )
        y += 22f

        data.sections.forEach { section ->
            ensureSpace(40f)
            canvas.drawText(section.title, margin, y, sectionPaint)
            y += 16f
            if (section.rows.isEmpty()) {
                canvas.drawText("Brak wpisów.", margin + 8f, y, metaPaint)
                y += 14f
            } else {
                section.rows.forEach { row ->
                    val line = section.columns.zip(row)
                        .filter { it.second.isNotBlank() }
                        .joinToString(" • ") { (column, value) -> "$column: $value" }
                    drawWrapped(line, bodyPaint, 13f, indent = 8f)
                }
            }
            y += 12f
        }

        document.finishPage(page)
        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    private fun csvRow(values: List<String>): String = values.joinToString(";") { escape(it) }

    private fun escape(value: String): String {
        val clean = value.replace("\r", " ").replace("\n", " ")
        return if (clean.contains(';') || clean.contains('"')) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }

    private fun timestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
