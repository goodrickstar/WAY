package etc.syndicate.locations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationResult
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Extensions {
}

fun View.vibrate() {
    this.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}

fun Location.coordinates(): Coordinates {
    return Coordinates(
        latitude,
        longitude,
        bearing,
        speed,
        altitude
    )
}

fun Context.permissionAccepted(): Boolean {
    return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

fun Coordinates.log() {
    Log.i("WAY", Gson().toJson(this))
}

fun Circle.log() {
    Log.i("WAY", Gson().toJson(this))
}

fun User.log() {
    Log.i("WAY", Gson().toJson(this))
}

fun FirebaseUser.log() {
    Log.i("WAY", Gson().toJson(this))
}

fun String.log() {
    Log.i("WAY", Gson().toJson(this))
}

fun Location.log() {
    Log.i("WAY", Gson().toJson(this))
}

fun LocationResult.log() {
    Log.i("WAY", Gson().toJson(this))
}

suspend fun Geocoder.getAddress(latitude: Double, longitude: Double): Address? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { cont ->
                getFromLocation(latitude, longitude, 1) {
                    cont.resume(it.firstOrNull())
                }
            }
        } else {
            suspendCoroutine { cont ->
                @Suppress("DEPRECATION")
                val address = getFromLocation(latitude, longitude, 1)?.firstOrNull()
                cont.resume(address)
            }
        }
    } catch (e: Exception) {
        null
    }
}

