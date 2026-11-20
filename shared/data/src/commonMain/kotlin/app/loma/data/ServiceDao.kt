// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Insert
    suspend fun insert(service: ServiceEntity): Long

    @Update
    suspend fun update(service: ServiceEntity)

    @Query("SELECT * FROM services WHERE active = 1 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<ServiceEntity>>
}
