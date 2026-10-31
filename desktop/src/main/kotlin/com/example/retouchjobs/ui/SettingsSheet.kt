package com.example.retouchjobs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.retouchjobs.data.DEFAULT_BLOCKED_DOMAINS

/** На компьютере настройки открываются окном по центру, а не шторкой снизу. */
@Composable
fun SettingsSheet(
    initialKey: String,
    initialBlocked: String,
    initialAutoContacts: Boolean,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, blocked: String, autoContacts: Boolean) -> Unit,
) {
    var key by remember { mutableStateOf(initialKey) }
    var blocked by remember { mutableStateOf(initialBlocked) }
    var autoContacts by remember { mutableStateOf(initialAutoContacts) }
    var keyVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 420.dp, max = 560.dp),
        title = { Text("Настройки", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    label = { Text("Serper API key") },
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (keyVisible) "Скрыть ключ" else "Показать ключ",
                            )
                        }
                    },
                    visualTransformation = if (keyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    supportingText = {
                        Text("Ключ берётся в кабинете serper.dev и хранится на компьютере в зашифрованном виде (защита Windows, DPAPI).")
                    },
                )

                OutlinedTextField(
                    value = blocked,
                    onValueChange = { blocked = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 2,
                    maxLines = 4,
                    label = { Text("Исключить сайты") },
                    leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null) },
                    supportingText = {
                        Text("Домены через запятую. Они добавляются в запрос как -site: и отфильтровываются в результатах.")
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Проверять объявления автоматически", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "После поиска приложение откроет страницы объявлений: найдёт контакты, проверит, " +
                                "не закрыта ли вакансия и можно ли работать удалённо. Расходует трафик — можно отключить и проверять по кнопке.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = autoContacts, onCheckedChange = { autoContacts = it })
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { blocked = DEFAULT_BLOCKED_DOMAINS }) {
                Text("Сбросить список")
            }
        },
        confirmButton = {
            Button(onClick = { onSave(key, blocked, autoContacts) }) { Text("Сохранить") }
        },
    )
}
