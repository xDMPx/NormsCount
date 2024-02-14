package com.xdmpx.normscount.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CounterDao {
    @Query("SELECT * FROM Counter")
    suspend fun getAll(): List<CounterEntity>

    @Query("SELECT name FROM Counter")
    suspend fun getNames(): List<String>

    @Query("SELECT * FROM Counter ORDER BY id DESC LIMIT 1")
    suspend fun getLast(): CounterEntity?

    @Query("SELECT id FROM Counter ORDER BY id DESC LIMIT 1")
    suspend fun getLastID(): Int?

    @Insert
    suspend fun insert(counterEntity: CounterEntity)

    @Insert
    suspend fun insertAll(vararg counterEntity: CounterEntity)

    @Delete
    suspend fun delete(counterEntity: CounterEntity)

    @Update
    suspend fun update(counterEntity: CounterEntity)
}