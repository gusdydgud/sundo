package com.example.liststart.datasource

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.liststart.datasource.BusinessDao
import com.example.liststart.datasource.MarkerDao
import com.example.liststart.model.Business
import com.example.liststart.model.Marker

@Database(entities = [Business::class, Marker::class], version = 1, exportSchema = false)
abstract class BusinessDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao  // BusinessDao 제공
    abstract fun markerDao(): MarkerDao  // MarkerDao 제공

    companion object {
        @Volatile
        private var INSTANCE: BusinessDatabase? = null

        // 싱글톤 패턴으로 데이터베이스 객체 생성
        fun getDatabase(context: Context): BusinessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusinessDatabase::class.java,
                    "business_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}