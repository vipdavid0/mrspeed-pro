package com.fdossena.speedtest.data

import kotlinx.coroutines.flow.*

class DesktopSyncManager(
    private val delegate: WebSyncManager
) : SyncProvider by delegate
