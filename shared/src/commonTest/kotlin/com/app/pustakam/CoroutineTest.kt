package com.app.pustakam

import kotlinx.coroutines.*
fun main(){
    val co = CoroutineTest().co()
}
class CoroutineTest {
    fun co() {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        GlobalScope.launch(exceptionHandler) {
            // Coroutine code
            GlobalScope.async{
                delay(2000)
            }
            delay(1000)
            GlobalScope.async{
                delay(20000)
            }.await()
            println("completed")
        }
    }
}