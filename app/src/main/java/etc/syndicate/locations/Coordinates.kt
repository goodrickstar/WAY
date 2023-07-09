package etc.syndicate.locations

import java.time.Instant

class Coordinates {
    var user : User = User()
    var latitude = 0.0
    var longitude = 0.0
    var bearing = 0f
    var speed = 0f
    var alititude = 0.0
    var stamp = Instant.now().epochSecond
    var checked = false

    constructor()

    constructor(latitude: Double, longitude: Double, bearing: Float, speed: Float, alititude: Double) {
        this.latitude = latitude
        this.longitude = longitude
        this.bearing = bearing
        this.speed = speed
        this.alititude = alititude
    }
    constructor(user : User, latitude: Double, longitude: Double, bearing: Float, speed: Float, alititude: Double) {
        this.latitude = latitude
        this.longitude = longitude
        this.bearing = bearing
        this.speed = speed
        this.alititude = alititude
    }

}
