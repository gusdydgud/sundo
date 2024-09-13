package com.example.liststart.model

import android.os.Parcelable
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize

@Parcelize
data class Business(
    @Expose(serialize = false) // 서버로 전송할 때 제외
    var bno: Long? = null,
    @Expose(serialize = false)
    var regdate: String? = null,
    @Expose(serialize = false)
    var update: String? = null,
    var title: String,
    var userName: String = "이름없음",
    var isChecked: Boolean = false
) : Parcelable
