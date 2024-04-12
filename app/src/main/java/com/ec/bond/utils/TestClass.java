package com.ec.bond.utils;

import android.util.Log;

public class TestClass {
    public static  String getAnswerC(String psw){

        if(psw.isEmpty()){
            Log.e("method_called","cool"+psw);
            return null;
        }else{
            Log.e("method_called","cool"+psw);
            return "true";
        }



    }
}
