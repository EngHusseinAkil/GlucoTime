package com.sugartrack

/** Centralized developer identity and optional external links. */
object DeveloperInfo {
    const val NAME = "Hussein Nabil Akil"
    // Optional private contact/payment details are excluded from the public source.
    const val PHONE = ""
    const val DISPLAY_PHONE = ""
    const val EMAIL = "husseinakilpc@gmail.com"
    const val WHISH_NUMBER = ""
}

object DeveloperLinks {
    val PHONE_URL = if (DeveloperInfo.PHONE.isBlank()) "" else "tel:${DeveloperInfo.PHONE}"
    val WHATSAPP_URL = if (DeveloperInfo.PHONE.isBlank()) "" else "https://wa.me/${DeveloperInfo.PHONE.replace("+", "")}"
    const val WHISH_SUPPORT_URL = ""
    const val CONTACT_URL = ""
    const val EMAIL = DeveloperInfo.EMAIL
    const val WEBSITE = ""
}
