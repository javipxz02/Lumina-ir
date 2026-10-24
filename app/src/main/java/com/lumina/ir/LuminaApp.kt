package com.lumina.ir

import android.app.Application
import com.lumina.ir.core.ir.IrTransmitterManager

class LuminaApp : Application() {
    lateinit var irManager: IrTransmitterManager
        private set

    override fun onCreate() {
        super.onCreate()
        irManager = IrTransmitterManager.getInstance(this)
    }
}
