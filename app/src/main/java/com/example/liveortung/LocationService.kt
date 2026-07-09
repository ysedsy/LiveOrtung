package com.example.liveortung

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

/**
 * Foreground-Service: sendet alle 5 s die eigene Position nach
 * /rooms/{roomCode}/{myId} in die Firebase Realtime Database.
 */
class LocationService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var callback: LocationCallback

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val roomCode = intent?.getStringExtra("room") ?: return START_NOT_STICKY
        val myId = intent.getStringExtra("myId") ?: return START_NOT_STICKY

        startForeground(1, buildNotification())

        val ref = FirebaseDatabase.getInstance()
            .getReference("rooms").child(roomCode).child(myId)
        // Eintrag löschen, wenn App/Verbindung wegfällt:
        ref.onDisconnect().removeValue()

        fused = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateDistanceMeters(2f).build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    ref.setValue(
                        mapOf(
                            "lat" to loc.latitude,
                            "lng" to loc.longitude,
                            "acc" to loc.accuracy,
                            "ts" to System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        try {
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) { stopSelf() }

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "loc_channel"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "Standortfreigabe",
                    NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Standort wird geteilt")
            .setContentText("Deine Position ist für deinen Partner sichtbar.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        if (::fused.isInitialized) fused.removeLocationUpdates(callback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
