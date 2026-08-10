package com.zenlemon.domain.repository

import com.zenlemon.domain.model.Category
import com.zenlemon.domain.model.ContentType
import com.zenlemon.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(providerId: Long): Flow<List<Category>>
    suspend fun setCategoryProtection(
        providerId: Long,
        categoryId: Long,
        type: ContentType,
        isProtected: Boolean
    ): Result<Unit>
}
