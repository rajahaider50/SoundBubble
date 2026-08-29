package com.soundbubble.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundbubble.app.databinding.FloatingBubbleBinding
import com.soundbubble.app.databinding.FloatingPanelBinding
import java.io.File
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleBinding: FloatingBubbleBinding? = null
    private var panelBinding: FloatingPanelBinding? = null
    private var panelAdapter: AudioAdapter? = null

    private val channelId = "soundbubble_channel"

    // remembers bubble position so panel opens near it / bubble returns to same spot
    private var lastX = 0
    private var lastY = 300

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SoundBubble",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SoundBubble is running")
            .setContentText("Floating button is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private fun windowType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    // ---------- BUBBLE ----------

    private fun showBubble() {
        removePanel()
        removeBubble()

        val binding = FloatingBubbleBinding.inflate(LayoutInflater.from(this))
        bubbleBinding = binding

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = lastX
        params.y = lastY

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        binding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    runCatching { windowManager.updateViewLayout(binding.root, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    lastX = params.x
                    lastY = params.y
                    if (!isDragging) showPanel()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(binding.root, params)
    }

    private fun removeBubble() {
        bubbleBinding?.let { runCatching { windowManager.removeView(it.root) } }
        bubbleBinding = null
    }

    // ---------- PANEL ----------

    private fun showPanel() {
        removeBubble()
        removePanel()

        val binding = FloatingPanelBinding.inflate(LayoutInflater.from(this))
        panelBinding = binding

        val audioDir = File(getExternalFilesDir(null), "audio")
        val files = audioDir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        val adapter = AudioAdapter(files) { file ->
            file.delete()
            showPanel() // refresh after delete
        }
        panelAdapter = adapter

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnClose.setOnClickListener { showBubble() }
        binding.btnStop.setOnClickListener { stopSelf() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = lastX
        params.y = lastY

        windowManager.addView(binding.root, params)
    }

    private fun removePanel() {
        panelAdapter?.release()
        panelAdapter = null
        panelBinding?.let { runCatching { windowManager.removeView(it.root) } }
        panelBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBubble()
        removePanel()
    }
}
