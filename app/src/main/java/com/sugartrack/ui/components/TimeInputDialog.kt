package com.sugartrack.ui.components

import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun TimeInputDialog(
    hour24: Int,
    minute: Int,
    onSave: (Int, Int) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(hour24, minute) {
        TimePickerDialog(
            context,
            { _, hour, selectedMinute -> onSave(hour, selectedMinute) },
            hour24,
            minute,
            false
        ).apply {
            setOnCancelListener { onClose() }
            setOnDismissListener { onClose() }
            show()
        }
    }
}
