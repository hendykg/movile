package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.PredictionEntity
import com.example.data.local.entity.StadiumEntity
import com.example.data.local.entity.SyncLogEntity
import com.example.data.model.Group
import com.example.data.model.LeaderboardEntry
import com.example.data.model.UserProfile
import com.example.data.remote.NetworkModule
import com.example.data.repository.QuinielaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QuinielaViewModel(application: Application) : AndroidViewModel(application) {

    // Simple service locator for dependency injection without gradle classpath conflicts
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "quiniela_database"
        ).fallbackToDestructiveMigration().build()
    }

    private val sessionManager by lazy { SessionManager(application) }
    private val networkModule by lazy { NetworkModule(application) }

    val repository: QuinielaRepository by lazy {
        QuinielaRepository(
            apiService = networkModule.apiService,
            matchDao = database.matchDao(),
            predictionDao = database.predictionDao(),
            stadiumDao = database.stadiumDao(),
            syncLogDao = database.syncLogDao(),
            sessionManager = sessionManager
        )
    }

    // UI States
    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    // Loading & Network communication triggers (Requirement 32)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Reactive database streams (Offline-first source of truth, Requirement 25 & 27)
    val matches: StateFlow<List<MatchEntity>> = repository.getLocalMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val predictions: StateFlow<List<PredictionEntity>> = repository.getLocalPredictions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stadiums: StateFlow<List<StadiumEntity>> = repository.getLocalStadiums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.getSyncLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // API state hooks
    private val _groupsList = MutableStateFlow<List<Group>>(emptyList())
    val groupsList: StateFlow<List<Group>> = _groupsList.asStateFlow()

    private val _lastCreatedGroup = MutableStateFlow<Group?>(null)
    val lastCreatedGroup: StateFlow<Group?> = _lastCreatedGroup.asStateFlow()

    fun clearLastCreatedGroup() {
        _lastCreatedGroup.value = null
    }

    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup: StateFlow<Group?> = _currentGroup.asStateFlow()

    private val _currentLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val currentLeaderboard: StateFlow<List<LeaderboardEntry>> = _currentLeaderboard.asStateFlow()

    private val _stadiumMatches = MutableStateFlow<List<MatchEntity>>(emptyList())
    val stadiumMatches: StateFlow<List<MatchEntity>> = _stadiumMatches.asStateFlow()

    // Selected items for navigation detail pages
    private val _selectedMatch = MutableStateFlow<MatchEntity?>(null)
    val selectedMatch: StateFlow<MatchEntity?> = _selectedMatch.asStateFlow()

    private val _selectedStadium = MutableStateFlow<StadiumEntity?>(null)
    val selectedStadium: StateFlow<StadiumEntity?> = _selectedStadium.asStateFlow()

    init {
        // Build/sync local cache upon login state
        if (sessionManager.isLoggedIn()) {
            loadUserProfileOnStartup()
        }
        viewModelScope.launch {
            // Seed static stadiums from API if Room is uninitiated
            repository.syncStadiums()
            repository.syncMatches()
        }
    }

    private fun loadUserProfileOnStartup() {
        _currentUserProfile.value = repository.getSessionUser()
        viewModelScope.launch {
            try {
                _currentUserProfile.value = repository.getProfile()
                repository.syncPredictions()
                loadMyGroups()
            } catch (e: Exception) {
                // Squelch network exceptions - displays offline seeded Profile data
            }
        }
    }

    // Action handlers
    fun loginUser(email: String, passcode: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val response = repository.login(email, passcode)
                _currentUserProfile.value = response.user
                _isLoggedIn.value = true
                _successMessage.value = "Sesión iniciada con éxito"

                // Fetch database info
                repository.syncStadiums()
                repository.syncMatches()
                repository.syncPredictions()
                loadMyGroups()
            } catch (e: Exception) {
                _errorMessage.value = "Error al iniciar sesión: ${e.localizedMessage ?: "Verifique sus credenciales"}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun registerUser(username: String, email: String, passcode: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val response = repository.register(username, email, passcode)
                _currentUserProfile.value = response.user
                _isLoggedIn.value = true
                _successMessage.value = "Cuenta registrada con éxito"

                // Seed initial databases
                repository.syncStadiums()
                repository.syncMatches()
                loadMyGroups()
            } catch (e: Exception) {
                _errorMessage.value = "Error al registrarse: ${e.localizedMessage ?: "Intente de nuevo"}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun logoutUser() {
        repository.logout()
        _currentUserProfile.value = null
        _isLoggedIn.value = false
        _groupsList.value = emptyList()
        _currentGroup.value = null
        _currentLeaderboard.value = emptyList()
        _stadiumMatches.value = emptyList()
        _selectedMatch.value = null
        _selectedStadium.value = null
    }

    fun loadMyGroups() {
        viewModelScope.launch {
            try {
                _groupsList.value = repository.getGroups()
            } catch (e: Exception) {
                Log.e("QuinielaVM", "Failed to load groups", e)
            }
        }
    }

    fun createNewGroup(name: String, desc: String, stadiumName: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val newGroup = repository.createGroup(name, desc, stadiumName)
                _lastCreatedGroup.value = newGroup
                _successMessage.value = "Grupo '${newGroup.name}' creado!"
                loadMyGroups()
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo crear el grupo: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun joinGroupWithCode(code: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val joined = repository.joinGroup(code)
                _successMessage.value = "¡Te has unido a '${joined.name}'!"
                loadMyGroups()
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo unir al grupo: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun loadGroupDetails(groupId: Int) {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                _currentGroup.value = repository.getGroupDetails(groupId)
                _currentLeaderboard.value = repository.getGroupLeaderboard(groupId)
            } catch (e: Exception) {
                _errorMessage.value = "Mostrando copia local del grupo. Error de red."
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun selectMatch(match: MatchEntity) {
        _selectedMatch.value = match
    }

    fun selectStadium(stadium: StadiumEntity) {
        _selectedStadium.value = stadium
        loadStadiumMatches(stadium.id)
    }

    fun loadStadiumMatches(stadiumId: Int) {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                _stadiumMatches.value = repository.getMatchesInStadium(stadiumId)
            } catch (e: Exception) {
                _errorMessage.value = "Error al descargar partidos del estadio"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun submitUserPrediction(matchId: Int, homeScore: Int, awayScore: Int) {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                repository.submitPrediction(matchId, homeScore, awayScore)
                _successMessage.value = "Pronóstico guardado exitosamente"
                
                // Refresh local statistics of predictions
                currentUserProfile.value?.let { profile ->
                    _currentUserProfile.value = profile.copy(
                        predictionCount = profile.predictionCount + 1
                    )
                }
                
                // Recalculate leaderboard if details active (Requirement 17)
                _currentGroup.value?.let { group ->
                    loadGroupDetails(group.id)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "No se pudo guardar el pronóstico"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Manual synchronization/refresh trigger (Requirement 17, 33)
    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                // Sincronizar partidos primero (Requirement 25)
                repository.syncMatches()
                repository.syncPredictions()
                
                // Sincronizar perfil para actualizar puntos y scores
                try {
                    _currentUserProfile.value = repository.getProfile()
                    loadMyGroups()
                    _currentGroup.value?.let { group ->
                        _currentLeaderboard.value = repository.getGroupLeaderboard(group.id)
                    }
                } catch (e: Exception) {
                    // Squelch sub-requests if offline
                }

                _successMessage.value = "Sincronización completada"
            } catch (e: Exception) {
                _errorMessage.value = "Fallo de sincronización: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearSyncLogs()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
}
