package au.com.dmg.fusioncloud.android.demo.utils

import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.MessageType
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.response.ResponseResult
import au.com.dmg.fusion.response.SaleToPOIResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FusionMessageHandler {
    var sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    var fusionMessageResponse = FusionMessageResponse()
    var messageCategory: MessageCategory? = null
    fun handle(
        request: SaleToPOIRequest
    ): FusionMessageResponse {
        log("Start SaleToPOIRequest")
        log(String.format("Request(JSON): %s", request.toJson()))
        if (request.messageHeader == null) {
            fusionMessageResponse.setMessage(false, "Invalid Message")
            return fusionMessageResponse
        }
        messageCategory = request.messageHeader.messageCategory
        if (messageCategory == MessageCategory.Display) {
            val displayRequest = request.displayRequest
            if (displayRequest != null) {
                log("Display Output = " + displayRequest.displayText)
                fusionMessageResponse.setMessage(
                    true,
                    MessageType.Request,
                    MessageCategory.Display,
                    null,
                    displayRequest.displayText
                )
                return fusionMessageResponse
            }
        }
        log("End SaleToPOIRequest")
        fusionMessageResponse.setMessage(false, "Unknown Error") //TODO Check validation
        return fusionMessageResponse
    }

    fun handle(
        response: SaleToPOIResponse
    ): FusionMessageResponse {
        log("Start SaleToPOIResponse")
        log(String.format("Response(JSON): %s", response.toJson()))
        messageCategory = response.messageHeader.messageCategory
        log("Message Category: $messageCategory")
        val responseResult: ResponseResult
        when (messageCategory) {
            MessageCategory.Event -> {
                val eventNotification = response.eventNotification
                log("Event Details: " + eventNotification!!.eventDetails)
                fusionMessageResponse.setMessage(
                    MessageType.Response,
                    MessageCategory.Event,
                    response
                )
            }

            MessageCategory.Login -> {
                responseResult = response.loginResponse!!.response.result
                if (responseResult == ResponseResult.Success) {
                    fusionMessageResponse.setMessage(
                        true,
                        MessageType.Response,
                        MessageCategory.Login,
                        response,
                        "LOGIN SUCCESSFUL"
                    )
                } else {
                    val additionalResponse = response.loginResponse!!.response.additionalResponse
                    fusionMessageResponse.setMessage(
                        false,
                        MessageType.Response,
                        MessageCategory.Login,
                        response,
                        additionalResponse
                    )
                }
            }

            MessageCategory.Payment -> {
                responseResult = response.paymentResponse!!.response.result
                val paymentType = response.paymentResponse!!
                    .paymentResult!!.paymentType
                var type = ""
                type = if (paymentType == PaymentType.Normal) {
                    "Payment"
                } else {
                    paymentType!!.name
                }
                if (responseResult == ResponseResult.Success) {
                    fusionMessageResponse.setMessage(
                        true,
                        MessageType.Response,
                        MessageCategory.Payment,
                        response,
                        type.uppercase(
                            Locale.getDefault()
                        ) + " SUCCESSFUL"
                    )
                } else {
                    val additionalResponse = response.paymentResponse!!.response.additionalResponse
                    fusionMessageResponse.setMessage(
                        false,
                        MessageType.Response,
                        MessageCategory.Payment,
                        response,
                        additionalResponse
                    )
                }
            }

            MessageCategory.TransactionStatus -> {
                responseResult = response.transactionStatusResponse!!.response.result
                if (responseResult == ResponseResult.Success) {
                    fusionMessageResponse.setMessage(
                        true,
                        MessageType.Response,
                        MessageCategory.TransactionStatus,
                        response,
                        "TRANSACTION STATUS FOUND"
                    )
                } else {
                    val errorCondition = response.transactionStatusResponse!!
                        .response.errorCondition
                    val additionalResponse = response.transactionStatusResponse!!
                    .response.additionalResponse
                    fusionMessageResponse.setMessage(
                        false,
                        MessageType.Response,
                        MessageCategory.TransactionStatus,
                        response,
                        errorCondition,
                        additionalResponse
                    )
                }
            }

            else -> {
                //TODO Unknown MessageCategory
            }
        }
        log("End SaleToPOIResponse")
        return fusionMessageResponse
    }

    private fun log(logData: String) {
        println(sdf.format(Date(System.currentTimeMillis())) + ": " + TAG + ": " + logData) // 2021.03.24.16.34.26
    }

    companion object {
        private const val TAG = "FusionMessageHandler"
    }
}