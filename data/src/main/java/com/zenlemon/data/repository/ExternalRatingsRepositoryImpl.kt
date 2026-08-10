package com.zenlemon.data.repository

import com.zenlemon.domain.model.ExternalRatings
import com.zenlemon.domain.model.ExternalRatingsLookup
import com.zenlemon.domain.model.Result
import com.zenlemon.domain.repository.ExternalRatingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalRatingsRepositoryImpl @Inject constructor() : ExternalRatingsRepository {

    override suspend fun getRatings(lookup: ExternalRatingsLookup): Result<ExternalRatings> {
        return Result.success(ExternalRatings.unavailable())
    }
}