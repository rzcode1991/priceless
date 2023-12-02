package com.example.priceless

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(var id: String = "",
                var userName: String = "",
                var firstName: String = "",
                var lastName: String = "",
                var email: String = "",
                var image: String = "",
                var phoneNumber: Long = 0L,
                var wallet: String = "",
                var publicProfile: Boolean = true,
                var profileCompleted: Int = 0
): Parcelable