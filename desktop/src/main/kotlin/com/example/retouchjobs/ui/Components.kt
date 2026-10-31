package com.example.retouchjobs.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.retouchjobs.data.Category
import com.example.retouchjobs.data.Contact
import com.example.retouchjobs.data.ContactType
import com.example.retouchjobs.data.ListingStatus
import com.example.retouchjobs.data.Match
import com.example.retouchjobs.data.Period
import com.example.retouchjobs.data.RemoteStatus
import com.example.retouchjobs.data.Settings
import com.example.retouchjobs.data.Vacancy

@Composable
fun HeroCard(
    title: String,
    text: String,
    icon: ImageVector,
    loading: Boolean,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(cs.primary, cs.tertiary))),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = cs.onPrimary.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-20).dp)
                .size(150.dp),
        )
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onPrimary,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onPrimary.copy(alpha = 0.9f),
            )
            Button(
                onClick = onSearch,
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.onPrimary,
                    contentColor = cs.primary,
                    disabledContainerColor = cs.onPrimary.copy(alpha = 0.6f),
                    disabledContentColor = cs.primary.copy(alpha = 0.8f),
                ),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = cs.primary,
                    )
                } else {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Ищем…" else "Найти вакансии")
            }
        }
    }
}

@Composable
fun ApiKeyBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Key, contentDescription = null, tint = cs.onTertiaryContainer)
            Text(
                text = "Для поиска нужен API-ключ Serper.dev",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClick) { Text("Добавить") }
        }
    }
}

@Composable
fun FilterRow(
    settings: Settings,
    onPeriod: (Period) -> Unit,
    onRemote: (Boolean) -> Unit,
    onOnlyVacancies: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Опубликовано за",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Period.entries.forEach { p ->
                AppFilterChip(
                    label = p.label,
                    selected = settings.period == p,
                    onClick = { onPeriod(p) },
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppFilterChip(
                label = "Удалённо",
                selected = settings.remote,
                onClick = { onRemote(!settings.remote) },
            )
            AppFilterChip(
                label = "Только вакансии",
                selected = settings.onlyVacancies,
                onClick = { onOnlyVacancies(!settings.onlyVacancies) },
            )
        }
    }
}

@Composable
private fun AppFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else null,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
fun ResultsHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Найдено: $count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "сначала самые подходящие",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun VacancyCard(
    vacancy: Vacancy,
    category: Category,
    contacts: ContactsState?,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onLoadContacts: () -> Unit,
    onContact: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DomainAvatar(vacancy.domain)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vacancy.domain,
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    vacancy.date?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = vacancy.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (vacancy.snippet.isNotBlank()) {
                Text(
                    text = vacancy.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CheckRow(vacancy)
            ContactsSection(
                vacancy = vacancy,
                state = contacts,
                onLoad = onLoadContacts,
                onContact = onContact,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MatchBadge(vacancy.match, category)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShare) {
                    Icon(Icons.Rounded.Share, contentDescription = "Поделиться")
                }
                FilledTonalButton(onClick = onOpen) {
                    Text("Открыть")
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactsSection(
    vacancy: Vacancy,
    state: ContactsState?,
    onLoad: () -> Unit,
    onContact: (Contact) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val shown = (state as? ContactsState.Done)?.contacts ?: vacancy.contacts

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (shown.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                shown.forEach { contact ->
                    AssistChip(
                        onClick = { onContact(contact) },
                        label = { Text(contact.label, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                contactIcon(contact.type),
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
        }

        when (state) {
            ContactsState.Loading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    text = "Проверяем страницу объявления…",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
            }

            is ContactsState.Done -> if (shown.isEmpty()) {
                Text(
                    text = "Контактов на странице не нашли — откройте объявление.",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
            }

            ContactsState.Failed -> if (shown.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Страница не загрузилась.",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                    )
                    TextButton(onClick = onLoad) { Text("Повторить") }
                }
            }

            null -> if (shown.isEmpty()) {
                TextButton(
                    onClick = onLoad,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Contacts,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Проверить объявление")
                }
            }
        }
    }
}

private fun contactIcon(type: ContactType): ImageVector = when (type) {
    ContactType.PHONE -> Icons.Rounded.Phone
    ContactType.EMAIL -> Icons.Rounded.Email
    ContactType.TELEGRAM -> Icons.AutoMirrored.Rounded.Send
    ContactType.WHATSAPP -> Icons.Rounded.Chat
    ContactType.VK -> Icons.Rounded.Public
}

@Composable
private fun DomainAvatar(domain: String) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (domain.hashCode().mod(3)) {
        0 -> cs.primaryContainer to cs.onPrimaryContainer
        1 -> cs.secondaryContainer to cs.onSecondaryContainer
        else -> cs.tertiaryContainer to cs.onTertiaryContainer
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = domain.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

private val foundFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru"))

private fun formatFound(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(foundFormat)

/** Статус вакансии и условия удалёнки. «Не проверена» и «не установлено» — это не «да». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckRow(vacancy: Vacancy) {
    val cs = MaterialTheme.colorScheme
    val check = vacancy.check

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (check?.status) {
                ListingStatus.CONFIRMED -> InfoBadge(
                    "Открыта: подтверждено", cs.primaryContainer, cs.onPrimaryContainer, Icons.Rounded.Verified,
                )
                ListingStatus.STALE -> InfoBadge(
                    "Возможно устарела", cs.tertiaryContainer, cs.onTertiaryContainer,
                )
                ListingStatus.CLOSED -> InfoBadge(
                    "Закрыта", cs.errorContainer, cs.onErrorContainer,
                )
                ListingStatus.UNVERIFIED -> InfoBadge(
                    "Актуальность не подтверждена", cs.surfaceVariant, cs.onSurfaceVariant,
                )
                null -> InfoBadge(
                    "Страница не проверена", cs.surfaceVariant, cs.onSurfaceVariant,
                )
            }
            when (vacancy.remote) {
                RemoteStatus.YES -> InfoBadge(
                    "Удалённо: да", cs.secondaryContainer, cs.onSecondaryContainer,
                )
                RemoteStatus.NO -> InfoBadge(
                    "Удалённо: нет", cs.errorContainer, cs.onErrorContainer,
                )
                RemoteStatus.UNKNOWN -> InfoBadge(
                    "Удалённо: не установлено", cs.surfaceVariant, cs.onSurfaceVariant,
                )
            }
        }

        val meta = listOfNotNull(
            vacancy.foundAt.takeIf { it > 0 }?.let { "Найдено ${formatFound(it)}" },
            check?.postedDaysAgo?.let { if (it == 0) "опубликовано сегодня" else "опубликовано $it дн. назад" },
            check?.note,
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoBadge(label: String, bg: Color, fg: Color, icon: ImageVector? = null) {
    Surface(shape = RoundedCornerShape(50), color = bg, contentColor = fg) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MatchBadge(match: Match, category: Category) {
    val cs = MaterialTheme.colorScheme
    val (label, bg, fg) = when (match) {
        Match.EXACT -> Triple(category.exactLabel, cs.tertiaryContainer, cs.onTertiaryContainer)
        Match.GOOD -> Triple(category.goodLabel, cs.secondaryContainer, cs.onSecondaryContainer)
        Match.WEAK -> Triple("Возможно по теме", cs.surfaceVariant, cs.onSurfaceVariant)
    }
    Surface(shape = RoundedCornerShape(50), color = bg, contentColor = fg) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (match == Match.EXACT) {
                Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .alpha(pulse),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(placeholderColor()),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlaceholderBar(width = 120.dp, height = 12.dp)
                    PlaceholderBar(width = 70.dp, height = 10.dp)
                }
            }
            PlaceholderBar(width = null, height = 16.dp)
            PlaceholderBar(width = 220.dp, height = 16.dp)
            PlaceholderBar(width = null, height = 12.dp)
            PlaceholderBar(width = 180.dp, height = 12.dp)
        }
    }
}

@Composable
private fun placeholderColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

@Composable
private fun PlaceholderBar(width: Dp?, height: Dp) {
    val base = Modifier
        .height(height)
        .clip(RoundedCornerShape(6.dp))
        .background(placeholderColor())
    Box(if (width == null) base.fillMaxWidth() else base.width(width))
}

@Composable
fun ErrorCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cs.errorContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = cs.onErrorContainer)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onErrorContainer,
                )
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.error,
                    contentColor = cs.onError,
                ),
            ) { Text(actionLabel) }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Rounded.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Ничего не нашлось", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Попробуйте увеличить период или отключить фильтр «Только вакансии».",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun IntroHint(modifier: Modifier = Modifier) {
    Text(
        text = "Один поиск — это 4 запроса к Serper.dev с разными формулировками. " +
            "Результаты с исключённых сайтов отбрасываются.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    )
}
