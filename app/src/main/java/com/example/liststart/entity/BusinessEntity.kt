package com.example.liststart.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.example.liststart.model.Business
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "business")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true) // Primary key 자동 생성
    var bno: Long? = null,

    @ColumnInfo(name = "regdate") // 등록일자
    val regdate: String? = getCurrentTime(), // 처음 삽입 시 자동으로 설정

    @ColumnInfo(name = "update") // 업데이트 일자
    var update: String? = getCurrentTime(), // 삽입 시에도 초기값을 설정하되, 업데이트 시에 갱신

    var title: String,

    // 기본값으로 "이름없음"을 설정
    var userName: String? = "이름없음",

    // 체크 상태는 DB에 저장되지 않음
    @Ignore
    var isChecked: Boolean = false
) {
    companion object {
        fun getCurrentTime(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    // 기본 생성자를 추가하여 Room에서 매칭되는 생성자를 찾을 수 있도록 한다
    constructor(
        bno: Long?,
        regdate: String?,
        update: String?,
        title: String,
        userName: String?
    ) : this(bno, regdate, update, title, userName, false)
}

// BusinessEntity -> Business 변환 함수
fun BusinessEntity.toBusiness(): Business {
    return Business(
        bno = this.bno,
        regdate = this.regdate,
        update = this.update,
        title = this.title,
        userName = this.userName,
        isChecked = this.isChecked
    )
}

// Business -> BusinessEntity 변환 함수
fun Business.toBusinessEntity(): BusinessEntity {
    return BusinessEntity(
        bno = this.bno,
        regdate = this.regdate ?: BusinessEntity.getCurrentTime(), // 기존 값이 없으면 현재 시간으로 설정
        update = BusinessEntity.getCurrentTime(), // 항상 현재 시간으로 업데이트
        title = this.title,
        userName = this.userName
    )
}
