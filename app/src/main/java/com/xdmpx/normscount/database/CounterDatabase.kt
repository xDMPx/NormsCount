package com.xdmpx.normscount.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CounterEntity::class], version = 1)
abstract class CounterDatabase : RoomDatabase() {

    abstract val counterDatabase: CounterDao

    companion object {
        @Volatile
        private var INSTANCE: CounterDatabase? = null

        fun getInstance(context: Context): CounterDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, CounterDatabase::class.java, "CounterDatabase"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}