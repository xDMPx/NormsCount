package com.xdmpx.normscount.counter

import android.content.Context
import androidx.lifecycle.ViewModel
import com.xdmpx.normscount.database.CounterDatabase
import com.xdmpx.normscount.database.CounterEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CounterState(
    val count: Long = 0, val name: String = ""
)

class Counter(
    private val id: Int,
    value: Long,
    name: String,
    val onDelete: (Counter) -> Unit,
) : ViewModel() {
    private val _counterState = MutableStateFlow(CounterState(value, name))
    val counterState: StateFlow<CounterState> = _counterState.asStateFlow()

    fun resetCounter() {
        _counterState.value.let {
            _counterState.value = it.copy(count = 0)
        }
    }

    fun incrementCounter() {
        _counterState.value.let {
            _counterState.value = it.copy(count = it.count + 1)
        }
    }

    fun decrementCounter() {
        _counterState.value.let {
            _counterState.value = it.copy(count = it.count - 1)
        }
    }

    fun setCounterValue(value: Long) {
        _counterState.value.let {
            _counterState.value = it.copy(count = value)
        }
    }

    fun setCounterName(name: String) {
        _counterState.value.let {
            _counterState.value = it.copy(name = name)
        }
    }

    fun getCounterId() = id

    private fun getCounterEntity() =
        CounterEntity(id, _counterState.value.name, _counterState.value.count)

    suspend fun updateDatabase(context: Context) {
        val database = CounterDatabase.getInstance(context).counterDatabase
        val counterEntity = getCounterEntity()
        database.update(counterEntity)
    }

}

abstract class CurrentCounter {

    companion object {
        @Volatile
        private var INSTANCE: Counter? = null

        fun setInstance(instance: Counter) {
            INSTANCE = instance
        }

        fun getInstance(): Counter? {
            synchronized(this) {
                return INSTANCE
            }
        }
    }
}
