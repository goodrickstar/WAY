package etc.syndicate.locations

import java.time.Instant

class UserNotification{
    var notificationId = ""
    var mode = 0
    var title = ""
    var payload = ""
    var extra = ""
    var user : User = User()
    var stamp = Instant.now().epochSecond


    constructor(notificationId: String, title: String, extra: String, user: User) {
        this.notificationId = notificationId
        this.title = title
        this.extra = extra
        this.user = user
    }

    constructor()
}