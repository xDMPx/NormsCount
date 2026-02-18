package com.xdmpx.normscount.counter

import android.content.Context
import androidx.lifecycle.ViewModel
import com.xdmpx.normscount.database.CounterDatabase
import com.xdmpx.normscount.database.CounterEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CountersState(
    val countersViewModels: List<CounterViewModel> = listOf()
)

class CountersViewModel : ViewModel() {
    private val _countersState = MutableStateFlow(CountersState())
    val countersState: StateFlow<CountersState> = _countersState.asStateFlow()

    private val _currentCounterState = MutableStateFlow(CounterViewModel())
    val currentCounterState: StateFlow<CounterViewModel> = _currentCounterState.asStateFlow()

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
            _countersState.value = it.copy(countersViewModels = listOf())
        }
    }

    fun synchronizeCountersWithCurrentCounter() {
        _currentCounterState.value.let { currentCounter ->
            _countersState.value.let { counters ->
                _countersState.value =
                    counters.copy(countersViewModels = counters.countersViewModels.map {
                        if (currentCounter.getCounterId() == it.getCounterId()) {
                            it.setCounterName(currentCounter.counterState.value.name)
                            it.setCounterValue(currentCounter.counterState.value.count)
                        }
                        it
                    })
            }
        }
    }

    fun add(counterViewModel: CounterViewModel) {
        _countersState.value.let {
            _countersState.value =
                it.copy(countersViewModels = it.countersViewModels.plus(counterViewModel))
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
            CounterViewModel(
                counterEntity.id,
                counterEntity.value,
                counterEntity.name,
            )
        )

        this@CountersViewModel.setCurrentCounter(
            id = counterEntity.id, counterEntity.name, counterEntity.value
        )
    }

    suspend fun updateDatabase(context: Context) {
        _countersState.value.countersViewModels.forEach {
            it.updateDatabase(context)
        }
    }

    suspend fun deleteCounterById(context: Context, id: Int) {
        var index = _countersState.value.countersViewModels.indexOfFirst { it.getCounterId() == id }
        _countersState.value.let {
            _countersState.value =
                it.copy(countersViewModels = it.countersViewModels.mapNotNull { c -> if (c.getCounterId() != id) c else null })
        }
        val database = CounterDatabase.getInstance(context).counterDatabase
        database.deleteByID(id)

        index = if (index > 0) index - 1 else 0
        if (index == _countersState.value.countersViewModels.size) {
            this@CountersViewModel.addCounter(context, null, 0)
        } else {
            val counter = _countersState.value.countersViewModels[index]
            this@CountersViewModel.setCurrentCounter(
                counter.getCounterId(),
                counter.counterState.value.name,
                counter.counterState.value.count
            )
        }
    }

    suspend fun deleteAllCounters(context: Context) {
        this@CountersViewModel._countersState.value.let {
            this@CountersViewModel._countersState.value = it.copy(countersViewModels = listOf())
        }

        val database = CounterDatabase.getInstance(context).counterDatabase
        database.deleteAll()

        addCounter(context)
    }

    suspend fun loadCountersFromDatabase(context: Context) {
        val counters =
            CounterDatabase.getInstance(context).counterDatabase.getAll().map { counterEntity ->
                CounterViewModel(
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