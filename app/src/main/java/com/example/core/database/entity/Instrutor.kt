package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instrutor")
data class Instrutor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val foto: String,
    val cnh: String,
    val validadeCnh: String
)
