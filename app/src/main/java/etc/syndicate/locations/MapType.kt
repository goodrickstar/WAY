package etc.syndicate.locations

import com.google.android.gms.maps.GoogleMap

enum class MapType(val value: Int) {
    NORMAL(GoogleMap.MAP_TYPE_NORMAL), HYBRID(GoogleMap.MAP_TYPE_HYBRID)
}