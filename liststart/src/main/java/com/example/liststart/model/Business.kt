package com.example.liststart.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Parcelize
@Entity(tableName = "business")
data class Business(
    @PrimaryKey(autoGenerate = true)
    val bno: Long? = null,
    val regdate: String? = getCurrentTime(),
    val update: String? = getCurrentTime(),
    var title: String,
    val userName: String? = "이름없음",
    var isChecked: Boolean = false
) : Parcelable
{
    companion object {
        fun getCurrentTime(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}