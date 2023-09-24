package etc.syndicate.locations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationResult
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson

class Extensions {
}

fun View.vibrate() {
    this.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}

fun Location.coordinates() : Coordinates{
    return Coordinates(
        latitude,
        longitude,
        bearing,
        speed,
        altitude
    )
}

fun Context.permissionAccepted() : Boolean{
    return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

fun Coordinates.log(){
    Log.i("WAY", Gson().toJson(this))
}

fun Circle.log(){
    Log.i("WAY", Gson().toJson(this))
}
fun User.log(){
    Log.i("WAY", Gson().toJson(this))
}

fun FirebaseUser.log(){
    Log.i("WAY", Gson().toJson(this))
}

fun String.log(){
    Log.i("WAY", Gson().toJson(this))
}

fun Location.log(){
    Log.i("WAY", Gson().toJson(this))
}

fun LocationResult.log(){
    Log.i("WAY", Gson().toJson(this))
}