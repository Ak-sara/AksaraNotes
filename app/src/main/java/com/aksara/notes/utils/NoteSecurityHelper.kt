package com.aksara.notes.utils

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.aksara.notes.data.database.entities.Note

class NoteSecurityHelper(private val activity: FragmentActivity) {

    private val biometricHelper = BiometricHelper(activity)

    fun checkNoteAccess(
        note: Note,
        onAccessGranted: () -> Unit,
        onAccessDenied: (String) -> Unit = { error ->
            Toast.makeText(activity, "Access denied: $error", Toast.LENGTH_SHORT).show()
        }
    ) {
        if (note.requiresPin) {
            authenticateForNote(onAccessGranted, onAccessDenied)
        } else {
            onAccessGranted()
        }
    }

    fun authenticateForPinToggle(
        onSuccess: () -> Unit,
        onError: (String) -> Unit = { error ->
            Toast.makeText(activity, "Authentication failed: $error", Toast.LENGTH_SHORT).show()
        }
    ) {
        authenticateForNote(onSuccess, onError)
    }

    private fun authenticateForNote(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        biometricHelper.authenticateUser(
            onSuccess = {
                Toast.makeText(activity, "🔓 Access granted", Toast.LENGTH_SHORT).show()
                onSuccess()
            },
            onError = onError,
            onPasswordFallback = {
                biometricHelper.showPasswordDialog(
                    onSuccess = {
                        Toast.makeText(activity, "🔓 Access granted", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    },
                    onError = onError,
                    onCancel = {
                        onError("Authentication cancelled")
                    }
                )
            }
        )
    }

    companion object {
        fun isNoteProtected(note: Note): Boolean = note.requiresPin

        fun getProtectedNotePreview(note: Note): String {
            return if (note.requiresPin) {
                "🔒 This note is protected. Tap to unlock."
            } else {
                note.content.take(100) + if (note.content.length > 100) "..." else ""
            }
        }

        fun getProtectedNoteTitle(note: Note): String {
            return if (note.requiresPin) {
                "🔒 ${note.title.ifEmpty { "Untitled" }}"
            } else {
                note.title.ifEmpty { "Untitled" }
            }
        }
    }
}