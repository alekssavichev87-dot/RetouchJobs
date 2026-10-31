package com.example.retouchjobs.ui

import com.example.retouchjobs.data.Category
import com.example.retouchjobs.data.Contact
import com.example.retouchjobs.data.ContactExtractor
import com.example.retouchjobs.data.Match
import com.example.retouchjobs.data.PageFetcher
import com.example.retouchjobs.data.Period
import com.example.retouchjobs.data.RemoteStatus
import com.example.retouchjobs.data.SerperClient
import com.example.retouchjobs.data.SerperException
import com.example.retouchjobs.data.Settings
import com.example.retouchjobs.data.AppDirs
import com.example.retouchjobs.data.SettingsStore
import com.example.retouchjobs.data.StoredData
import com.example.retouchjobs.data.Vacancy
import com.example.retouchjobs.data.VacancyRepository
import com.example.retouchjobs.data.VacancyStorage
import com.example.retouchjobs.data.PageCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

data class SearchError(val message: String, val kind: SerperException.Kind?)

data class SearchState(
    val loading: Boolean = false,
    val items: List<Vacancy> = emptyList(),
    val searched: Boolean = false,
    val error: SearchError? = null,
)

sealed interface ContactsState {
    data object Loading : ContactsState
    data class Done(val contacts: List<Contact>) : ContactsState
    data object Failed : ContactsState
}

class MainViewModel {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val store = SettingsStore()
    private val repository = VacancyRepository(SerperClient())
    private val fetcher = PageFetcher()
    private val storage = VacancyStorage(File(AppDirs.dir, "vacancies.json"))

    /** Не больше 4 страниц объявлений грузятся одновременно. */
    private val permits = Semaphore(4)

    val settings: StateFlow<Settings> = store.settings

    private val stored: StoredData = storage.load()

    /** Когда каждая вакансия впервые попалась в поиске (url → мс). */
    private val firstSeen = stored.firstSeen.toMutableMap()

    /** У каждого раздела свои результаты; после перезапуска берутся из истории. */
    private val _states = MutableStateFlow(
        Category.entries.associateWith { c ->
            val items = stored.results[c.name].orEmpty()
            SearchState(items = items, searched = items.isNotEmpty())
        },
    )
    val states: StateFlow<Map<Category, SearchState>> = _states.asStateFlow()

    /** Состояние поиска контактов по адресу объявления. */
    private val _contacts = MutableStateFlow<Map<String, ContactsState>>(emptyMap())
    val contacts: StateFlow<Map<String, ContactsState>> = _contacts.asStateFlow()

    private val searchJobs = mutableMapOf<Category, Job>()
    private val contactJobs = mutableMapOf<Category, Job>()
    private var persistJob: Job? = null

    /** Разделы, в которых искали в этом сеансе: только их обновляем при смене фильтров. */
    private val searchedThisSession = mutableSetOf<Category>()

    private fun updateState(category: Category, transform: (SearchState) -> SearchState) {
        _states.update { it + (category to transform(it.getValue(category))) }
    }

    /** Сохраняет историю на диск с небольшой задержкой, чтобы не писать файл на каждый чих. */
    private fun persistSoon() {
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(500)
            val snapshot = StoredData(
                firstSeen = firstSeen.toMap(),
                results = _states.value.mapKeys { it.key.name }.mapValues { it.value.items },
            )
            withContext(Dispatchers.IO) { runCatching { storage.save(snapshot) } }
        }
    }

    private fun trimFirstSeen() {
        val extra = firstSeen.size - MAX_REMEMBERED
        if (extra > 0) {
            firstSeen.entries.sortedBy { it.value }.take(extra).forEach { firstSeen.remove(it.key) }
        }
    }

    fun search(category: Category) {
        searchedThisSession.add(category)
        searchJobs[category]?.cancel()
        contactJobs[category]?.cancel()
        searchJobs[category] = scope.launch {
            updateState(category) { it.copy(loading = true, error = null) }
            try {
                val cfg = store.settings.first()
                val found = repository.search(cfg, category)

                // Дата первого обнаружения и результаты прошлых проверок переходят на новые карточки.
                val now = System.currentTimeMillis()
                val previous = _states.value.getValue(category).items.associateBy { it.url }
                val items = found.map { v ->
                    val old = previous[v.url]
                    v.copy(
                        foundAt = firstSeen.getOrPut(v.url) { now },
                        check = old?.check,
                        contacts = ContactExtractor.merge(v.contacts, old?.contacts.orEmpty()),
                        remote = if (v.remote == RemoteStatus.UNKNOWN) {
                            old?.remote ?: RemoteStatus.UNKNOWN
                        } else {
                            v.remote
                        },
                    )
                }
                trimFirstSeen()

                _contacts.update { it - previous.keys }
                updateState(category) { SearchState(items = items, searched = true) }
                persistSoon()

                if (cfg.autoContacts) {
                    val targets = if (cfg.onlyVacancies) {
                        items.filter { it.isVacancy && it.match != Match.WEAK }
                    } else {
                        items
                    }
                    loadAllContacts(category, targets)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SerperException) {
                updateState(category) {
                    it.copy(
                        loading = false,
                        searched = true,
                        error = SearchError(e.message.orEmpty(), e.kind),
                    )
                }
            } catch (e: Exception) {
                updateState(category) {
                    it.copy(
                        loading = false,
                        searched = true,
                        error = SearchError("Что-то пошло не так: ${e.message}", null),
                    )
                }
            }
        }
    }

    /** Ручная проверка одной карточки (или повтор после ошибки). */
    fun loadContacts(vacancy: Vacancy) {
        _contacts.update { it + (vacancy.url to ContactsState.Loading) }
        scope.launch { fetchOne(vacancy) }
    }

    private fun loadAllContacts(category: Category, items: List<Vacancy>) {
        _contacts.update { it + items.associate { v -> v.url to ContactsState.Loading } }
        contactJobs[category] = scope.launch {
            items.map { v -> async { fetchOne(v) } }.awaitAll()
        }
    }

    private suspend fun fetchOne(vacancy: Vacancy) {
        val result: ContactsState = permits.withPermit {
            try {
                val page = fetcher.fetch(vacancy.url)
                val merged = ContactExtractor.merge(vacancy.contacts, page.contacts)
                applyPage(vacancy.url, merged, page.check)
                ContactsState.Done(merged)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (vacancy.contacts.isNotEmpty()) {
                    ContactsState.Done(vacancy.contacts)
                } else {
                    ContactsState.Failed
                }
            }
        }
        _contacts.update { it + (vacancy.url to result) }
    }

    /** Записывает итог проверки страницы в карточку (во всех разделах, где она есть). */
    private fun applyPage(url: String, contacts: List<Contact>, check: PageCheck) {
        _states.update { map ->
            map.mapValues { (_, s) ->
                if (s.items.none { it.url == url }) {
                    s
                } else {
                    s.copy(
                        items = s.items.map { v ->
                            if (v.url != url) {
                                v
                            } else {
                                v.copy(
                                    contacts = contacts,
                                    check = check,
                                    remote = if (check.remote != RemoteStatus.UNKNOWN) check.remote else v.remote,
                                )
                            }
                        },
                    )
                }
            }
        }
        persistSoon()
    }

    /** Период и «Удалённо» общие: повторяем поиск в тех разделах, где уже искали. */
    private fun refreshSearched() {
        searchedThisSession.toList().forEach(::search)
    }

    fun setPeriod(period: Period) {
        scope.launch {
            store.setPeriod(period)
            refreshSearched()
        }
    }

    fun setRemote(value: Boolean) {
        scope.launch {
            store.setRemote(value)
            refreshSearched()
        }
    }

    /** Применяется к уже загруженным результатам, без нового запроса. */
    fun setOnlyVacancies(value: Boolean) {
        scope.launch { store.setOnlyVacancies(value) }
    }

    fun saveSettings(apiKey: String, blocked: String, autoContacts: Boolean) {
        scope.launch {
            store.saveSettings(apiKey, blocked, autoContacts)
            _states.update { map ->
                map.mapValues { (_, s) ->
                    val kind = s.error?.kind
                    if (kind == SerperException.Kind.NO_KEY || kind == SerperException.Kind.BAD_KEY) {
                        s.copy(error = null)
                    } else {
                        s
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_REMEMBERED = 3000
    }
}
