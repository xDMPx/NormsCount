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

    private val _currentCounterState = MutableStateFlow(CounterState(0, 0, "Counter #"))
    val currentCounterState: StateFlow<CounterState> = _currentCounterState.asStateFlow()

    fun getCountersState() = countersState.value.countStates

    fun getCurrentCounterId() = currentCounterState.value.id

    fun setCurrentCounter(id: Int, name: String, value: Long) {
        _currentCounterState.value.let {
            _currentCounterState.value = it.copy(
                id = id, count = value, name = name
            )
        }
    }

    fun incrementCurrentCounter() {
        _currentCounterState.value.let {
            _currentCounterState.value = it.copy(count = it.count + 1)
        }
    }

    fun decrementCurrentCounter() {
        _currentCounterState.value.let {
            _currentCounterState.value = it.copy(count = it.count - 1)
        }
    }

    fun resetCurrentCounter() {
        _currentCounterState.value.let {
            _currentCounterState.value = it.copy(count = 0)
        }
    }

    fun setCurrentCounterName(name: String) {
        _currentCounterState.value.let {
            _currentCounterState.value = it.copy(name = name)
        }
    }

    fun setCurrentCounterValue(value: Long) {
        _currentCounterState.value.let {
            _currentCounterState.value = it.copy(count = value)
        }
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
                    if (currentCounter.id == it.id) {
                        it.copy(
                            name = currentCounter.name, count = currentCounter.count
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
            val counterEntity = CounterEntity(it.id, it.name, it.count)
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