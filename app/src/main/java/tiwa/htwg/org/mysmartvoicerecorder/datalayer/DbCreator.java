package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;

class DbCreator extends SQLiteOpenHelper {
    public static final String TABLE_SEGMENTS = "segment";
    public static final String TABLE_TRANSCRIPT = "transcript";
    public static final String TABLE_RECORDELEMENT = "record_element";
    public static final String TABLE_RECORD = "record";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CREATIONDATE = "creationDate";
    public static final String COLUMN_UPDATEDATE = "updateDate";
    public static final String COLUMN_FK_TRANSCRIPT = "fk_transkript";
    public static final String COLUMN_AUDIODATAPATH = "audioDataPath";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_FK_RECORDELEMENT = "fk_recordelement";
    public static final String COLUMN_POSITION = "position";
    public static final String COLUMN_FK_RECORD = "fk_record";
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "MySmartVoiceRecorder.db";
    // Database creation sql statement
    private static final String DATABASE_CREATE_SEGMENTS = "create table if not exists " +
            TABLE_SEGMENTS + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_CREATIONDATE + " integer not null, " +
            COLUMN_UPDATEDATE + " integer, " +
            COLUMN_FK_TRANSCRIPT + " integer not null, " +
            COLUMN_AUDIODATAPATH + " text not null, " +
            COLUMN_DURATION + " real," +
            COLUMN_FK_RECORDELEMENT + " integer not null, " +
            COLUMN_POSITION + " integer not null," +
            " FOREIGN KEY(" + COLUMN_FK_TRANSCRIPT + ") REFERENCES " + TABLE_TRANSCRIPT + "(" + COLUMN_ID + ")," +
            " FOREIGN KEY(" + COLUMN_FK_RECORDELEMENT + ") REFERENCES " + TABLE_RECORDELEMENT + "(" + COLUMN_ID + ")" +
            ");";

    private static final String DATABASE_CREATE_TRANSCRIPT = "create table if not exists " +
            TABLE_TRANSCRIPT + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_TEXT + " text, " +
            COLUMN_CREATIONDATE + " integer not null, " +
            COLUMN_UPDATEDATE + " integer);";
    private static final String DATABASE_CREATE_RECORDELEMENT = "create table if not exists " +
            TABLE_RECORDELEMENT + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_NAME + " text not null, " +
            COLUMN_CREATIONDATE + " integer not null, " +
            COLUMN_UPDATEDATE + " integer," +
            COLUMN_POSITION + " integer not null," +
            COLUMN_FK_RECORD + " integer not null," +
            " FOREIGN KEY(" + COLUMN_FK_RECORD + ") REFERENCES " + TABLE_RECORD + "(" + COLUMN_ID + ") " +
            ");";
    private static final String DATABASE_CREATE_RECORD = "create table if not exists " +
            TABLE_RECORD + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_NAME + " text not null, " +
            COLUMN_CREATIONDATE + " integer not null, " +
            COLUMN_UPDATEDATE + " integer" + ");";


    public DbCreator(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_RECORD);
        db.execSQL(DATABASE_CREATE_RECORDELEMENT);
        db.execSQL(DATABASE_CREATE_TRANSCRIPT);
        db.execSQL(DATABASE_CREATE_SEGMENTS);
        Log.v(App.getTag(), "tables created");

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEGMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSCRIPT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDELEMENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORD);
        Log.v(App.getTag(), "tables deleted");
        onCreate(db);
    }
}
