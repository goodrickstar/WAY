package etc.syndicate.locations

class User {
    var userId = ""
    var email = ""
    var name = ""
    var photoUrl = "https://firebasestorage.googleapis.com/v0/b/locations-95d30.appspot.com/o/icon.png?alt=media&token=9713385a-dbda-4e54-bb7e-03a6fe22351a"
    var stamp = 0L

    constructor(userId: String, email: String, name: String, photoUrl: String, stamp: Long) {
        this.userId = userId
        this.email = email
        this.name = name
        this.photoUrl = photoUrl
        this.stamp = stamp
    }



    constructor()
}