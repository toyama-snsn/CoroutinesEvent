package com.example.coroutinesevent

import android.os.Handler
import android.os.Looper

object Dispatcher {
    private val listeners = mutableListOf<Listener>()

    fun dispatch(action: Any) {
        Handler(Looper.getMainLooper()).post {
            listeners.forEach { it.on(action) }
        }
    }

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener) {
        listeners.remove(listener)
    }

    interface Listener {
        fun on(action: Any)
    }
}