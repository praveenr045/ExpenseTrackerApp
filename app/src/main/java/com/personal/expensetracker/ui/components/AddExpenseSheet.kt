package com.personal.expensetracker.ui.components

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personal.expensetracker.data.model.Expense
import com.personal.expensetracker.data.model.ExpenseCategory
import com.personal.expensetracker.voice.VoiceParser
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    existing: Expense? = null,
    onDismiss: () -> Unit,
    onSave: (Double, String, ExpenseCategory, Long) -> Unit
) {
    val isEditing = existing != null
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(existing?.category ?: ExpenseCategory.OTHER) }
    var selectedTimestamp by remember { mutableStateOf(existing?.timestamp ?: System.currentTimeMillis()) }
    var isListening by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val dateTimeFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val calendar = remember(selectedTimestamp) {
        Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
    }

    DisposableEffect(Unit) { onDispose { speechRecognizer.destroy() } }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { voiceStatus = "Listening…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(r: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false; voiceStatus = "Processing…" }
            override fun onError(e: Int) { isListening = false; voiceStatus = "Couldn't hear. Try again." }
            override fun onPartialResults(r: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
            override fun onResults(results: Bundle?) {
                val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                voiceStatus = "\"$transcript\""
                val parsed = VoiceParser.parse(transcript)
                if (parsed != null) {
                    amount = parsed.amount.toString()
                    description = parsed.description
                    selectedCategory = parsed.category
                } else {
                    voiceStatus = "Couldn't parse amount. Fill manually."
                }
            }
        })
        speechRecognizer.startListening(intent)
        isListening = true
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (isEditing) "Edit Expense" else "Add Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Voice input button (only when adding new)
            if (!isEditing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (isListening) { speechRecognizer.stopListening(); isListening = false }
                            else startVoiceInput()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (isListening) MaterialTheme.colorScheme.onError
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isListening) "Stop" else "Speak Expense")
                    }
                }
                if (voiceStatus.isNotBlank()) {
                    Text(voiceStatus, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category dropdown
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                OutlinedTextField(
                    value = "${selectedCategory.emoji} ${selectedCategory.displayName}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    ExpenseCategory.values().forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.emoji} ${cat.displayName}") },
                            onClick = { selectedCategory = cat; catExpanded = false }
                        )
                    }
                }
            }

            // Date & Time picker
            OutlinedTextField(
                value = dateTimeFormat.format(Date(selectedTimestamp)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date & Time") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Row {
                        TextButton(onClick = { showDatePicker = true }) { Text("Date") }
                        TextButton(onClick = { showTimePicker = true }) { Text("Time") }
                    }
                }
            )

            // Save button
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal != null && amountVal > 0 && description.isNotBlank()) {
                        onSave(amountVal, description, selectedCategory, selectedTimestamp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.toDoubleOrNull() != null && description.isNotBlank()
            ) {
                Text(if (isEditing) "Save Changes" else "Add Expense")
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val pickedDate = datePickerState.selectedDateMillis
                    if (pickedDate != null) {
                        // Keep existing time, swap date
                        val pickedCal = Calendar.getInstance().apply { timeInMillis = pickedDate }
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = selectedTimestamp
                            set(Calendar.YEAR, pickedCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, pickedCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, pickedCal.get(Calendar.DAY_OF_MONTH))
                        }
                        selectedTimestamp = newCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    // Time picker dialog
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = selectedTimestamp
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }
                    selectedTimestamp = newCal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}