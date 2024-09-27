package com.example.liststart.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.example.liststart.model.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "marker")
data class MarkerEntity(
    @PrimaryKey(autoGenerate = true) // Primary key 자동 생성
    var mno: Long,

    @ColumnInfo(name = "regdate") // 등록일자
    val regdate: String? = getCurrentTime(), // 처음 삽입 시 자동으로 설정

    @ColumnInfo(name = "update") // 업데이트 일자
    var update: String? = getCurrentTime(), // 삽입 시에도 초기값을 설정하되, 업데이트 시에 갱신

    var degree: Long,
    var latitude: Double,
    var longitude: Double,
    val bno: Long,
    var model: String,
    var title: String?
) {
    companion object {
        fun getCurrentTime(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}

// BusinessEntity -> Business 변환 함수
fun MarkerEntity.toMarker(): Marker {
    return Marker(
        mno = this.mno,
        regdate = this.regdate,
        update = this.update,
        degree = this.degree,
        latitude = this.latitude,
        longitude = this.longitude,
        bno = this.bno,
        model = this.model,
        title = this.title
    )
}

// Business -> BusinessEntity 변환 함수
fun Marker.toMarkerEntity(): MarkerEntity {
    return MarkerEntity(
        mno = this.mno,
        regdate = this.regdate ?: BusinessEntity.getCurrentTime(), // 기존 값이 없으면 현재 시간으로 설정
        update = BusinessEntity.getCurrentTime(), // 항상 현재 시간으로 업데이트
        degree = this.degree,
        latitude = this.latitude,
        longitude = this.longitude,
        bno = this.bno,
        model = this.model,
        title = this.title
    )
}
