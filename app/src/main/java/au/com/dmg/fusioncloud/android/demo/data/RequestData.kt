package au.com.dmg.fusioncloud.android.demo.data

import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.PaymentType
import au.com.dmg.fusion.request.paymentrequest.SaleItem
import java.io.Serializable
import java.math.BigDecimal

data class RequestData(
    var serviceID: String,
    var MessageCategory: MessageCategory,
    var PaymentType: PaymentType?,
    var requestedAmount: BigDecimal?,
    var tipAmount: BigDecimal?,
    var productCode:String?,
    var saleItem: SaleItem?
): Serializable
