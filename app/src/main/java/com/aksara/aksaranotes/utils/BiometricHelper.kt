package com.aksara.aksaranotes.utils

import androidx.fragment.app.FragmentActivity

class BiometricHelper(private val activity: FragmentActivity) {

    fun authenticateWithBiometric(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onUserCancel: () -> Unit
    ) {
        // For now, just call success (we'll implement biometric later)
        onSuccess()
    }
}