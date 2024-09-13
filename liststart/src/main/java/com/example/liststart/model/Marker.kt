package com.example.liststart.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Marker(
    val mno: Long,
    val regdate: String,
    var update: String,
    var degree: Long,
    var latitude: Double,
    var longitude: Double,
    val bno: Long,
    var model: String,
    var title: String?
) : Parcelable
