package com.example.retouchjobs.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retouchjobs.data.Category
import com.example.retouchjobs.data.Contact
import com.example.retouchjobs.data.ContactType
import com.example.retouchjobs.data.Match
import com.example.retouchjobs.data.SerperException
import com.example.retouchjobs.data.Vacancy

private fun Category.icon(): ImageVector = when (this) {
    Category.RETOUCH -> Icons.Rounded.AutoFixHigh
    Category.DEV -> Icons.Rounded.Code
    Category.DESIGN -> Icons.Rounded.Palette
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val states by vm.states.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf(Category.RETOUCH) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    val state = states.getValue(category)
    val cfg = settings
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
        PullToRefreshBox(
            isRefreshing = state.loading && state.items.isNotEmpty(),
            onRefresh = { vm.search(category) },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // key(category): у каждого раздела свежая прокрутка списка.
            key(category) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
                                onShare = { shareVacancy(context, vacancy) },
                                onLoadContacts = { vm.loadContacts(vacancy) },
                                onContact = { openContact(context, it) },
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
            onSave = { key, blocked, auto ->
                vm.saveSettings(key, blocked, auto)
                showSettings = false
            },
        )
    }
}

private fun openContact(context: Context, contact: Contact) {
    val uri = Uri.parse(contact.uri)
    val intent = when (contact.type) {
        ContactType.PHONE -> Intent(Intent.ACTION_DIAL, uri)
        ContactType.EMAIL -> Intent(Intent.ACTION_SENDTO, uri)
        else -> Intent(Intent.ACTION_VIEW, uri)
    }
    runCatching { context.startActivity(intent) }
}

private fun shareVacancy(context: Context, vacancy: Vacancy) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${vacancy.title}\n${vacancy.url}")
    }
    context.startActivity(Intent.createChooser(intent, null))
}
