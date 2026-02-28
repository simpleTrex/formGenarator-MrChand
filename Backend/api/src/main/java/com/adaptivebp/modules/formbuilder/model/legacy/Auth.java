package com.adaptivebp.modules.formbuilder.model.legacy;

import java.util.EnumSet;

public class Auth {
    public static enum AUTH_ENUM {
        X, W, R
    }

    private EnumSet<AUTH_ENUM> auth;

    Auth() { auth.clear(); }

    public EnumSet<AUTH_ENUM> getAuth() { return auth; }
    public void setAuth(EnumSet<AUTH_ENUM> auth) { this.auth = auth; }
}
