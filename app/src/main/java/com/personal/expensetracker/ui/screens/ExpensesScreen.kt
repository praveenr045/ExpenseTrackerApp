package com.personal.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.expensetracker.data.model.Expense
import com.personal.expensetracker.data.model.ExpenseCategory
import com.personal.expensetracker.ui.components.AddExpenseSheet
import com.personal.expensetracker.ui.components.ExportSheet
import com.personal.expensetracker.ui.components.ExpenseItem
import com.personal.expensetracker.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpenseViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val allExpenses by viewModel.allExpenses.collectAsState()
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editExpense by remember { mutableStateOf<Expense?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var dateFrom by remember { mutableStateOf<Long?>(null) }
    var dateTo by remember { mutableStateOf<Long?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    val filtered = remember(allExpenses, selectedCategory, searchQuery, dateFrom, dateTo) {
        allExpenses
            .filter { selectedCategory == null || it.category == selectedCategory }
            .filter {
                searchQuery.isBlank() ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.merchant.contains(searchQuery, ignoreCase = true)
            }
            .filter { dateFrom == null || it.timestamp >= dateFrom!! }
            .filter {
                if (dateTo == null) true
                else {
                    val endOfDay = Calendar.getInstance().apply {
                        timeInMillis = dateTo!!
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }.timeInMillis
                    it.timestamp <= endOfDay
                }
            }
    }

    val filteredTotal = remember(filtered) { filtered.sumOf { it.amount } }
    val activeFilters = (if (selectedCategory != null) 1 else 0) + (if (dateFrom != null || dateTo != null) 1 else 0)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("All Expenses", fontWeight = FontWeight.Bold)
                        Text("${filtered.size} records", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                actions = {
                    BadgedBox(badge = { if (activeFilters > 0) Badge { Text("$activeFilters") } }) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                    IconButton(onClick = { showExportSheet = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or merchant…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Active filter chips summary
            if (activeFilters > 0) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedCategory?.let { cat ->
                        InputChip(
                            selected = true,
                            onClick = { selectedCategory = null },
                            label = { Text("${cat.emoji} ${cat.displayName}  ×") }
                        )
                    }
                    if (dateFrom != null || dateTo != null) {
                        val fromStr = if (dateFrom != null) dateFormat.format(Date(dateFrom!!)) else "Any"
                        val toStr = if (dateTo != null) dateFormat.format(Date(dateTo!!)) else "Any"
                        InputChip(
                            selected = true,
                            onClick = { dateFrom = null; dateTo = null },
                            label = { Text("$fromStr → $toStr  ×") }
                        )
                    }
                }
            }

            // Filtered total bar
            if (filtered.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total (${filtered.size} items)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "₹${"%.2f".format(filteredTotal)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expenses match your filters.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { expense ->
                        ExpenseItem(
                            expense = expense,
                            onDelete = { viewModel.deleteExpense(expense) },
                            onEdit = { editExpense = expense }
                        )
                    }
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        FilterSheet(
            selectedCategory = selectedCategory,
            dateFrom = dateFrom,
            dateTo = dateTo,
            onApply = { cat, from, to ->
                selectedCategory = cat
                dateFrom = from
                dateTo = to
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showAddSheet) {
        AddExpenseSheet(
            onDismiss = { showAddSheet = false },
            onSave = { amount, desc, category, timestamp ->
                viewModel.addExpense(amount, desc, category = category, timestamp = timestamp)
                showAddSheet = false
            }
        )
    }

    editExpense?.let { expense ->
        AddExpenseSheet(
            existing = expense,
            onDismiss = { editExpense = null },
            onSave = { amount, desc, category, timestamp ->
                viewModel.updateExpense(expense.copy(amount = amount, description = desc, category = category, timestamp = timestamp))
                editExpense = null
            }
        )
    }

    if (showExportSheet) {
        ExportSheet(
            viewModel = viewModel,
            onDismiss = { showExportSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    selectedCategory: ExpenseCategory?,
    dateFrom: Long?,
    dateTo: Long?,
    onApply: (ExpenseCategory?, Long?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var localCategory by remember { mutableStateOf(selectedCategory) }
    var localFrom by remember { mutableStateOf(dateFrom) }
    var localTo by remember { mutableStateOf(dateTo) }
    var catExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Filter Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Category dropdown
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                OutlinedTextField(
                    value = localCategory?.let { "${it.emoji} ${it.displayName}" } ?: "All Categories",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("All Categories") },
                        onClick = { localCategory = null; catExpanded = false }
                    )
                    ExpenseCategory.values().forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.emoji} ${cat.displayName}") },
                            onClick = { localCategory = cat; catExpanded = false }
                        )
                    }
                }
            }

            // Date from picker
            DatePickerField(
                label = "From Date",
                value = localFrom,
                onValueChange = { localFrom = it }
            )

            // Date to picker
            DatePickerField(
                label = "To Date",
                value = localTo,
                onValueChange = { localTo = it }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { localCategory = null; localFrom = null; localTo = null },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear All") }
                Button(
                    onClick = { onApply(localCategory, localFrom, localTo) },
                    modifier = Modifier.weight(1f)
                ) { Text("Apply") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(label: String, value: Long?, onValueChange: (Long?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    OutlinedTextField(
        value = if (value != null) dateFormat.format(Date(value)) else "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("Select date") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            Row {
                if (value != null) {
                    TextButton(onClick = { onValueChange(null) }) { Text("Clear") }
                }
                TextButton(onClick = { showPicker = true }) { Text("Pick") }
            }
        }
    )

    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = value ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(datePickerState.selectedDateMillis)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}