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
        // Check if PIN is set up, if not, prompt to set it up
        if (!biometricHelper.isPinEnabled()) {
            biometricHelper.showPinSetupDialog(
                onSuccess = {
                    Toast.makeText(activity, "✅ PIN set up successfully!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                },
                onError = { error ->
                    Toast.makeText(activity, "❌ $error", Toast.LENGTH_SHORT).show()
                    onError(error)
                },
                onCancel = {
                    onError("PIN setup cancelled")
                }
            )
        } else {
            // PIN already set up, proceed
            onSuccess()
        }
    }

    private fun authenticateForNote(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Check if PIN is enabled for note protection
        if (biometricHelper.isPinEnabled()) {
            // Use PIN for note authentication
            biometricHelper.showPinDialog(
                title = "Enter PIN",
                message = "Enter your PIN to access this protected note",
                onSuccess = {
                    Toast.makeText(activity, "🔓 Note unlocked", Toast.LENGTH_SHORT).show()
                    onSuccess()
                },
                onError = { error ->
                    Toast.makeText(activity, "❌ $error", Toast.LENGTH_SHORT).show()
                    onError(error)
                },
                onCancel = {
                    onError("PIN entry cancelled")
                }
            )
        } else {
            // Fallback: No PIN set up, request PIN setup
            Toast.makeText(activity, "❌ PIN not set up. Please set up a PIN in Security Settings first.", Toast.LENGTH_LONG).show()
            onError("PIN not configured")
        }
    }

    companion object {
        fun isNoteProtected(note: Note): Boolean = note.requiresPin

        fun getProtectedNotePreview(note: Note): String {
            return if (note.requiresPin) {
                "••••••••••••••••••••••••••••••••\nTap to unlock and view content"
            } else {
                note.content.take(100) + if (note.content.length > 100) "..." else ""
            }
        }

        fun getProtectedNoteTitle(note: Note): String {
            return if (note.requiresPin) {
                "🔒 ${if (note.title.isNotEmpty()) "•".repeat(note.title.length) else "Protected Note"}"
            } else {
                note.title.ifEmpty { "Untitled" }
            }
        }
    }
}