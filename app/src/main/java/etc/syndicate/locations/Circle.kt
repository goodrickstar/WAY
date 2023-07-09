package etc.syndicate.locations

import java.time.Instant

class Circle {
    var id = ""
    var name = ""
    var owner : User = User()
    var stamp = Instant.now().epochSecond

    constructor(id: String, name: String, owner: User) {
        this.id = id
        this.name = name
        this.owner = owner
    }

    constructor()
}