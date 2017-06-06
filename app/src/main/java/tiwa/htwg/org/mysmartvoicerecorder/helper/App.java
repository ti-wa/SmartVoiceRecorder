package tiwa.htwg.org.mysmartvoicerecorder.helper;

public class App {
    //button codes
    public static final int PLAY = 100;
    public static final int RECORD = 101;
    public static final int STOP = 102;
    //other Information
    public static final int DURATION = 200;
    public static final int PLAYEDSECOND = 201;
    public static final int RECORDSECONDS = 203;
    public static final int NEWSEGMENTPLAYING = 400;
    public static final int RELOADUI = 300;
    //info what to do with record
    public static final int NEWRECORD = 500;
    public static final int NEWELEMENT = 501;
    public static final int APPENDELEMENT = 502;
    public static final int APPENDSEGMENT = 503;

    public static String getTag() {
        String tag = "";
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        for (int i = 0; i < ste.length; i++) {
            if (ste[i].getMethodName().equals("getTag")) {
                tag = ste[i + 1].getClassName() + "_" + ste[i + 1].getLineNumber();
            }
        }
        return tag;
    }
}

