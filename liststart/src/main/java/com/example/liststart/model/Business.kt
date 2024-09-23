package com.example.liststart.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "business")
data class Business(
    @PrimaryKey(autoGenerate = true)
    val bno: Long? = null,
    val regdate: String? = null,
    val update: String? = null,
    var title: String,
    val userName: String? = "이름없음",
    var isChecked: Boolean = false
) : Parcelable