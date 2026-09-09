package com.example.domain.focus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first as kotlinxFirst

/** Keeps FocusEngine imports minimal while using a one-shot value from repository flows. */
suspend fun <T> Flow<T>.first(): T = this.kotlinxFirst()
