package com.example.ppp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceStatus(
    @SerialName("is_outage") val isOutage: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("severity") val severity: String = "warning"
)
