package com.dc.fitnessauthentication.customapi;

import com.github.scribejava.core.builder.api.DefaultApi10a;

/**
 * Created with IntelliJ IDEA.
 * User: khanhnguyen
 * Date: 04.06.14
 * Time: 17:04
 */
public class GarminAPI extends DefaultApi10a {
    private static final String AUTHORIZE_URL = "https://connect.garmin.com/oauthConfirm";
    private static final String REQUEST_TOKEN_RESOURCE = "https://connectapi.garmin.com/oauth-service/oauth/request_token";
    private static final String ACCESS_TOKEN_RESOURCE = "https://connectapi.garmin.com/oauth-service/oauth/access_token";

    @Override
    public String getRequestTokenEndpoint() {
        return REQUEST_TOKEN_RESOURCE;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_RESOURCE;
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return AUTHORIZE_URL;
    }


}