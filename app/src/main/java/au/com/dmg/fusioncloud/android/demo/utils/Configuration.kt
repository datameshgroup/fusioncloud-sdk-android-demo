package au.com.dmg.fusioncloud.android.demo.utils

import au.com.dmg.fusion.client.FusionClient

object Configuration {
    @JvmStatic
    var useTestEnvironment = true
    @JvmStatic
    var saleId = "VA POS" // test environment only - replace for production
    @JvmStatic
    var poiId = "DMGVA002" // test environment only - replace for production
    @JvmStatic
    var kek =
        "44DACB2A22A4A752ADC1BBFFE6CEFB589451E0FFD83F8B21" // test environment only - replace for production
    @JvmStatic
    var providerIdentification = "Company A" // test environment only - replace for production
    @JvmStatic
    var applicationName =
        "POS Retail" //"Android POS"; // test environment only - replace for production
    @JvmStatic
    var softwareVersion = "01.00.00" // test environment only - replace for production
    @JvmStatic
    var certificationCode =
        "98cf9dfc-0db7-4a92-8b8cb66d4d2d7169" // test environment only - replace for production
}