package au.com.dmg.fusioncloud.android.demo;

public class Settings {
    public static String saleId = "VA POS"; // test environment only - replace for production
    public static String poiId = "DMGVA002"; // test environment only - replace for production
    public static String kek = "44DACB2A22A4A752ADC1BBFFE6CEFB589451E0FFD83F8B21"; // test environment only - replace for production
    public static String providerIdentification = "Company A"; // test environment only - replace for production
    public static String applicationName = "POS Retail";//"Android POS"; // test environment only - replace for production
    public static String softwareVersion = "01.00.00"; // test environment only - replace for production
    public static String certificationCode = "98cf9dfc-0db7-4a92-8b8cb66d4d2d7169"; // test environment only - replace for production

    public static String getSaleId(){
        return saleId;
    }
    public static String getPoiId(){
        return poiId;
    }
    public static String getKek(){
        return kek;
    }
    public static String getProviderIdentification(){
        return providerIdentification;
    }
    public static String getApplicationName(){
        return applicationName;
    }
    public static String getSoftwareVersion(){
        return softwareVersion;
    }
    public static String getCertificationCode(){
        return certificationCode;
    }

    public static void setSaleId(String saleId) {
        Settings.saleId = saleId;
    }

    public static void setPoiId(String poiId) {
        Settings.poiId = poiId;
    }

    public static void setKek(String kek) {
        Settings.kek = kek;
    }

    public static void setProviderIdentification(String providerIdentification) {
        Settings.providerIdentification = providerIdentification;
    }

    public static void setApplicationName(String applicationName) {
        Settings.applicationName = applicationName;
    }

    public static void setSoftwareVersion(String softwareVersion) {
        Settings.softwareVersion = softwareVersion;
    }

    public static void setCertificationCode(String certificationCode) {
        Settings.certificationCode = certificationCode;
    }
}
