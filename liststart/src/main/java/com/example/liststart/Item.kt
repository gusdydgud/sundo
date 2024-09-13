package com.example.liststart

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Item(
    val bno: Long = 0L,
    val title: String,
    val date: String,
    val profileImage: Int = R.drawable.profile,
    val lat: Double = 91.0,
    val long: Double = 181.0,
    var isChecked: Boolean = false
) : Parcelable
