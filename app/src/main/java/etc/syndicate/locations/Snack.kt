package etc.syndicate.locations

import com.google.android.material.snackbar.Snackbar

class Snack {
    var message = ""
    var length = Snackbar.LENGTH_LONG

    constructor(message: String, length: Int) {
        this.message = message
        this.length = length
    }

    constructor(message: String) {
        this.message = message
    }
}
