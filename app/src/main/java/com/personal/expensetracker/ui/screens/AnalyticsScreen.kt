package com.personal.expensetracker.ui.screens

import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.personal.expensetracker.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: ExpenseViewModel = hiltViewModel()) {
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val dailySpending by viewModel.dailySpending.collectAsState()
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val isDark = isSystemInDarkTheme()

    // Pick label/legend colors based on system theme
    val labelColor = if (isDark) Color.WHITE else Color.DKGRAY
    var gridColor  = if (isDark) Color.rgb(80, 80, 80) else Color.LTGRAY

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analytics", fontWeight = FontWeight.Bold)
                        Text("This month's overview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Pie chart ──────────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Spending by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        if (categoryBreakdown.isEmpty()) {
                            Text("No data yet",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        } else {
                            AndroidView(
                                modifier = Modifier.fillMaxWidth().height(280.dp),
                                factory = { context ->
                                    PieChart(context).apply {
                                        description.isEnabled = false
                                        setUsePercentValues(true)
                                        isDrawHoleEnabled = true
                                        holeRadius = 42f
                                        transparentCircleRadius = 47f
                                        setHoleColor(Color.TRANSPARENT)
                                        setEntryLabelColor(Color.WHITE)
                                        setEntryLabelTextSize(11f)
                                        legend.apply {
                                            orientation = Legend.LegendOrientation.VERTICAL
                                            isWordWrapEnabled = true
                                            textSize = 12f
                                            textColor = labelColor
                                        }
                                    }
                                },
                                update = { chart ->
                                    val colors = listOf(
                                        Color.rgb(255, 99, 132), Color.rgb(54, 162, 235),
                                        Color.rgb(255, 206, 86), Color.rgb(75, 192, 192),
                                        Color.rgb(153, 102, 255), Color.rgb(255, 159, 64),
                                        Color.rgb(199, 199, 199), Color.rgb(83, 102, 255),
                                        Color.rgb(255, 99, 255), Color.rgb(99, 255, 132)
                                    )
                                    val entries = categoryBreakdown.entries.toList()
                                        .sortedByDescending { it.value }
                                        .map { (cat, amount) ->
                                            PieEntry(amount.toFloat(),
                                                cat.emoji + " " + cat.displayName)
                                        }
                                    val dataSet = PieDataSet(entries, "").apply {
                                        this.colors = colors.take(entries.size)
                                        valueTextSize = 12f
                                        valueTextColor = Color.WHITE
                                        valueFormatter = PercentFormatter(chart)
                                        sliceSpace = 2f
                                    }
                                    chart.legend.textColor = labelColor
                                    chart.data = PieData(dataSet)
                                    chart.invalidate()
                                }
                            )
                        }
                    }
                }
            }

            // ── Bar chart ──────────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Daily Spending This Month",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        if (dailySpending.isEmpty()) {
                            Text("No data yet",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        } else {
                            AndroidView(
                                modifier = Modifier.fillMaxWidth().height(240.dp),
                                factory = { ctx ->
                                    BarChart(ctx).apply {
                                        description.isEnabled = false
                                        setDrawGridBackground(false)
                                        setFitBars(true)
                                        legend.isEnabled = false
                                        axisRight.isEnabled = false
                                        xAxis.apply {
                                            position = XAxis.XAxisPosition.BOTTOM
                                            granularity = 1f
                                            setDrawGridLines(false)
                                        }
                                    }
                                },
                                update = { chart ->
                                    // Apply theme-aware colors every update
                                    chart.xAxis.apply {
                                        textColor = labelColor
                                        textSize  = 11f
                                    }
                                    chart.axisLeft.apply {
                                        textColor = labelColor
                                        textSize  = 11f
                                    }

                                    val sorted = dailySpending.entries.sortedBy { it.key }
                                    val entries = sorted.mapIndexed { i, (_, amount) ->
                                        BarEntry(i.toFloat(), amount.toFloat())
                                    }
                                    val labels = sorted.map { it.key.toString() }
                                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                                    chart.xAxis.labelCount = labels.size

                                    val dataSet = BarDataSet(entries, "Daily").apply {
                                        color = Color.rgb(54, 162, 235)
                                        valueTextSize  = 9f
                                        valueTextColor = labelColor   // bar top values
                                    }
                                    chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                                    chart.invalidate()
                                }
                            )
                        }
                    }
                }
            }

            // ── Category list ──────────────────────────────────────────────────
            item {
                Text("Category Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            val sortedCategories = categoryBreakdown.entries
                .sortedByDescending { it.value }.toList()
            items(sortedCategories) { (category, amount) ->
                val pct = if (monthlyTotal > 0) (amount / monthlyTotal * 100).toInt() else 0
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${category.emoji} ${category.displayName}",
                                fontWeight = FontWeight.Medium)
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("₹${"%.0f".format(amount)}", fontWeight = FontWeight.Bold)
                            Text("$pct%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}