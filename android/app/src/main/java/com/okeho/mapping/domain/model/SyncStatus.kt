package com.okeho.mapping.domain.model

enum class SyncStatus(val displayName: String) {
    PENDING("Pending"),
    SYNCED("Synced"),
    FAILED("Failed")
}
