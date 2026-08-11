package com.okeho.mapping.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Confirms sign-out, warning about local data loss.
 *
 * Shared by the drawer and the settings screen so the unsynced-record warning
 * cannot drift between them: sign-out wipes the device, and [pendingCount]
 * records would go with it.
 */
@Composable
fun SignOutConfirmDialog(
    pendingCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign out?") },
        text = {
            Text(
                if (pendingCount > 0) {
                    "You have $pendingCount record(s) that have not synced yet. " +
                        "Signing out erases the records on this device, so those " +
                        "unsynced ones will be lost. Sync first if you want to keep them."
                } else {
                    "Records on this device will be erased. Everything already synced " +
                        "stays on the server and comes back when you sign in again."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Sign out") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
