package com.fdossena.speedtest.data

import kotlinx.coroutines.flow.*

class AndroidServiceStatusRepository(
    private val delegate: WebServiceStatusRepository
) : ServiceStatusRepository by delegate
