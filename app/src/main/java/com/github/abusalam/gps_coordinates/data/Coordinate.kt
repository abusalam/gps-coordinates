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
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val tag: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CoordinateDao {
    @Query("SELECT * FROM coordinates ORDER BY isPinned DESC, timestamp DESC")
    fun getAll(): Flow<List<Coordinate>>

    @Insert
    suspend fun insert(coordinate: Coordinate)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(coordinates: List<Coordinate>)

    @Update
    suspend fun update(coordinate: Coordinate)

    @Query("DELETE FROM coordinates WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM coordinates ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAllList(): List<Coordinate>

    @Query("SELECT COUNT(*) FROM coordinates")
    suspend fun getCount(): Int

    @Query("DELETE FROM coordinates")
    suspend fun deleteAll()
}

@Database(entities = [Coordinate::class], version = 3, exportSchema = false)
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
