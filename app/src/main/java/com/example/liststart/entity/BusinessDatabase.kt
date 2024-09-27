package com.example.liststart.entity

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BusinessEntity::class], version = 2, exportSchema = false) // 버전 번호를 1에서 2로 증가
abstract class BusinessDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao

    companion object {
        @Volatile
        private var INSTANCE: BusinessDatabase? = null

        fun getInstance(context: Context): BusinessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusinessDatabase::class.java,
                    "business_database"
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
