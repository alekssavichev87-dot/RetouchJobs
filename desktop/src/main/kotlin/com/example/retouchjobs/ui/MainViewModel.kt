package com.example.retouchjobs.ui

import com.example.retouchjobs.data.Category
import com.example.retouchjobs.data.Contact
import com.example.retouchjobs.data.ContactExtractor
import com.example.retouchjobs.data.ContactFetcher
import com.example.retouchjobs.data.Match
import com.example.retouchjobs.data.Period
import com.example.retouchjobs.data.SerperClient
import com.example.retouchjobs.data.SerperException
import com.example.retouchjobs.data.Settings
import com.example.retouchjobs.data.SettingsStore
import com.example.retouchjobs.data.Vacancy
import com.example.retouchjobs.data.VacancyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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
    private val fetcher = ContactFetcher()

    /** Не больше 4 страниц объявлений грузятся одновременно. */
    private val permits = Semaphore(4)

    val settings: StateFlow<Settings> = store.settings

    /** У каждого раздела свои результаты. */
    private val _states = MutableStateFlow(Category.entries.associateWith { SearchState() })
    val states: StateFlow<Map<Category, SearchState>> = _states.asStateFlow()

    /** Состояние поиска контактов по адресу объявления. */
    private val _contacts = MutableStateFlow<Map<String, ContactsState>>(emptyMap())
    val contacts: StateFlow<Map<String, ContactsState>> = _contacts.asStateFlow()

    private val searchJobs = mutableMapOf<Category, Job>()
    private val contactJobs = mutableMapOf<Category, Job>()

    private fun updateState(category: Category, transform: (SearchState) -> SearchState) {
        _states.update { it + (category to transform(it.getValue(category))) }
    }

    fun search(category: Category) {
        searchJobs[category]?.cancel()
        contactJobs[category]?.cancel()
        searchJobs[category] = scope.launch {
            updateState(category) { it.copy(loading = true, error = null) }
            try {
                val cfg = store.settings.first()
                val items = repository.search(cfg, category)

                val oldUrls = _states.value.getValue(category).items.map { it.url }.toSet()
                _contacts.update { it - oldUrls }
                updateState(category) { SearchState(items = items, searched = true) }

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

    /** Ручной поиск контактов для одной карточки (или повтор после ошибки). */
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
                ContactsState.Done(
                    ContactExtractor.merge(vacancy.contacts, fetcher.fetch(vacancy.url)),
                )
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

    /** Период и «Удалённо» общие: повторяем поиск в тех разделах, где уже искали. */
    private fun refreshSearched() {
        Category.entries
            .filter { _states.value.getValue(it).searched }
            .forEach(::search)
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
}
