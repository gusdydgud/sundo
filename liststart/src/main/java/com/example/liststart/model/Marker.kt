package com.example.liststart.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Marker(
    val mno: Long,
    val regdate: String,
    var update: String,
    var degree: Long,
    var latitude: Double = 91.0,
    var longitude: Double = 181.0,
    val bno: Long,
    var model: String
) : Parcelable
