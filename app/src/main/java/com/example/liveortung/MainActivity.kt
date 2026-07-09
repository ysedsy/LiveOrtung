package com.example.liveortung

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.liveortung.databinding.ActivityMainBinding
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val markers = mutableMapOf<String, Marker>()
    private var roomRef: DatabaseReference? = null
    private var listener: ValueEventListener? = null
    private var firstFix = true

    private val myId by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) joinRoom()
        else Toast.makeText(this, "Standortberechtigung nötig", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid: User-Agent setzen (Pflicht laut OSM Tile Usage Policy)
        Configuration.getInstance().userAgentValue = packageName

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.map.setTileSource(TileSourceFactory.MAPNIK)
        b.map.setMultiTouchControls(true)
        b.map.controller.setZoom(6.0)
        b.map.controller.setCenter(GeoPoint(48.35, 8.97)) // Start: Hechingen-Region

        // Eigener blauer Standortpunkt
        val myLoc = MyLocationNewOverlay(GpsMyLocationProvider(this), b.map)
        myLoc.enableMyLocation()
        b.map.overlays.add(myLoc)

        b.btnJoin.setOnClickListener {
            if (b.etRoom.text.isNullOrBlank()) {
                Toast.makeText(this, "Raum-Code eingeben", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (hasLocationPermission()) joinRoom()
            else permLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            ))
        }
        b.btnStop.setOnClickListener { leaveRoom() }
    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun joinRoom() {
        val room = b.etRoom.text.toString().trim()

        startForegroundService(Intent(this, LocationService::class.java).apply {
            putExtra("room", room)
            putExtra("myId", myId)
        })

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(room)
        listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                for (child in snap.children) {
                    val id = child.key ?: continue
                    if (id == myId) continue
                    val lat = child.child("lat").getValue(Double::class.java) ?: continue
                    val lng = child.child("lng").getValue(Double::class.java) ?: continue
                    val pos = GeoPoint(lat, lng)

                    val m = markers.getOrPut(id) {
                        Marker(b.map).apply {
                            title = "Partner"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            b.map.overlays.add(this)
                        }
                    }
                    m.position = pos
                    if (firstFix) {
                        b.map.controller.animateTo(pos, 16.0, 800L)
                        firstFix = false
                    }
                }
                // Offline gegangene Nutzer entfernen
                markers.keys.retainAll { key ->
                    if (snap.hasChild(key)) true
                    else { b.map.overlays.remove(markers[key]); false }
                }
                b.map.invalidate()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        roomRef!!.addValueEventListener(listener!!)
        Toast.makeText(this, "Raum \"$room\" beigetreten", Toast.LENGTH_SHORT).show()
    }

    private fun leaveRoom() {
        stopService(Intent(this, LocationService::class.java))
        listener?.let { roomRef?.removeEventListener(it) }
        roomRef?.child(myId)?.removeValue()
        markers.values.forEach { b.map.overlays.remove(it) }
        markers.clear()
        firstFix = true
        b.map.invalidate()
    }

    override fun onResume() { super.onResume(); b.map.onResume() }
    override fun onPause() { super.onPause(); b.map.onPause() }
    override fun onDestroy() { leaveRoom(); super.onDestroy() }
}
