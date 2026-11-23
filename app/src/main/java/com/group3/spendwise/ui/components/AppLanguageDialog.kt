package com.group3.spendwise.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.group3.spendwise.R
import com.group3.spendwise.ui.settings.AppLanguageManager

/** Language picker in the Dashboard account menu. */
@Composable
fun AppLanguageDialog(onDismiss: () -> Unit) {
    // Keep these choices local to this composable to avoid duplicate type declarations.
    // Each pair is (language tag, label string). An empty tag means "use the device language".
    val languageChoices = listOf(
        "" to R.string.language_system,
        "en" to R.string.language_english,
        "fr" to R.string.language_french
    )
    // Work out which option is currently active so it can be shown on the button.
    val selectedTag = AppLanguageManager.selectedLanguageTag()
    val selectedLabelId = languageChoices.firstOrNull { it.first == selectedTag }
        ?.second ?: R.string.language_system
    // Controls whether the dropdown list is open.
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_label)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.language_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Button showing the current language; tapping it opens the dropdown.
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(selectedLabelId),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // One menu row per language, with a check mark next to the active one.
                        languageChoices.forEach { (tag, labelId) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelId)) },
                                onClick = {
                                    expanded = false
                                    onDismiss()
                                    // Only switch if the user picked a different language (switching recreates the activity).
                                    if (tag != selectedTag) {
                                        AppLanguageManager.selectLanguage(tag)
                                    }
                                },
                                trailingIcon = if (tag == selectedTag) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done))
            }
        }
    )
}
