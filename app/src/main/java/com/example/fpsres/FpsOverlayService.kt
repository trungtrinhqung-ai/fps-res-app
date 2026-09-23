package com.example.fpsres

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class FpsOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: TextView
    private var lastFrameTimeNanos = 0L
    private var frameCount = 0
    private var lastFpsUpdateTime = 0L

    private val choreographer = Choreographer.getInstance()
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameTimeNanos != 0L) {
                frameCount++
                val now = System.currentTimeMillis()
                if (now - lastFpsUpdateTime >= 500) {
                    val elapsedSeconds = (now - lastFpsUpdateTime) / 1000.0
                    val fps = (frameCount / elapsedSeconds).toInt()
                    overlayView.text = "$fps FPS"
                    frameCount = 0
                    lastFpsUpdateTime = now
                }
            } else {
                lastFpsUpdateTime = System.currentTimeMillis()
            }
            lastFrameTimeNanos = frameTimeNanos
            choreographer.postFrameCallback(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        overlayView = TextView(this).apply {
            text = "-- FPS"
            setTextColor(Color.GREEN)
            setBackgroundColor(Color.argb(150, 0, 0, 0))
            textSize = 14f
            setPadding(16, 8, 16, 8)
        }

        val layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 100

        windowManager.addView(overlayView, params)
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameCallback)
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
