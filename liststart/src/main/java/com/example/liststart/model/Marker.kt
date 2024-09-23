package com.example.liststart.model
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "marker")
data class Marker(
    @PrimaryKey(autoGenerate = true)
    val mno: Long = 0,  // autoGenerate로 변경
    val regdate: String,
    var update: String,
    var degree: Long,
    var latitude: Double,
    var longitude: Double,
    val bno: Long,  // Business와 연관된 키
    var model: String,
    var title: String?
) : Parcelable