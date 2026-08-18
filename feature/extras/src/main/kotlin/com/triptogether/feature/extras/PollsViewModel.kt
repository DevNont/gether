package com.triptogether.feature.extras

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Poll
import com.triptogether.core.domain.model.PollOption
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.PollRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.feature.extras.navigation.PollsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

const val POLL_MIN_OPTIONS = 2
const val POLL_MAX_OPTIONS = 8

data class PollsUiState(
    val isLoading: Boolean = true,
    val polls: List<Poll> = emptyList(),
    val members: List<Member> = emptyList(),
    val myMemberId: String? = null,
) {
    fun memberName(memberId: String): String = members.firstOrNull { it.id == memberId }?.displayName ?: ""

    fun canClose(poll: Poll): Boolean = !poll.closed && poll.createdBy == myMemberId
}

sealed interface PollsEvent {
    data class Error(
        @StringRes val messageResId: Int,
    ) : PollsEvent
}

@HiltViewModel
class PollsViewModel
    @Inject
    constructor(
        private val pollRepository: PollRepository,
        tripRepository: TripRepository,
        authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<PollsRoute>()

        private val _events = Channel<PollsEvent>(Channel.BUFFERED)
        val events: Flow<PollsEvent> = _events.receiveAsFlow()

        val uiState: StateFlow<PollsUiState> =
            combine(
                pollRepository.observePolls(route.tripId),
                tripRepository.observeMembers(route.tripId),
                authRepository.observeAuthState(),
            ) { polls, members, user ->
                PollsUiState(
                    isLoading = false,
                    polls = polls,
                    members = members,
                    myMemberId = members.firstOrNull { it.userId == user?.id }?.id,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = PollsUiState(),
            )

        fun createPoll(
            question: String,
            optionLabels: List<String>,
            multiChoice: Boolean,
        ) {
            val labels = optionLabels.map { it.trim() }.filter { it.isNotEmpty() }
            if (question.isBlank() || labels.size < POLL_MIN_OPTIONS || labels.size > POLL_MAX_OPTIONS) return
            val me = uiState.value.myMemberId ?: return
            viewModelScope.launch {
                pollRepository.create(
                    route.tripId,
                    Poll(
                        id = "",
                        question = question.trim(),
                        options = labels.map { PollOption(id = UUID.randomUUID().toString(), label = it) },
                        multiChoice = multiChoice,
                        createdBy = me,
                    ),
                ).onFailure { _events.send(PollsEvent.Error(R.string.polls_error)) }
            }
        }

        fun toggleVote(
            poll: Poll,
            option: PollOption,
        ) {
            val me = uiState.value.myMemberId ?: return
            if (poll.closed) return
            val selected = me !in option.voterMemberIds
            viewModelScope.launch {
                pollRepository.setVote(route.tripId, poll.id, option.id, me, selected)
                    .onFailure { _events.send(PollsEvent.Error(R.string.polls_error)) }
            }
        }

        fun closePoll(poll: Poll) {
            if (!uiState.value.canClose(poll)) return
            viewModelScope.launch {
                pollRepository.close(route.tripId, poll.id)
                    .onFailure { _events.send(PollsEvent.Error(R.string.polls_error)) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
