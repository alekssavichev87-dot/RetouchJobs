package com.example.retouchjobs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.retouchjobs.data.Category
import com.example.retouchjobs.data.Contact
import com.example.retouchjobs.data.ContactType
import com.example.retouchjobs.data.Match
import com.example.retouchjobs.data.SerperException
import com.example.retouchjobs.data.Settings
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

private fun Category.icon(): ImageVector = when (this) {
    Category.RETOUCH -> Icons.Rounded.AutoFixHigh
    Category.DEV -> Icons.Rounded.Code
    Category.DESIGN -> Icons.Rounded.Palette
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val states by vm.states.collectAsState()
    val settings by vm.settings.collectAsState()
    val contacts by vm.contacts.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(Category.RETOUCH) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    fun copy(text: String, message: String) {
        clipboard.setText(AnnotatedString(text))
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun openContact(contact: Contact) {
        when (contact.type) {
            // Звонить с компьютера нечем: копируем номер, чтобы набрать с телефона.
            ContactType.PHONE -> copy(contact.uri.removePrefix("tel:"), "Номер скопирован")
            ContactType.EMAIL -> {
                val opened = runCatching {
                    Desktop.getDesktop().mail(URI(contact.uri))
                }.isSuccess
                if (!opened) copy(contact.uri.removePrefix("mailto:"), "Адрес скопирован")
            }
            else -> uriHandler.openUri(contact.uri)
        }
    }

    val state = states.getValue(category)
    val cfg: Settings? = settings
    val strictFilter = cfg?.onlyVacancies != false
    val visible = remember(state.items, strictFilter) {
        if (strictFilter) {
            state.items.filter { it.isVacancy && it.match != Match.WEAK }
        } else {
            state.items
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = cs.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = category.screenTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = cs.background,
                    scrolledContainerColor = cs.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            NavigationBar(containerColor = cs.surfaceContainer) {
                Category.entries.forEach { c ->
                    NavigationBarItem(
                        selected = category == c,
                        onClick = { category = c },
                        icon = { Icon(c.icon(), contentDescription = null) },
                        label = { Text(c.tabLabel, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // key(category): у каждого раздела свежая прокрутка списка.
            key(category) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "hero") {
                        HeroCard(
                            title = category.heroTitle,
                            text = category.heroText,
                            icon = category.icon(),
                            loading = state.loading,
                            onSearch = { vm.search(category) },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    if (cfg != null && cfg.apiKey.isBlank()) {
                        item(key = "nokey") {
                            ApiKeyBanner(
                                onClick = { showSettings = true },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (cfg != null) {
                        item(key = "filters") {
                            FilterRow(
                                settings = cfg,
                                onPeriod = vm::setPeriod,
                                onRemote = vm::setRemote,
                                onOnlyVacancies = vm::setOnlyVacancies,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    state.error?.let { err ->
                        item(key = "error") {
                            val needsKey = err.kind == SerperException.Kind.NO_KEY ||
                                err.kind == SerperException.Kind.BAD_KEY
                            ErrorCard(
                                message = err.message,
                                actionLabel = if (needsKey) "Открыть настройки" else "Повторить",
                                onAction = {
                                    if (needsKey) showSettings = true else vm.search(category)
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    when {
                        state.loading && state.items.isEmpty() -> {
                            items(4, key = { "skeleton$it" }) { SkeletonCard() }
                        }

                        !state.searched -> {
                            item(key = "intro") { IntroHint() }
                        }

                        state.error == null && visible.isEmpty() -> {
                            item(key = "empty") { EmptyState(modifier = Modifier.animateItem()) }
                        }
                    }

                    if (visible.isNotEmpty()) {
                        item(key = "header") {
                            ResultsHeader(count = visible.size, modifier = Modifier.animateItem())
                        }
                        items(visible, key = { it.url }) { vacancy ->
                            VacancyCard(
                                vacancy = vacancy,
                                category = category,
                                contacts = contacts[vacancy.url],
                                onOpen = { uriHandler.openUri(vacancy.url) },
                                // На компьютере «Поделиться» копирует название и ссылку.
                                onShare = { copy("${vacancy.title}\n${vacancy.url}", "Ссылка скопирована") },
                                onLoadContacts = { vm.loadContacts(vacancy) },
                                onContact = { openContact(it) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings && cfg != null) {
        SettingsSheet(
            initialKey = cfg.apiKey,
            initialBlocked = cfg.blockedDomains,
            initialAutoContacts = cfg.autoContacts,
            onDismiss = { showSettings = false },
            onSave = { apiKey, blocked, auto ->
                vm.saveSettings(apiKey, blocked, auto)
                showSettings = false
            },
        )
    }
}
