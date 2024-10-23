package com.app.pustakam

import com.app.pustakam.extensions.isValidPhone
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonGreetingTest {

    @Test
    fun testExample() {
        assertTrue(Greeting().greet().contains("Hello"), "Check 'Hello' is mentioned")
    }
    @Test
    fun testForPhoneNumber(){
        val phone="+91595959595"
        val phone3="91595959595"
        val phone2="+*91595959595#"
        val phone4="AS+*91595959595#"
        val phone5="AS+*915 95959595#"
        assertTrue(phone.isValidPhone() , "Check your phone number ")
        assertTrue(phone2.isValidPhone() , "Check your phone number ")
        assertTrue(phone3.isValidPhone() , "Check your phone number ")
        assertFalse(phone4.isValidPhone() , "show not contain Alphas")
        assertFalse(phone5.isValidPhone() , "show not contain Alphas")
    }
}