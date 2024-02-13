package com.xdmpx.normscount.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Counter")
data class CounterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") var name: String = "Counter #",
    @ColumnInfo(name = "value") var value: Long,
)
