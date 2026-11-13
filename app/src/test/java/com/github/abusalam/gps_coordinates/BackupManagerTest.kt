package com.github.abusalam.gps_coordinates

import com.github.abusalam.gps_coordinates.data.Coordinate
import com.github.abusalam.gps_coordinates.data.CoordinateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeCoordinateDao : CoordinateDao {
    val list = mutableListOf<Coordinate>()

    override fun getAll(): Flow<List<Coordinate>> = flowOf(list)

    override suspend fun insert(coordinate: Coordinate) {
        list.add(coordinate)
    }

    override suspend fun insertAll(coordinates: List<Coordinate>) {
        list.addAll(coordinates)
    }

    override suspend fun update(coordinate: Coordinate) {
        val index = list.indexOfFirst { it.id == coordinate.id }
        if (index != -1) list[index] = coordinate
    }

    override suspend fun delete(id: Int) {
        list.removeAll { it.id == id }
    }

    override suspend fun getAllList(): List<Coordinate> = list.toList()

    override suspend fun getCount(): Int = list.size

    override suspend fun deleteAll() {
        list.clear()
    }
}

class BackupManagerTest {

    @Test
    fun testFakeDaoOperations() = runBlocking {
        val dao = FakeCoordinateDao()
        assertEquals(0, dao.getCount())

        val coord1 = Coordinate(
            id = 1,
            label = "Test Location",
            latitude = 37.7749,
            longitude = -122.4194,
            isPinned = true,
            altitude = 12.0,
            accuracy = 4.0f,
            tag = "outdoor"
        )
        dao.insert(coord1)
        assertEquals(1, dao.getCount())
        assertEquals("Test Location", dao.getAllList().first().label)

        val coord2 = Coordinate(
            id = 2,
            label = "Second Location",
            latitude = 40.7128,
            longitude = -74.0060,
            isPinned = false
        )
        dao.insertAll(listOf(coord2))
        assertEquals(2, dao.getCount())

        dao.delete(1)
        assertEquals(1, dao.getCount())
        assertEquals(2, dao.getAllList().first().id)

        dao.deleteAll()
        assertEquals(0, dao.getCount())
    }
}
