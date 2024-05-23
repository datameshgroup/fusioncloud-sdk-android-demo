package au.com.dmg.fusioncloud.android.demo;

import au.com.dmg.fusion.SaleToPOI;
import au.com.dmg.fusion.data.ErrorCondition;
import au.com.dmg.fusion.data.MessageCategory;
import au.com.dmg.fusion.data.MessageType;

public class FusionMessageResponse {
    Boolean isSuccessful;
    MessageType messageType;
    MessageCategory messageCategory;
    SaleToPOI saleToPOI;
    String displayMessage;
    ErrorCondition errorCondition;

    public void setMessage(boolean isSuccessful, MessageType messageType, MessageCategory messageCategory, SaleToPOI saleToPOI, String displayMsg) {
        this.isSuccessful = isSuccessful;
        this.messageType = messageType;
        this.messageCategory = messageCategory;
        this.saleToPOI = saleToPOI;
        this.displayMessage = displayMsg;
        this.errorCondition=null;
    }

    public void setMessage(MessageType messageType, MessageCategory messageCategory, SaleToPOI saleToPOI) {
        this.isSuccessful = true;
        this.messageType = messageType;
        this.messageCategory = messageCategory;
        this.saleToPOI = saleToPOI;
        this.displayMessage = "";
        this.errorCondition=null;

    }

    public void setMessage(boolean isSuccessful, String displayMsg) {
        this.isSuccessful = isSuccessful;
        this.messageType = null;
        this.messageCategory = null;
        this.saleToPOI = null;
        this.displayMessage = displayMsg;
        this.errorCondition=null;
    }
    public void setMessage(boolean isSuccessful, MessageType messageType, MessageCategory messageCategory, SaleToPOI saleToPOI, ErrorCondition err) {
        this.isSuccessful = isSuccessful;
        this.messageType = messageType;
        this.messageCategory = messageCategory;
        this.saleToPOI = saleToPOI;
        this.displayMessage = "";
        this.errorCondition=err;
    }
}
