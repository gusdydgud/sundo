package com.example.liststart.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0L,
    
)