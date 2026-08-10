package com.zenlemon.domain.repository

import com.zenlemon.domain.model.ExternalRatings
import com.zenlemon.domain.model.ExternalRatingsLookup
import com.zenlemon.domain.model.Result

interface ExternalRatingsRepository {
    suspend fun getRatings(lookup: ExternalRatingsLookup): Result<ExternalRatings>
}