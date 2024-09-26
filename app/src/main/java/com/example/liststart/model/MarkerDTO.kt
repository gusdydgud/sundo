package com.example.liststart.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class MarkerDTO(
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val model: String,
    val degree: Long,
    val bno: Long
) : Parcelable
