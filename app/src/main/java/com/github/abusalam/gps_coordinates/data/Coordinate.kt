package com.github.abusalam.gps_coordinates.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "coordinates")
data class Coordinate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CoordinateDao {
    @Query("SELECT * FROM coordinates ORDER BY isPinned DESC, timestamp DESC")
    fun getAll(): Flow<List<Coordinate>>

    @Insert
    suspend fun insert(coordinate: Coordinate)

    @Update
    suspend fun update(coordinate: Coordinate)

    @Query("DELETE FROM coordinates WHERE id = :id")
    suspend fun delete(id: Int)
}

@Database(entities = [Coordinate::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coordinateDao(): CoordinateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coordinate_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
