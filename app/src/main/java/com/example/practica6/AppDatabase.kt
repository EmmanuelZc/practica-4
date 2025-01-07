package com.example.practica6

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.practica6.data.api.FavoriteDao
import com.example.practica6.data.models.FavoriteLocal
import com.example.practica6.data.models.RolLocal
import com.example.practica6.data.models.UserDao
import com.example.practica6.data.models.UserLocal
import com.example.practica6.data.models.UserRoleCrossRef
@Database(
    entities = [UserLocal::class, RolLocal::class, UserRoleCrossRef::class, FavoriteLocal::class], // Agrega FavoriteLocal
    version = 4, // Incrementa la versión
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun favoriteDao(): FavoriteDao // Agrega el DAO de favoritos

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createDatabase(context).also { INSTANCE = it }
            }
        }

        private fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            )
                .addMigrations(MIGRATION_3_4) // Nueva migración
                .build()
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crea la tabla de favoritos
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS favorites (
                        bookId TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        author TEXT,
                        publishYear INTEGER,
                        isSynced INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )
            }
        }
    }
}
