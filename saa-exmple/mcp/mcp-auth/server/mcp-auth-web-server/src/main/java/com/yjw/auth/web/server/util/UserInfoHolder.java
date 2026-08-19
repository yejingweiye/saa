package com.yjw.auth.web.server.util;

public class UserInfoHolder {

    public static final InheritableThreadLocal<String> userInfoContext = new InheritableThreadLocal<>();

    public static String getUserInfo(){
        return userInfoContext.get();
    }

    public static void setUserInfo(String userInfo){
        userInfoContext.set(userInfo);
    }
}
