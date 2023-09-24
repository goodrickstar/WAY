package etc.syndicate.locations

class User{
    constructor(userId: String, name: String?, email : String?, photoUrl: String)
    constructor()
    var userId : String = ""
    var name : String? = ""
    var email : String? = ""
    var photoUrl : String = ""
    var stamp = System.currentTimeMillis()
}