package com.zenlemon.domain.manager

interface ParentalPinVerifier {
    suspend fun verifyParentalPin(pin: String): Boolean
}