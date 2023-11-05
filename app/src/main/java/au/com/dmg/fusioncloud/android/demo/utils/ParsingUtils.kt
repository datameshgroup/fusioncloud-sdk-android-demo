package au.com.dmg.fusioncloud.android.demo.utils

import android.widget.TextView
import au.com.dmg.fusion.util.BigDecimalAdapter
import au.com.dmg.fusion.util.PairingData
import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date

class ParsingUtils {

    companion object {
        var sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
//        lateinit var jsonLogs: TextView

        fun prettyPrintJson(json: Any?): String {
            val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
            return gson.toJson(json)
        }

        fun log(logData: String?) {
            //TODO return log instead of updating UI here?
            println(sdf.format(Date(System.currentTimeMillis())) + ": " + logData) // 2021.03.24.16.34.26
//            runOnUiThread {
//                jsonLogs!!.append("${sdf.format(Date(System.currentTimeMillis()))} : $logData")
//            }
        }


    }
}