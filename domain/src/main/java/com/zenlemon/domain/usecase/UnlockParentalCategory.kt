package com.zenlemon.domain.usecase

import com.zenlemon.domain.manager.ParentalControlManager
import com.zenlemon.domain.manager.ParentalPinVerifier
import com.zenlemon.domain.model.Result
import javax.inject.Inject

data class UnlockParentalCategoryCommand(
    val providerId: Long,
    val categoryId: Long,
    val pin: String
)

class UnlockParentalCategory @Inject constructor(
    private val parentalPinVerifier: ParentalPinVerifier,
    private val parentalControlManager: ParentalControlManager
) {
    suspend operator fun invoke(command: UnlockParentalCategoryCommand): Result<Unit> {
        if (command.providerId <= 0L || command.categoryId <= 0L) {
            return Result.error("Locked category context is unavailable.")
        }
        if (command.pin.isBlank()) {
            return Result.error("PIN is required.")
        }
        val verified = parentalPinVerifier.verifyParentalPin(command.pin)
        if (!verified) {
            return Result.error("Incorrect PIN")
        }
        parentalControlManager.unlockCategory(command.providerId, command.categoryId)
        return Result.success(Unit)
    }
}