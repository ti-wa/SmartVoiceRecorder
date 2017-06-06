package tiwa.htwg.org.mysmartvoicerecorder.helper;

import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SupportMethods {
    private static final int[] colors = new int[]{Color.RED, Color.BLACK, Color.MAGENTA, Color.GRAY, Color.BLUE, Color.DKGRAY};
    private static final String dateFormate = "EEE d MMM yyyy, HH:mm";
    private static int colorIterator = 0;

    public static int getUnixTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public static String millisecondsToHMS(int millisecondS) {
        String message = "";
        String hours_s;
        String minutues_s;
        String seconds_s;
        long milliseconds = (long) millisecondS;
        if (milliseconds >= 1000) {
            int seconds = (int) (milliseconds / 1000) % 60;
            int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
            int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
            if (hours < 10) {
                hours_s = "0" + hours;
            } else {
                hours_s = String.valueOf(hours);
            }
            if (minutes < 10) {
                minutues_s = "0" + minutes;
            } else {
                minutues_s = String.valueOf(minutes);
            }
            if (seconds < 10) {
                seconds_s = "0" + seconds;
            } else {
                seconds_s = String.valueOf(seconds);
            }
            message = hours_s + ":" + minutues_s + ":" + seconds_s;
        }
        return message;
    }

    public static void createAppFolder() {
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "voiceRecorder");
        if (!folder.exists()) {
            Log.d(App.getTag(), "create App Folder");
            boolean result = folder.mkdir();
            if (!result) {
                Log.e(App.getTag(), "failed create folder");
            }
        }
    }

    public static void createFolderinAppFolder(String folderName) {
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "voiceRecorder" + File.separator + folderName);
        if (!folder.exists()) {
            Log.d(App.getTag(), "create Folder: " + folderName);
            boolean result = folder.mkdir();
            if (!result) {
                Log.e(App.getTag(), "failed create folder");
            }
        }
    }

    public static String unixTimeToFormated(int unixtime) {
        long dv = (long) unixtime * 1000;// its need to be in milisecond
        Date df = new Date(dv);
        return new SimpleDateFormat(dateFormate).format(df);
    }

    public static String getTimeFormated() {
        DateFormat df = new SimpleDateFormat(dateFormate);
        return df.format(Calendar.getInstance().getTime());
    }

    public static byte[] readFile(File file) throws IOException {
        Log.v(App.getTag(), "read File: " + file.getPath());
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }

    public static int getColor() {
        if (colors.length == colorIterator) {
            colorIterator = 0;
        }
        int color = colors[colorIterator];
        colorIterator++;
        return color;
    }
}
