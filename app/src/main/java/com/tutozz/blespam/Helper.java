package com.tutozz.blespam;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.util.Random;

public class Helper {
    private static final char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    public static Random random = new Random();
    public static int delay = 20;
    public static int[] delays = {0, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 6000, 8000, 10000, 12000, 15000, 18000, 20000};
    public static int MAX_LOOP = 50_000_000;

    public static boolean isPermissionGranted(Context c){
        return ActivityCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
    }
    public static byte[] convertHexToByteArray(String hex) {
        // Remove any "0x" prefix, if present
        hex = hex.replaceAll("0x", "");

        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String randomHexFiller(int size){
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < size; i++) {
            sb.append(hexDigits[random.nextInt(hexDigits.length)]);
        }
        return sb.toString();
    }
}
