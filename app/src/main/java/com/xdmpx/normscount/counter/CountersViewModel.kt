package com.xdmpx.normscount.counter

import android.content.Context
import androidx.lifecycle.ViewModel
import com.xdmpx.normscount.database.CounterDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CountersState(
    val countersViewModels: List<CounterViewModel?> = listOf()
)

class CountersViewModel : ViewModel() {
    private val _countersState = MutableStateFlow(CountersState())
    val countersState: StateFlow<CountersState> = _countersState.asStateFlow()

    fun clear() {
        _countersState.value.let {
            _countersState.value = it.copy(countersViewModels = listOf())
        }
    }

    fun add(counterViewModel: CounterViewModel) {
        _countersState.value.let {
            _countersState.value =
                it.copy(countersViewModels = it.countersViewModels.plus(counterViewModel))
        }
    }

    fun size() = _countersState.value.countersViewModels.size

    fun last() = _countersState.value.countersViewModels.last()

    fun indexOfFirst(predicate: (CounterViewModel?) -> Boolean) =
        _countersState.value.countersViewModels.indexOfFirst { predicate(it) }

    operator fun get(index: Int) = _countersState.value.countersViewModels[index]
    operator fun set(index: Int, value: CounterViewModel?) {
        _countersState.value.let {
            _countersState.value =
                it.copy(countersViewModels = it.countersViewModels.mapIndexed { i, v -> if (i == index) value else v })
        }
    }

    suspend fun forEach(action: suspend (CounterViewModel?) -> Unit) {
        _countersState.value.countersViewModels.forEach { action(it) }
    }

    suspend fun deleteCounterById(context: Context, id: Int) {
        _countersState.value.let {
            _countersState.value =
                it.copy(countersViewModels = it.countersViewModels.map { c -> if (c?.id != id) c else null })
        }

        val database = CounterDatabase.getInstance(context).counterDatabase
        database.deleteByID(id)
    }

    fun filterNotNull() = _countersState.value.countersViewModels.filterNotNull()

}