package etc.syndicate.locations

import android.util.Log

object Logger {
    fun e(msg: String) {
        println("I: !! $msg")
    }
    fun e(tag: String, text: String?) {
        println("E !! $tag : $text")
    }

    fun i(msg: String) {
        Log.i("WAY", msg)
    }

    fun i(tag: String, text: String) {
        Log.i(tag, text)
    }

    fun i(stamp: String, stamp1: Long) {
        println("I: !! : $stamp $stamp1")
    }

    fun i(tag: String, varible: Float) {
        println("I: !! : $tag $varible")
    }
    
    fun i(tag: String, b: Boolean) {
        println("I: !! : $tag $b")
    }

    fun i(tag: String, exception: Exception) {
        println("I !! $tag : $exception")
    }

    fun e(tag: String, exception: Exception) {
        println("E !! $tag : $exception")
    }

}
