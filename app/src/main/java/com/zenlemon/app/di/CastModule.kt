package com.zenlemon.app.di

import com.zenlemon.app.cast.CastPlaybackCoordinator
import com.zenlemon.app.cast.DefaultCastPlaybackCoordinator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CastModule {
    @Binds
    @Singleton
    abstract fun bindCastPlaybackCoordinator(
        impl: DefaultCastPlaybackCoordinator
    ): CastPlaybackCoordinator
}
