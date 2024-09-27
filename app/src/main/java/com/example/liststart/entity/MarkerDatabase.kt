package com.example.liststart.entity

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MarkerEntity::class], version = 1, exportSchema = false)
abstract class MarkerDatabase : RoomDatabase() {

    abstract fun markerDao(): MarkerDao

    companion object {
        @Volatile
        private var INSTANCE: MarkerDatabase? = null

        fun getInstance(context: Context): MarkerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarkerDatabase::class.java,
                    "marker_database"
                )
                    .fallbackToDestructiveMigration() // 기존 데이터를 삭제하고 새 스키마로 교체
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
