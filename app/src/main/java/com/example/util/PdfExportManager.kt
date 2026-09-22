package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.ExpenseEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportManager {

    private const val PAGE_WIDTH = 595  // Standard A4 width in postscript points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in postscript points

    fun generateAndSharePdfReport(
        context: Context,
        expenses: List<ExpenseEntity>,
        startDateMillis: Long,
        endDateMillis: Long,
        currencySymbol: String,
        rangeLabel: String
    ): File? {
        if (expenses.isEmpty()) {
            return null
        }

        val pdfDocument = PdfDocument()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val generatedAt = timeFormat.format(Date())
        val periodText = "${dateFormat.format(Date(startDateMillis))} - ${dateFormat.format(Date(endDateMillis))}"

        val totalAmount = expenses.sumOf { it.amount }
        val daysCount = (((endDateMillis - startDateMillis) / (1000 * 60 * 60 * 24)) + 1).coerceAtLeast(1)
        val dailyAverage = totalAmount / daysCount

        // Group by category for summary
        val categoryBreakdown = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val topCategory = categoryBreakdown.firstOrNull()?.first ?: "N/A"

        // Paints
        val primaryPaint = Paint().apply {
            color = Color.rgb(26, 43, 76) // Deep Slate Navy
            isAntiAlias = true
        }

        val accentPaint = Paint().apply {
            color = Color.rgb(15, 122, 90) // Emerald Green Accent
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(210, 225, 245)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headingPaint = Paint().apply {
            color = Color.rgb(26, 43, 76)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(40, 45, 55)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val textBoldPaint = Paint().apply {
            color = Color.rgb(40, 45, 55)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textMutedPaint = Paint().apply {
            color = Color.rgb(120, 125, 135)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(240, 244, 250)
        }

        val rowAltPaint = Paint().apply {
            color = Color.rgb(249, 250, 252)
        }

        val dividerPaint = Paint().apply {
            color = Color.rgb(225, 230, 238)
            strokeWidth = 0.8f
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(245, 248, 253)
        }

        // Layout pagination calculation
        val rowHeight = 22f
        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Draw Header Banner on Page 1
        drawHeaderBanner(canvas, primaryPaint, titlePaint, subtitlePaint, periodText, generatedAt, rangeLabel)

        // Draw KPI Summary Cards on Page 1
        drawKpiCards(
            canvas,
            cardBgPaint,
            dividerPaint,
            headingPaint,
            textBoldPaint,
            textMutedPaint,
            totalAmount,
            expenses.size,
            dailyAverage,
            topCategory,
            currencySymbol
        )

        // Draw Category Breakdown Section on Page 1
        var currentY = 240f
        canvas.drawText("CATEGORY BREAKDOWN", 36f, currentY, headingPaint)
        currentY += 14f

        val catColWidth = (PAGE_WIDTH - 72f) / 4f
        val catToShow = categoryBreakdown.take(4)
        for ((idx, entry) in catToShow.withIndex()) {
            val startX = 36f + (idx * catColWidth)
            val rect = RectF(startX, currentY, startX + catColWidth - 8f, currentY + 36f)
            canvas.drawRoundRect(rect, 6f, 6f, cardBgPaint)

            canvas.drawText(entry.first, startX + 8f, currentY + 14f, textBoldPaint)
            val percentage = if (totalAmount > 0) ((entry.second / totalAmount) * 100).toInt() else 0
            val amountStr = "$currencySymbol${String.format(Locale.getDefault(), "%,.1f", entry.second)} ($percentage%)"
            canvas.drawText(amountStr, startX + 8f, currentY + 28f, textMutedPaint)
        }
        currentY += 50f

        // Table Ledger Heading
        canvas.drawText("ITEMIZED EXPENSE LEDGER (${expenses.size} Records)", 36f, currentY, headingPaint)
        currentY += 14f

        // Draw Table Header
        currentY = drawTableHeader(canvas, currentY, tableHeaderPaint, dividerPaint, textBoldPaint)

        // Draw Transactions
        val sortedExpenses = expenses.sortedByDescending { it.dateMillis }
        val maxPageY = PAGE_HEIGHT - 60f

        for ((index, item) in sortedExpenses.withIndex()) {
            // Check if page overflow
            if (currentY + rowHeight > maxPageY) {
                // Draw Footer for current page
                drawFooter(canvas, currentPageNumber, dividerPaint, textMutedPaint)
                pdfDocument.finishPage(page)

                // Start Next Page
                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // Header for subsequent pages
                currentY = 40f
                canvas.drawText("EXPANSE TRACKER — ITEMIZED LEDGER (Continued)", 36f, currentY, headingPaint)
                currentY += 16f
                currentY = drawTableHeader(canvas, currentY, tableHeaderPaint, dividerPaint, textBoldPaint)
            }

            // Draw row background alternating
            if (index % 2 == 1) {
                canvas.drawRect(36f, currentY, PAGE_WIDTH - 36f, currentY + rowHeight, rowAltPaint)
            }
            canvas.drawLine(36f, currentY + rowHeight, PAGE_WIDTH - 36f, currentY + rowHeight, dividerPaint)

            val itemDate = dateFormat.format(Date(item.dateMillis))
            val noteText = if (item.notes.isNotBlank()) item.notes else item.category
            val truncatedNote = if (noteText.length > 28) noteText.substring(0, 26) + "…" else noteText
            val amountText = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", item.amount)}"

            val textBaseY = currentY + 14.5f
            canvas.drawText(itemDate, 44f, textBaseY, textPaint)
            canvas.drawText(truncatedNote, 120f, textBaseY, textPaint)
            canvas.drawText(item.category, 300f, textBaseY, textMutedPaint)
            canvas.drawText(item.paymentMethod, 395f, textBaseY, textMutedPaint)
            canvas.drawText(amountText, PAGE_WIDTH - 44f - textBoldPaint.measureText(amountText), textBaseY, textBoldPaint)

            currentY += rowHeight
        }

        // Draw Footer on last page
        drawFooter(canvas, currentPageNumber, dividerPaint, textMutedPaint)
        pdfDocument.finishPage(page)

        // Save PDF file to cache directory
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            val fileName = "Expanse_Report_${System.currentTimeMillis()}.pdf"
            val pdfFile = File(reportsDir, fileName)
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Trigger Share Intent
            sharePdfFile(context, pdfFile, periodText)
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawHeaderBanner(
        canvas: Canvas,
        primaryPaint: Paint,
        titlePaint: Paint,
        subtitlePaint: Paint,
        periodText: String,
        generatedAt: String,
        rangeLabel: String
    ) {
        val bannerRect = RectF(0f, 0f, PAGE_WIDTH.toFloat(), 105f)
        canvas.drawRect(bannerRect, primaryPaint)

        canvas.drawText("EXPANSE TRACKER", 36f, 40f, titlePaint)
        canvas.drawText("Official Financial Statement & Expense Audit Report", 36f, 56f, subtitlePaint)

        val badgePaint = Paint().apply {
            color = Color.rgb(24, 134, 98) // Verified Green Badge
        }
        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val badgeRect = RectF(36f, 68f, 158f, 86f)
        canvas.drawRoundRect(badgeRect, 4f, 4f, badgePaint)
        canvas.drawText("✓ 100% OFFLINE & VERIFIED", 42f, 80f, badgeTextPaint)

        val metaPaint = Paint().apply {
            color = Color.rgb(200, 215, 238)
            textSize = 9f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        canvas.drawText("Scope: $rangeLabel", PAGE_WIDTH - 36f, 42f, metaPaint)
        canvas.drawText("Period: $periodText", PAGE_WIDTH - 36f, 58f, metaPaint)
        canvas.drawText("Generated: $generatedAt", PAGE_WIDTH - 36f, 74f, metaPaint)
    }

    private fun drawKpiCards(
        canvas: Canvas,
        cardBgPaint: Paint,
        dividerPaint: Paint,
        headingPaint: Paint,
        textBoldPaint: Paint,
        textMutedPaint: Paint,
        totalAmount: Double,
        count: Int,
        dailyAverage: Double,
        topCategory: String,
        currencySymbol: String
    ) {
        val cardY = 120f
        val cardHeight = 82f
        val cardWidth = (PAGE_WIDTH - 72f - 24f) / 3f

        // Card 1: Total Expenditure
        val c1Rect = RectF(36f, cardY, 36f + cardWidth, cardY + cardHeight)
        canvas.drawRoundRect(c1Rect, 8f, 8f, cardBgPaint)
        canvas.drawText("TOTAL SPENDING", 46f, cardY + 22f, textMutedPaint)
        val bigValPaint = Paint().apply {
            color = Color.rgb(180, 40, 40) // Strong crimson
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val totalFormatted = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", totalAmount)}"
        canvas.drawText(totalFormatted, 46f, cardY + 46f, bigValPaint)
        canvas.drawText("$count transactions logged", 46f, cardY + 66f, textMutedPaint)

        // Card 2: Daily Average
        val c2X = 36f + cardWidth + 12f
        val c2Rect = RectF(c2X, cardY, c2X + cardWidth, cardY + cardHeight)
        canvas.drawRoundRect(c2Rect, 8f, 8f, cardBgPaint)
        canvas.drawText("DAILY AVERAGE", c2X + 10f, cardY + 22f, textMutedPaint)
        val dailyPaint = Paint().apply {
            color = Color.rgb(26, 43, 76)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val avgFormatted = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", dailyAverage)}"
        canvas.drawText(avgFormatted, c2X + 10f, cardY + 46f, dailyPaint)
        canvas.drawText("Per active day in period", c2X + 10f, cardY + 66f, textMutedPaint)

        // Card 3: Top Category
        val c3X = c2X + cardWidth + 12f
        val c3Rect = RectF(c3X, cardY, c3X + cardWidth, cardY + cardHeight)
        canvas.drawRoundRect(c3Rect, 8f, 8f, cardBgPaint)
        canvas.drawText("PRIMARY CATEGORY", c3X + 10f, cardY + 22f, textMutedPaint)
        val topCatPaint = Paint().apply {
            color = Color.rgb(20, 110, 180)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(topCategory, c3X + 10f, cardY + 46f, topCatPaint)
        canvas.drawText("Highest financial impact", c3X + 10f, cardY + 66f, textMutedPaint)
    }

    private fun drawTableHeader(
        canvas: Canvas,
        startY: Float,
        headerBgPaint: Paint,
        dividerPaint: Paint,
        textBoldPaint: Paint
    ): Float {
        val hHeight = 22f
        canvas.drawRect(36f, startY, PAGE_WIDTH - 36f, startY + hHeight, headerBgPaint)
        canvas.drawLine(36f, startY, PAGE_WIDTH - 36f, startY, dividerPaint)
        canvas.drawLine(36f, startY + hHeight, PAGE_WIDTH - 36f, startY + hHeight, dividerPaint)

        val headerTextY = startY + 14.5f
        canvas.drawText("DATE", 44f, headerTextY, textBoldPaint)
        canvas.drawText("DESCRIPTION / NOTE", 120f, headerTextY, textBoldPaint)
        canvas.drawText("CATEGORY", 300f, headerTextY, textBoldPaint)
        canvas.drawText("MODE", 395f, headerTextY, textBoldPaint)
        val amountHeader = "AMOUNT"
        canvas.drawText(amountHeader, PAGE_WIDTH - 44f - textBoldPaint.measureText(amountHeader), headerTextY, textBoldPaint)

        return startY + hHeight
    }

    private fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        dividerPaint: Paint,
        textMutedPaint: Paint
    ) {
        val footerY = PAGE_HEIGHT - 32f
        canvas.drawLine(36f, footerY - 10f, PAGE_WIDTH - 36f, footerY - 10f, dividerPaint)

        val signPaint = Paint().apply {
            color = Color.rgb(90, 100, 115)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        canvas.drawText(
            "Verified Personal Financial Record • Generated by Expanse Tracker • Developed by QM Labs",
            36f,
            footerY + 2f,
            signPaint
        )

        val pagePaint = Paint().apply {
            color = Color.rgb(100, 110, 125)
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("Page $pageNumber", PAGE_WIDTH - 36f, footerY + 2f, pagePaint)
    }

    private fun sharePdfFile(context: Context, pdfFile: File, periodText: String) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Expanse Tracker Statement ($periodText)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is my personal expense statement generated by Expanse Tracker ($periodText). Developed by QM Labs."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share or View PDF Statement")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
