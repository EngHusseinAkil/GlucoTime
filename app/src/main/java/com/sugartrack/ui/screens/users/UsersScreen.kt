package com.sugartrack.ui.screens.users

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sugartrack.R
import com.sugartrack.data.UserProfile
import com.sugartrack.ui.components.GlucoDialog
import com.sugartrack.ui.components.GlucoTextField

@Composable
fun UsersScreen(
    profiles: List<UserProfile>,
    currentId: Long,
    onSelected: (UserProfile) -> Unit,
    onAdd: (String) -> Unit,
    onUpdate: (UserProfile) -> Unit,
    onDelete: (UserProfile) -> Unit,
    close: () -> Unit
) {
    var editor by remember { mutableStateOf<UserProfile?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<UserProfile?>(null) }

    GlucoDialog(
        onDismissRequest = close,
        title = stringResource(R.string.users),
        confirmButtonText = stringResource(R.string.cancel),
        onConfirm = close
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            profiles.forEach { profile ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onSelected(profile) }) {
                        Text(
                            text = if (profile.id == currentId) "✓ ${profile.name}" else profile.name,
                            color = if (profile.id == currentId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row {
                        TextButton(onClick = { editor = profile }) { Text(stringResource(R.string.edit_name)) }
                        if (profiles.size > 1) {
                            TextButton(onClick = { deleteTarget = profile }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            TextButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_user))
            }
        }
    }

    if (adding || editor != null) {
        val original = editor
        var name by remember(original?.id, adding) { mutableStateOf(original?.name.orEmpty()) }
        val duplicate = profiles.any { it.id != original?.id && it.name.equals(name.trim(), ignoreCase = true) }
        
        GlucoDialog(
            onDismissRequest = { adding = false; editor = null },
            title = if (adding) stringResource(R.string.add_user) else stringResource(R.string.edit_name),
            confirmButtonText = stringResource(R.string.save),
            onConfirm = {
                if (adding) onAdd(name.trim()) else original?.let { onUpdate(it.copy(name = name.trim())) }
                adding = false
                editor = null
            },
            dismissButtonText = stringResource(R.string.cancel),
            confirmEnabled = name.trim().isNotEmpty() && !duplicate
        ) {
            GlucoTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.user_name)
            )
        }
    }

    deleteTarget?.let { profile ->
        GlucoDialog(
            onDismissRequest = { deleteTarget = null },
            title = stringResource(R.string.delete_user),
            confirmButtonText = stringResource(R.string.delete),
            onConfirm = {
                deleteTarget = null
                onDelete(profile)
            },
            dismissButtonText = stringResource(R.string.cancel)
        ) {
            Text(stringResource(R.string.delete_user_message))
        }
    }
}
