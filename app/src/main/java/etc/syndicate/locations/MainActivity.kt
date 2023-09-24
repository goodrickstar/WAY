package etc.syndicate.locations

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener
import com.google.android.gms.maps.GoogleMap.OnCameraMoveListener
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import etc.syndicate.locations.MainActivity.ShortcutAdapter.MyViewHolder
import etc.syndicate.locations.databinding.MainActivityLayoutBinding
import java.util.Locale

@SuppressLint("NotifyDataSetChanged")
class MainActivity : FragmentActivity(),
    MI,
    ValueEventListener,
    ChildEventListener,
    View.OnClickListener,
    OnMapReadyCallback,
    OnCameraIdleListener,
    OnMarkerClickListener,
    OnCameraMoveListener {

    private var user: User = User()
    private lateinit var binding: MainActivityLayoutBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var snackbar: Snackbar
    private lateinit var locale: Locale
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private val shortcutsAdapter = ShortcutAdapter()
    private val profileOptions = RequestOptions().circleCrop()
    private val stateMap: MutableMap<String, String> = HashMap()
    private var position: Position = Position(40.7128, 74.0060, 17.787579f)
    private var sharing = false
    private val gson = Gson()
    private var following: String = ""
    private var follow = true
    private var mapType = MapType.NORMAL
    private val markers = ArrayList<Marker>()
    private val circles = ArrayList<Circle>()
    private val users = ArrayList<Coordinates>()
    private val notifications = ArrayList<UserNotification>()
    private lateinit var cameraPosition :CameraPosition

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseAuth.getInstance().currentUser?.let {
            user.userId = it.uid
            user.name = it.displayName
            user.email = it.email
            user.photoUrl = it.photoUrl.toString()
        }
        databaseReference = Utils.getDatabase().reference
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locale = Locale.getDefault()
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        sharing = sharedPreferences.getBoolean("sharing", false) && permissionGranted()
        follow = sharedPreferences.getBoolean("follow", false)
        position = gson.fromJson(
            sharedPreferences.getString(
                "position",
                gson.toJson(Position(40.7128, 74.0060, 17.787579f))
            ), Position::class.java
        )
        following = sharedPreferences.getString("following", "")!!
        sharedPreferences.getString("maptype", MapType.NORMAL.name)?.let {
            mapType = MapType.valueOf(it)
        }
        stateMap.putAll(createStateShorts())
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        binding.sharingIcon.setOnClickListener(this)
        binding.profilePhoto.setOnClickListener(this)
        binding.layersButton.setOnClickListener(this)
        binding.settingsButton.setOnClickListener(this)
        binding.followButton.setOnClickListener(this)
        binding.circlesButton.setOnClickListener(this)
        binding.shortcuts.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.shortcuts.adapter = shortcutsAdapter
        binding.userName.text = user.name
        binding.userLocation.text = ""
        databaseReference.child("users").child(user.userId).setValue(user)
        updateUi()
        GlideImageLoader(this, binding.profilePhoto, findViewById(R.id.progressBar)).load(
            user.photoUrl,
            profileOptions
        )
        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                if (result.locations.isNotEmpty() && circles.isNotEmpty()) {
                    for (location in result.locations) {
                        location.log()
                        if (location.hasAccuracy()){
                            locationLabel(location)
                            for (circle in circles) {
                                "Location updated to circle ${circle.name}".log()
                                databaseReference.child("locations").child(circle.id).child(user.userId).setValue(location.coordinates())
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (permissionGranted() && sharing) {
            stopService(Intent(this, LocationService::class.java))
            startSharing()
        }
        startListening()
    }

    override fun onResume() {
        super.onResume()
        checkForNotifications()
    }

    override fun onStop() {
        super.onStop()
        if (permissionGranted() && sharing && circles.isNotEmpty()) startForegroundService(
            Intent(this, LocationService::class.java).putExtra("user", gson.toJson(user))
                .putExtra("circles", gson.toJson(circles))
        )
        stopListening()
        stopSharing()
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.edit().putString("position", gson.toJson(position)).apply()
        sharedPreferences.edit().putBoolean("sharing", sharing).apply()
        sharedPreferences.edit().putBoolean("follow", follow).apply()
        sharedPreferences.edit().putString("following", following).apply()
    }

    override fun moveCamera(lattitude: Double, longitude: Double, zoom: Float) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lattitude, longitude), zoom))
    }

    override fun onCameraIdle() {
        position.latitude = map.cameraPosition.target.latitude
        position.longitude = map.cameraPosition.target.longitude
        position.zoom = map.cameraPosition.zoom
    }

    override fun onCameraMove() {}

    override fun onMarkerClick(marker: Marker): Boolean {
        val coordinates = marker.tag as Coordinates
        marker.snippet = Utils.since(coordinates.stamp)
        if (marker.isInfoWindowShown) marker.hideInfoWindow()
        else marker.showInfoWindow()
        return false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = mapType.value
        map.isIndoorEnabled = true
        map.uiSettings.isRotateGesturesEnabled = false
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        map.setOnCameraIdleListener(this)
        map.setOnCameraMoveListener(this)
        map.setOnMarkerClickListener(this)
        if (this.permissionGranted()) map.isMyLocationEnabled = true
        if (users.isNotEmpty()) {
            val index = indexOfUsers(following)
            if (index != -1) {
                val user = users[index]
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(user.latitude, user.longitude), position.zoom))
            } else map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(position.latitude, position.longitude), position.zoom))
        } else map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(position.latitude, position.longitude), position.zoom))
    }

    override fun onClick(view: View) {
        view.vibrate()
        when (view.id) {
            R.id.circles_button -> showCirclesDialog(0, null)
            R.id.sharingIcon -> if (!sharing) {
                if (permissionGranted()) {
                    showSnack(Snack("Location sharing ON", Snackbar.LENGTH_SHORT))
                    sharing = true
                    startSharing()
                } else requestionLocationPermisions()
            } else {
                showSnack(Snack("Location sharing OFF", Snackbar.LENGTH_SHORT))
                removeLocations()
                sharing = false
                stopSharing()
            }

            R.id.follow_button -> {
                if (follow) {
                    showSnack(Snack("Follow mode DISABLED", Snackbar.LENGTH_SHORT))
                    follow = false
                } else {
                    showSnack(Snack("Follow mode ENABLED", Snackbar.LENGTH_SHORT))
                    follow = true
                    if (following.isNotEmpty()) {
                        val markerIndex = indexOfMarkers(following)
                        if (markerIndex != -1) {
                            val marker = markers[markerIndex]
                            moveCamera(
                                marker.position.latitude,
                                marker.position.longitude,
                                position.zoom
                            )
                        }
                    }
                }
                updateUi()
            }

            R.id.settings_button -> {
                following = user.userId
                shortcutsAdapter.notifyDataSetChanged()
                if (permissionGranted()) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location ->
                        moveCamera(
                            location.latitude,
                            location.longitude,
                            position.zoom
                        )
                    }
                }
            }

            R.id.layers_button -> {
                mapType = when (mapType) {
                    MapType.NORMAL -> MapType.HYBRID
                    MapType.HYBRID -> MapType.NORMAL
                }
                map.mapType = mapType.value
                sharedPreferences.edit().putString("maptype", mapType.name).apply()
                when (mapType) {
                    MapType.NORMAL -> showSnack(Snack("Map set to NORMAL", Snackbar.LENGTH_SHORT))
                    MapType.HYBRID -> showSnack(Snack("Map set to HYBRID", Snackbar.LENGTH_SHORT))
                }
            }
        }
    }

    override fun showCirclesDialog(mode: Int, circle: Circle?) {
        if (circles.isEmpty()) showNewCircleDialog() else {
            val circlesDialog = CirclesDialog()
            val bundle = Bundle()
            bundle.putString("data", gson.toJson(circles))
            bundle.putString("user", gson.toJson(user))
            bundle.putString("circle", gson.toJson(circle))
            bundle.putInt("mode", mode)
            circlesDialog.arguments = bundle
            circlesDialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.mydialog)
            circlesDialog.show(supportFragmentManager, "circlesDialog")
        }
    }

    override fun showNewCircleDialog() {
        val createCircleDialog = CreateCircleDialog()
        val bundle = Bundle()
        bundle.putBoolean("empty", circles.isEmpty())
        createCircleDialog.arguments = bundle
        createCircleDialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.mydialog)
        createCircleDialog.show(supportFragmentManager, "createCircleDialog")
    }

    override fun createNewCircle(name: String) {
        databaseReference.child("circles").push().key?.let {
            val circle = Circle(it, name, user)
            databaseReference.child("circles").child(circle.id).setValue(circle)
                .addOnSuccessListener {
                    databaseReference.child("subscriptions").child(user.userId).child(circle.id)
                        .setValue(circle)
                        .addOnSuccessListener { showCirclesDialog(1, circle) }
                    databaseReference.child("rollcall").child(circle.id).child(user.userId)
                        .setValue(user)
                }
        }

    }

    override fun deleteCircle(circle: Circle) {
        databaseReference.child("circles").child(circle.id).removeValue()
            .addOnSuccessListener {
                databaseReference.child("subscriptions").child(user.userId).child(circle.id)
                    .removeValue()
                    .addOnSuccessListener { showCirclesDialog(0, null) }
                databaseReference.child("rollcall").child(circle.id)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (child in dataSnapshot.children) {
                                child.getValue(User::class.java)?.let { user ->
                                    databaseReference.child("subscriptions").child(user.userId)
                                        .child(circle.id).removeValue()
                                    if (user.userId != this@MainActivity.user.userId) {
                                        databaseReference.push().key?.let { key ->
                                            sendMessage(
                                                user.userId, UserNotification(
                                                    key,
                                                    "Circle was deleted",
                                                    user.name + " deleted " + circle.name,
                                                    user
                                                )
                                            )
                                        }
                                    } else postNotification(
                                        "Circle was deleted",
                                        circle.name + " was successfully deleted"
                                    )
                                }
                            }
                            databaseReference.child("rollcall").child(circle.id).removeValue()
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
            }
    }

    override fun leaveCircle(circle: Circle) {
        databaseReference.child("subscriptions").child(user.userId).child(circle.id)
            .removeValue().addOnSuccessListener {
                showCirclesDialog(0, null)
                databaseReference.child("rollcall").child(circle.id).child(user.userId)
                    .removeValue().addOnSuccessListener {
                        postNotification("Left Circle", "You successfully left " + circle.name)
                    }
            }
    }

    override fun invite(circle: Circle) {
        val inviteUser = InviteUser()
        val bundle = Bundle()
        bundle.putString("user", gson.toJson(user))
        bundle.putString("circle", gson.toJson(circle))
        inviteUser.arguments = bundle
        inviteUser.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.mydialog)
        inviteUser.show(supportFragmentManager, "inviteUser")
    }

    fun postNotification(title: String, content: String) {
        notifications.add(UserNotification("", title, content, user))
        checkForNotifications()
    }

    private fun sendMessage(receivingId: String, notification: UserNotification) {
        databaseReference.child("notifications").child(receivingId).child("open")
            .child(notification.notificationId).setValue(notification)
    }

    private fun checkForNotifications() {
        if (notifications.isNotEmpty()) {
            val notificationMessage = NotificationMessage()
            val bundle = Bundle()
            bundle.putString("data", gson.toJson(notifications[0]))
            notificationMessage.arguments = bundle
            notificationMessage.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.mydialog)
            notificationMessage.show(supportFragmentManager, "notification")
        }
    }

    override fun deleteNotification(notification: UserNotification) {
        if (notification.notificationId.isEmpty()) {
            notifications.remove(notification)
            return
        }
        databaseReference.child("notifications").child(user.userId).child("open")
            .child(notification.notificationId).removeValue()
        databaseReference.child("notifications").child(user.userId).child("closed")
            .child(notification.notificationId).setValue(notification)
    }

    override fun onDataChange(dataSnapshot: DataSnapshot) { //notifications
        notifications.clear()
        for (child in dataSnapshot.children) {
            child.getValue(UserNotification::class.java)?.let {
                notifications.add(it)
            }
        }
        checkForNotifications()
    }

    override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) { //Circles
        if (dataSnapshot.key == user.userId) return
        dataSnapshot.getValue(Coordinates::class.java)?.let { coordinates ->
            if (indexOfMarkers(dataSnapshot.key) == -1) {
                with(
                    map.addMarker(
                        MarkerOptions().position(
                            LatLng(
                                coordinates.latitude,
                                coordinates.longitude
                            )
                        )
                    )
                ) {
                    this?.let { marker ->
                        marker.tag = coordinates
                        marker.isFlat = true
                        marker.title = coordinates.user.name
                        marker.snippet = Utils.since(coordinates.stamp)
                        markers.add(marker)
                    }
                }
            }
            if (indexOfUsers(dataSnapshot.key) == -1) {
                users.add(coordinates)
                shortcutsAdapter.notifyItemInserted(indexOfUsers(coordinates.user.userId))
            }
            if (following.isNotEmpty()) {
                if (dataSnapshot.key == following) {
                    moveCamera(coordinates.latitude, coordinates.longitude, position.zoom)
                    position.latitude = coordinates.latitude
                    position.longitude = coordinates.longitude
                }
            }
        }
    }

    override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) { //Circles
        if (dataSnapshot.key == user.userId) return
        val markerIndex = indexOfMarkers(dataSnapshot.key)
        val userIndex = indexOfUsers(dataSnapshot.key)
        if (markerIndex != -1) {
            //animate marker change
            dataSnapshot.getValue(Coordinates::class.java)?.let { coordinates ->
                val wasShown = markers[markerIndex].isInfoWindowShown
                markers[markerIndex].tag = coordinates
                markers[markerIndex].snippet = Utils.since(coordinates.stamp)
                val marker = markers[markerIndex]
                val startValues = doubleArrayOf(marker.position.latitude, marker.position.longitude)
                val endValues = doubleArrayOf(coordinates.latitude, coordinates.longitude)
                val latLngAnimator =
                    ValueAnimator.ofObject(DoubleArrayEvaluator(), startValues, endValues)
                latLngAnimator.duration = 600
                latLngAnimator.interpolator = DecelerateInterpolator()
                latLngAnimator.addUpdateListener { animation: ValueAnimator ->
                    val animatedValue = animation.animatedValue as DoubleArray
                    markers[markerIndex].position = LatLng(animatedValue[0], animatedValue[1])
                }
                latLngAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        super.onAnimationStart(animation)
                        if (wasShown) markers[markerIndex].hideInfoWindow()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        if (wasShown) markers[markerIndex].showInfoWindow()
                    }
                })
                latLngAnimator.start()
                users[userIndex] = coordinates
                if (following.isNotEmpty() && follow) {
                    if (dataSnapshot.key == following) {
                        moveCamera(coordinates.latitude, coordinates.longitude, position.zoom)
                        position.latitude = coordinates.latitude
                        position.longitude = coordinates.longitude
                    }
                }
            }
        }

    }

    override fun onChildRemoved(dataSnapshot: DataSnapshot) { //Circles
        val userId = dataSnapshot.key
        if (userId == user.userId) return
        val markerIndex = indexOfMarkers(userId)
        val userIndex = indexOfUsers(userId)
        if (markerIndex != -1) markers.removeAt(markerIndex)
        if (userIndex != -1) {
            users.removeAt(userIndex)
            shortcutsAdapter.notifyItemRemoved(userIndex)
        }
        if (following == userId) following = ""
    }

    override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) { //Circles
    }

    override fun onCancelled(databaseError: DatabaseError) {}

    private fun removeLocations() {
        for (circle in circles) {
            databaseReference.child("locations").child(circle.id).child(user.userId)
                .removeValue()
        }
    }

    private fun startSharing() {
        if (this.permissionGranted()) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, object : LocationCallback(){
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    if (result.locations.isNotEmpty() && circles.isNotEmpty()) {
                        for (location in result.locations) {
                            location.log()
                            locationLabel(location)
                            for (circle in circles) {
                                "Location updated to circle ${circle.name}".log()
                                databaseReference.child("locations").child(circle.id).child(user.userId).setValue(location.coordinates())
                            }
                        }
                    }
                }
            }, Looper.getMainLooper())
            updateUi()
        }
    }

    private fun stopSharing() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        for (circle in circles) {
            databaseReference.child("locations").child(circle.id).child(user.userId).removeValue()
        }
        updateUi()
    }

    private fun updateUi() {
        if (sharing) binding.sharingIcon.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.location_share_w
            )
        ) else binding.sharingIcon.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.location_share_g
            )
        )
        if (follow) binding.followButton.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.follow_yes
            )
        ) else binding.followButton.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.follow_no
            )
        )
    }

    private fun requestionLocationPermisions() {
        if (!permissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                AlertDialog.Builder(this)
                    .setTitle("Location Sharing")
                    .setMessage("This app revolves around the Location permission, please accept to allow functionality")
                    .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            99
                        )
                    }
                    .create()
                    .show()
            } else ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                99
            )
        }
    }

    private fun permissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 99) {
            showSnack(Snack("Location sharing ON", Snackbar.LENGTH_SHORT))
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            map.isMyLocationEnabled = true
            sharing = true
            startSharing()
        }
    }


    override fun database(): DatabaseReference {
        return databaseReference
    }

    private fun startListening() {
        databaseReference.child("notifications").child(user.userId).child("open")
            .addValueEventListener(this)
        databaseReference.child("subscriptions").child(user.userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    circles.clear()
                    for (data in snapshot.children) {
                        data.getValue(Circle::class.java)?.let {
                            circles.add(it)
                        }
                    }
                    for (circle in circles) {
                        databaseReference.child("locations").child(circle.id)
                            .addChildEventListener(this@MainActivity)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun stopListening() {
        users.clear()
        shortcutsAdapter.notifyDataSetChanged()
        for (circle in circles) {
            databaseReference.child("locations").child(circle.id)
                .removeEventListener((this as ChildEventListener))
        }
        circles.clear()
        databaseReference.child("notifications").child(user.userId).child("open")
            .removeEventListener(
                (this as ValueEventListener)
            )
    }


    private fun indexOfUsers(id: String?): Int {
        for (i in users.indices) {
            if (users[i].user.userId == id) return i
        }
        return -1
    }

    private fun indexOfMarkers(userId: String?): Int {
        for (i in markers.indices) {
            val coordinates = markers[i].tag as Coordinates
            if (userId == coordinates.user.userId) return i
        }
        return -1
    }

    private fun indexOfCircles(id: String): Int {
        for (i in circles.indices) {
            if (circles[i].id == id) return i
        }
        return -1
    }

    private fun showSnack(snack: Snack) {
        snackbar =
            Snackbar.make(findViewById(R.id.coordinator), snack.message, Snackbar.LENGTH_SHORT)
        val view = snackbar.view
        val tv = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        tv.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        if (snack.length == Snackbar.LENGTH_INDEFINITE) {
            snackbar.setActionTextColor(Color.WHITE)
            snackbar.setAction("10 4") { v: View ->
                v.vibrate()
                snackbar.dismiss()
            }
        } else tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        snackbar.show()
    }

    @SuppressLint("NewApi")
    private fun locationLabel(location: Location) {
        val geoCoder = Geocoder(this@MainActivity, locale)
        geoCoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
            for (address in addresses) {
                val city = address.locality
                val state = address.adminArea
                if (address.countryCode != null) {
                    if (city != null && state != null) {
                        if (address.countryCode == "US") binding.mapLocation.text =
                            city.trim { it <= ' ' } + ", " + getAbbreviationFromUSState(state)
                        else binding.mapLocation.text =
                            address.locality + ", " + address.countryCode
                    }
                }
            }
        }
    }

    private fun getAbbreviationFromUSState(state: String): String? {
        return if (stateMap.containsKey(state)) stateMap[state] else state
    }

    private fun createStateShorts(): Map<String, String> {
        val map: MutableMap<String, String> = HashMap()
        map["Alabama"] = "AL"
        map["Alaska"] = "AK"
        map["Alberta"] = "AB"
        map["Arizona"] = "AZ"
        map["Arkansas"] = "AR"
        map["British Columbia"] = "BC"
        map["California"] = "CA"
        map["Colorado"] = "CO"
        map["Connecticut"] = "CT"
        map["Delaware"] = "DE"
        map["District of Columbia"] = "DC"
        map["Florida"] = "FL"
        map["Georgia"] = "GA"
        map["Guam"] = "GU"
        map["Hawaii"] = "HI"
        map["Idaho"] = "ID"
        map["Illinois"] = "IL"
        map["Indiana"] = "IN"
        map["Iowa"] = "IA"
        map["Kansas"] = "KS"
        map["Kentucky"] = "KY"
        map["Louisiana"] = "LA"
        map["Maine"] = "ME"
        map["Manitoba"] = "MB"
        map["Maryland"] = "MD"
        map["Massachusetts"] = "MA"
        map["Michigan"] = "MI"
        map["Minnesota"] = "MN"
        map["Mississippi"] = "MS"
        map["Missouri"] = "MO"
        map["Montana"] = "MT"
        map["Nebraska"] = "NE"
        map["Nevada"] = "NV"
        map["New Brunswick"] = "NB"
        map["New Hampshire"] = "NH"
        map["New Jersey"] = "NJ"
        map["New Mexico"] = "NM"
        map["New York"] = "NY"
        map["Newfoundland"] = "NF"
        map["North Carolina"] = "NC"
        map["North Dakota"] = "ND"
        map["Northwest Territories"] = "NT"
        map["Nova Scotia"] = "NS"
        map["Nunavut"] = "NU"
        map["Ohio"] = "OH"
        map["Oklahoma"] = "OK"
        map["Ontario"] = "ON"
        map["Oregon"] = "OR"
        map["Pennsylvania"] = "PA"
        map["Prince Edward Island"] = "PE"
        map["Puerto Rico"] = "PR"
        map["Quebec"] = "QC"
        map["Rhode Island"] = "RI"
        map["Saskatchewan"] = "SK"
        map["South Carolina"] = "SC"
        map["South Dakota"] = "SD"
        map["Tennessee"] = "TN"
        map["Texas"] = "TX"
        map["Utah"] = "UT"
        map["Vermont"] = "VT"
        map["Virgin Islands"] = "VI"
        map["Virginia"] = "VA"
        map["Washington"] = "WA"
        map["West Virginia"] = "WV"
        map["Wisconsin"] = "WI"
        map["Wyoming"] = "WY"
        map["Yukon Territory"] = "YT"
        return map
    }

    internal inner class ShortcutAdapter : RecyclerView.Adapter<MyViewHolder?>(),
        View.OnClickListener {

        override fun onClick(view: View) {
            view.vibrate()
            val coordinates = view.tag as Coordinates
            following = coordinates.user.userId
            val markerIndex = indexOfMarkers(coordinates.user.userId)
            if (markerIndex != -1) {
                if (markers[markerIndex].isInfoWindowShown) markers[markerIndex].hideInfoWindow()
            }
            moveCamera(coordinates.latitude, coordinates.longitude, position.zoom)
            position.latitude = coordinates.latitude
            position.longitude = coordinates.longitude
            for (x in users.indices) {
                users[x].checked = users[x].user.userId == following
            }
            shortcutsAdapter.notifyDataSetChanged()
            object : CountDownTimer(30000, 1000) {

                override fun onTick(millisUntilFinished: Long) {
                }

                override fun onFinish() {
                    for (x in users.indices) {
                        users[x].checked = false
                    }
                    shortcutsAdapter.notifyDataSetChanged()
                }
            }.start()

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(layoutInflater.inflate(R.layout.shortcuts_row, parent, false))
        }

        override fun onBindViewHolder(holder: MyViewHolder, i: Int) {
            val coordinates = users[i]
            GlideImageLoader(this@MainActivity, holder.profile, holder.progressBar).load(
                coordinates.user.photoUrl, profileOptions
            )
            holder.profile.tag = coordinates
            holder.profile.setOnClickListener(this)
            if (coordinates.checked) {
                holder.label.visibility = View.VISIBLE
                holder.label.text = coordinates.user.name
            } else {
                holder.label.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int {
            return users.size
        }

        internal inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var profile: ImageView
            var label: TextView
            var progressBar: ProgressBar

            init {
                profile = itemView.findViewById(R.id.profile)
                label = itemView.findViewById(R.id.name)
                progressBar = itemView.findViewById(R.id.progressBar)
            }
        }
    }

}