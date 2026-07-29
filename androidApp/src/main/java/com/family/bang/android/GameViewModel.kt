package com.family.bang.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Screen { HOME, LOBBY }

data class GameUiState(
    val screen: Screen = Screen.HOME,
    val gameCode: String = "",
    val playerName: String = "",
    val players: List<String> = emptyList(),
    val isHost: Boolean = false,
    val loading: Boolean = false,
    val role: String? = null,
    val error: String? = null,
)

class GameViewModel(private val api: GameApi = GameApi(BuildConfig.DEFAULT_API_URL)) : ViewModel() {
    var state = androidx.compose.runtime.mutableStateOf(GameUiState())
        private set

    private var hostToken: String? = null
    private var playerToken: String? = null
    private var polling: Job? = null

    fun createAndJoin(name: String) = launchRequest {
        requireName(name)
        val game = api.createGame()
        val player = api.joinGame(game.gameCode, name.trim())
        hostToken = game.hostToken
        enterLobby(game.gameCode, player, isHost = true)
    }

    fun join(code: String, name: String) = launchRequest {
        requireName(name)
        if (code.trim().length != 6) throw ApiException("Enter the 6-character game code")
        val cleanCode = code.trim().uppercase()
        val player = api.joinGame(cleanCode, name.trim())
        enterLobby(cleanCode, player, isHost = false)
    }

    fun refresh() = viewModelScope.launch { refreshLobby(showLoading = true) }

    fun deal() = launchRequest {
        api.deal(state.value.gameCode, hostToken ?: throw ApiException("Only the host can deal roles"))
        refreshLobby()
    }

    fun revealRole() = launchRequest {
        state.value = state.value.copy(role = api.role(state.value.gameCode, playerToken.orEmpty()))
    }

    fun hideRole() { state.value = state.value.copy(role = null) }

    fun dismissError() { state.value = state.value.copy(error = null) }

    fun leave() {
        polling?.cancel()
        hostToken = null
        playerToken = null
        state.value = GameUiState()
    }

    private suspend fun enterLobby(code: String, player: JoinedPlayer, isHost: Boolean) {
        playerToken = player.playerToken
        state.value = GameUiState(
            screen = Screen.LOBBY,
            gameCode = code,
            playerName = player.playerName,
            isHost = isHost,
        )
        refreshLobby()
        polling?.cancel()
        polling = viewModelScope.launch {
            while (isActive) {
                delay(3_000)
                refreshLobby()
            }
        }
    }

    private suspend fun refreshLobby(showLoading: Boolean = false) {
        val current = state.value
        if (current.screen != Screen.LOBBY) return
        if (showLoading) state.value = current.copy(loading = true)
        runCatching { api.lobby(current.gameCode, playerToken.orEmpty()) }
            .onSuccess { state.value = state.value.copy(players = it, loading = false) }
            .onFailure { if (showLoading) state.value = state.value.copy(loading = false, error = message(it)) }
    }

    private fun launchRequest(block: suspend () -> Unit) {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { state.value = state.value.copy(error = message(it)) }
            state.value = state.value.copy(loading = false)
        }
    }

    private fun requireName(name: String) {
        if (name.isBlank()) throw ApiException("Enter your name")
        if (name.trim().length > 40) throw ApiException("Names can contain at most 40 characters")
    }

    private fun message(error: Throwable) = error.message ?: "Something went wrong. Try again."
}
