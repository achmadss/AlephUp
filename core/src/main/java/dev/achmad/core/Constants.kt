package dev.achmad.core

object Constants {

    object Network {
        const val BASE_URL = "https://mock-api.achmad.dev/post-attendance"
    }

    object Device {

        object Battery {
            const val REQUEST_BATTERY_OPTIMIZATION_REQUEST_CODE = 1001
        }

        object Wifi {
            val BSSID_TARGETS = listOf(
                "8a:c4:34:5d:1e:79"
            )
            const val SSID_TARGET = "Susilo"
        }

    }

    object Auth {

        object Google {
            val ALLOWED_EMAIL_DOMAINS = setOf(
                "@aleph-labs.com",
            )
        }

    }

}