package com.paperyt

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chaquo.python.Python

class DownloadService : Service() {
    private val CHANNEL_ID = "download_channel"
    private val NOTIF_ID = 1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: ""
        val args = intent?.getStringArrayExtra("args") ?: emptyArray()
        val dir = intent?.getStringExtra("dir") ?: ""
        val ffmpegPath = intent?.getStringExtra("ffmpegPath") ?: ""

        createNotificationChannel()
        val notification = buildNotification(0)
        startForeground(NOTIF_ID, notification)

        Thread {
            val py = Python.getInstance()
            val module = py.getModule("downloader")
    
            val callback = object {
                @androidx.annotation.Keep
                fun onProgress(p: Double) {
                    updateNotification(p.toInt())
                }
            }

            val result = module.callAttr("run_ytdlp", args, dir, ffmpegPath, callback).toString()
    
            if (result == "Download Completed!") {
                showCompleteNotification(dir) // 完了通知を表示
            } else {
                showErrorNotification(result) 
            }

            stopForeground(STOP_FOREGROUND_DETACH) // 通知を残すために DETACH にする
            stopSelf()
        }.start()

        return START_NOT_STICKY
    }

    private fun buildNotification(progress: Int) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("PaperYT: ダウンロード中")
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setProgress(100, progress, false)
        .setOngoing(true)
        .build()

    private fun updateNotification(progress: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(progress))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Download Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showCompleteNotification(dirPath: String) {
        val manager = getSystemService(NotificationManager::class.java)
    
        val completeNotif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ダウンロード完了")
            .setContentText("ファイルは $dirPath に保存されました")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true) // タップしたら消える
            .setOngoing(false)   // スワイプで消せる
            .build()

        manager.notify(NOTIF_ID, completeNotif)
    }

    private fun showErrorNotification(errorMsg: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val errorNotif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ダウンロード失敗")
            .setContentText(errorMsg) // Pythonから返ってきたエラー内容を表示
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()
        manager.notify(2, errorNotif) // 別のIDで出すと完了通知と混ざりません
    }

    override fun onBind(intent: Intent?): IBinder? = null
}