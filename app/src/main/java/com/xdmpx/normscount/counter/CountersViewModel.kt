package com.xdmpx.normscount.counter

import android.content.Context
import androidx.lifecycle.ViewModel
import com.xdmpx.normscount.database.CounterDatabase
import com.xdmpx.normscount.database.CounterEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CountersState(
    val countStates: List<CounterState> = listOf()
)

class CountersViewModel : ViewModel() {
    private val _countersState = MutableStateFlow(CountersState())
    val countersState: StateFlow<CountersState> = _countersState.asStateFlow()

    private val _currentCounterState = MutableStateFlow(CounterViewModel())
    val currentCounterState: StateFlow<CounterViewModel> = _currentCounterState.asStateFlow()

    fun getCountersState() = countersState.value.countStates

    fun getCurrentCounterId() = _currentCounterState.value.counterState.value.id

    fun setCurrentCounter(id: Int, name: String, value: Long) {
        _currentCounterState.value.let {
            _currentCounterState.value.setCounterId(id)
            _currentCounterState.value.setCounterName(name)
            _currentCounterState.value.setCounterValue(value)
        }
    }

    fun incrementCurrentCounter() {
        _currentCounterState.value.incrementCounter()
    }

    fun decrementCurrentCounter() {
        _currentCounterState.value.decrementCounter()
    }

    fun resetCurrentCounter() {
        _currentCounterState.value.resetCounter()
    }

    fun setCurrentCounterName(name: String) {
        _currentCounterState.value.setCounterName(name)
    }

    fun setCurrentCounterValue(value: Long) {
        _currentCounterState.value.setCounterValue(value)
    }

    fun clear() {
        _countersState.value.let {
            _countersState.value = it.copy(countStates = listOf())
        }
    }

    fun synchronizeCountersWithCurrentCounter() {
        _currentCounterState.value.let { currentCounter ->
            _countersState.value.let { counters ->
                _countersState.value = counters.copy(countStates = counters.countStates.map {
                    if (currentCounter.getCounterId() == it.id) {
                        it.copy(
                            name = currentCounter.counterState.value.name,
                            count = currentCounter.counterState.value.count
                        )
                    } else {
                        it
                    }
                })
            }
        }
    }

    fun synchronizeCurrentCounterWithDatabase(context: Context) {
        val database = CounterDatabase.getInstance(context).counterDatabase
        _currentCounterState.value.let {
            val counterEntity = CounterEntity(
                it.counterState.value.id, it.counterState.value.name, it.counterState.value.count
            )
            CoroutineScope(Dispatchers.IO).launch {
                database.update(counterEntity)
            }
        }
    }

    fun add(counterState: CounterState) {
        _countersState.value.let {
            _countersState.value = it.copy(countStates = it.countStates.plus(counterState))
        }
    }

    suspend fun addCounter(context: Context, name: String? = null, value: Long = 0) {
        val database = CounterDatabase.getInstance(context).counterDatabase

        val lastID = database.getLastID() ?: 0
        val name = name ?: "Counter #${lastID + 1}"
        val counterBase = CounterEntity(name = name, value = value)
        database.insert(counterBase)
        val counterEntity = database.getLast()!!
        this@CountersViewModel.add(
            CounterState(
                counterEntity.id, counterEntity.value, counterEntity.name
            )
        )

        this@CountersViewModel.setCurrentCounter(
            id = counterEntity.id, counterEntity.name, counterEntity.value
        )
    }

    suspend fun updateDatabase(context: Context) {
        val database = CounterDatabase.getInstance(context).counterDatabase
        _countersState.value.countStates.forEach {
            val counterEntity = CounterEntity(it.id, it.name, it.count)
            database.update(counterEntity)
        }
    }

    suspend fun deleteCounterById(context: Context, id: Int) {
        var index = _countersState.value.countStates.indexOfFirst { it.id == id }
        _countersState.value.let {
            _countersState.value =
                it.copy(countStates = it.countStates.mapNotNull { c -> if (c.id != id) c else null })
        }
        val database = CounterDatabase.getInstance(context).counterDatabase
        database.deleteByID(id)

        index = if (index > 0) index - 1 else 0
        if (index == _countersState.value.countStates.size) {
            this@CountersViewModel.addCounter(context, null, 0)
        } else {
            val counter = _countersState.value.countStates[index]
            this@CountersViewModel.setCurrentCounter(
                counter.id, counter.name, counter.count
            )
        }
    }

    suspend fun deleteAllCounters(context: Context) {
        this@CountersViewModel._countersState.value.let {
            this@CountersViewModel._countersState.value = it.copy(countStates = listOf())
        }

        val database = CounterDatabase.getInstance(context).counterDatabase
        database.deleteAll()

        addCounter(context)
    }

    suspend fun loadCountersFromDatabase(context: Context) {
        val counters =
            CounterDatabase.getInstance(context).counterDatabase.getAll().map { counterEntity ->
                CounterState(
                    counterEntity.id,
                    counterEntity.value,
                    counterEntity.name,
                )
            }

        this@CountersViewModel.clear()
        counters.forEach {
            this@CountersViewModel.add(it)
        }
    }
}

abstract class CountersViewModelInstance {

    companion object {
        @Volatile
        private var INSTANCE: CountersViewModel? = null

        fun setInstance(instance: CountersViewModel) {
            INSTANCE = instance
        }

        fun getInstance(): CountersViewModel? {
            synchronized(this) {
                return INSTANCE
            }
        }
    }
}