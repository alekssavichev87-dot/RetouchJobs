package com.example.retouchjobs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.retouchjobs.data.DEFAULT_BLOCKED_DOMAINS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    initialKey: String,
    initialBlocked: String,
    initialAutoContacts: Boolean,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, blocked: String, autoContacts: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var key by rememberSaveable { mutableStateOf(initialKey) }
    var blocked by rememberSaveable { mutableStateOf(initialBlocked) }
    var autoContacts by rememberSaveable { mutableStateOf(initialAutoContacts) }
    var keyVisible by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall)

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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = {
                    Text("Ключ берётся в кабинете serper.dev и хранится только на этом устройстве.")
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
                    Text("Искать контакты автоматически", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "После поиска приложение откроет страницы объявлений и найдёт телефоны, " +
                            "e-mail и мессенджеры. Расходует трафик — можно отключить и искать по кнопке.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = autoContacts, onCheckedChange = { autoContacts = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { blocked = DEFAULT_BLOCKED_DOMAINS }) {
                    Text("Сбросить список")
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSave(key, blocked, autoContacts) }) { Text("Сохранить") }
            }
        }
    }
}
