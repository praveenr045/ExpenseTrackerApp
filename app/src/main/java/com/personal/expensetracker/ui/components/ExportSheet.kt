package com.personal.expensetracker.ui.components

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.expensetracker.data.model.Expense
import com.personal.expensetracker.ui.screens.DatePickerField
import com.personal.expensetracker.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    var dateFrom by remember { mutableStateOf<Long?>(null) }
    var dateTo by remember { mutableStateOf<Long?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Export to Excel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Select a date range to export. Leave blank to export all expenses.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            DatePickerField(label = "From Date", value = dateFrom, onValueChange = { dateFrom = it })
            DatePickerField(label = "To Date", value = dateTo, onValueChange = { dateTo = it })

            if (isExporting) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("Exporting…", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val start = dateFrom ?: 0L
                            val end = if (dateTo != null) {
                                Calendar.getInstance().apply {
                                    timeInMillis = dateTo!!
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                }.timeInMillis
                            } else Long.MAX_VALUE

                            val expenses = viewModel.getExpensesByRange(start, end).first()
                            val result = exportToExcel(context, expenses)
                            isExporting = false
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                            }
                            if (result.startsWith("Exported")) onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private suspend fun exportToExcel(context: Context, expenses: List<Expense>): String {
    return withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Expenses")
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            // Header row
            val headerRow = sheet.createRow(0)
            val headers = listOf("Date", "Description", "Merchant", "Category", "Source", "Amount (₹)")
            headers.forEachIndexed { i, h ->
                val cell = headerRow.createCell(i)
                cell.setCellValue(h)
                val style = workbook.createCellStyle()
                val font = workbook.createFont()
                font.bold = true
                style.setFont(font)
                cell.cellStyle = style
            }

            // Data rows
            expenses.forEachIndexed { index, expense ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(dateFormat.format(Date(expense.timestamp)))
                row.createCell(1).setCellValue(expense.description)
                row.createCell(2).setCellValue(expense.merchant)
                row.createCell(3).setCellValue(expense.category.displayName)
                row.createCell(4).setCellValue(expense.source.name)
                row.createCell(5).setCellValue(expense.amount)
            }

            // Auto-size columns
            (0..5).forEach { sheet.autoSizeColumn(it) }

            // Summary row
            val summaryRow = sheet.createRow(expenses.size + 2)
            summaryRow.createCell(0).setCellValue("TOTAL")
            summaryRow.createCell(5).setCellValue(expenses.sumOf { it.amount })

            // Save to Downloads
            val fileName = "expenses_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.xlsx"
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext "Export failed: couldn't create file"

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            "Exported ${expenses.size} expenses to Downloads/$fileName"
        } catch (e: Exception) {
            "Export failed: ${e.message}"
        }
    }
}