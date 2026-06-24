package com.pumpkiiings.pklogin.api;

public interface AccountData {

    String getRealName();
    
    String getAddress();

    String getUuidType();

    String getRandomUuid();

    String getDiscordId();

    String getEmailAddress();

    long getLastLogin();

    long getRegDate();

}
