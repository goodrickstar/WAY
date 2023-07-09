package com.cb3g.channel19

object Logger {
    fun e(msg: String) {
        println("I: !! $msg")
    }
    fun e(tag: String, text: String?) {
        println("E !! $tag : $text")
    }

    fun i(msg: String) {
        println("I: !! $msg")
    }

    fun i(tag: String, text: String) {
        println("I: !! $tag : $text")
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
