package com.naigebao.storage.repository

import com.naigebao.storage.database.dao.UserDao
import com.naigebao.storage.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUser(userId: String): Flow<UserEntity?>

    suspend fun upsert(user: UserEntity)
}

class RoomUserRepository(
    private val userDao: UserDao
) : UserRepository {
    override fun observeUser(userId: String): Flow<UserEntity?> = userDao.observeUser(userId)

    override suspend fun upsert(user: UserEntity) {
        userDao.upsert(user)
    }
}
