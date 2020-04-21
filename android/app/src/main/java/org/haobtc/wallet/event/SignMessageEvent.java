package org.haobtc.wallet.event;

public class SignMessageEvent {

    private String signedRaw;
    public SignMessageEvent(String signedRaw) {
        this.signedRaw = signedRaw;
    }

    public String getSignedRaw() {
        return signedRaw;
    }

    public void setSignedRaw(String signedRaw) {
        this.signedRaw = signedRaw;
    }
}
