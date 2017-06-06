package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;
import tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.ITranscript;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.Transcript;

import java.io.File;
import java.util.TreeSet;

import static tiwa.htwg.org.mysmartvoicerecorder.datalayer.DbCreator.*;

public class DatabaseApi implements IDatabaseApi {
    private SQLiteDatabase database;
    private DbCreator dbcreator;

    public DatabaseApi() {
    }

    private void beginTransaction() {
        database.beginTransaction();
    }

    private void endTransaction() {
        database.setTransactionSuccessful();
        database.endTransaction();
    }


    @Override
    public void initDatabase(Context context) {
        Log.v(App.getTag(), "init Database");
        dbcreator = new DbCreator(context);
        database = openDatabase();
        if (database == null) {
            Log.e(App.getTag(), "Database is null");
        } else {
            Log.v(App.getTag(), "database connection successful");
        }
    }


    @Override
    public int getNextRecordId() {
        beginTransaction();
        int lastId;
        try {
            String query = "select seq from sqlite_sequence where name='" + TABLE_RECORD + "'";
            Cursor cursor = database.rawQuery(query, null);
            cursor.moveToFirst();
            lastId = cursor.getInt(0);
            cursor.close();
        } catch (CursorIndexOutOfBoundsException e) {
            Log.w(App.getTag(), "No return value for NextRecordId");
            lastId = 0;
        }
        endTransaction();
        return lastId + 1;
    }


    @Override
    public int getNextRecordElementId() {
        beginTransaction();
        int lastId;
        try {
            String query = "select seq from sqlite_sequence where name='" + TABLE_RECORDELEMENT + "'";
            Cursor cursor = database.rawQuery(query, null);
            cursor.moveToFirst();
            lastId = cursor.getInt(0);
            cursor.close();
        } catch (CursorIndexOutOfBoundsException e) {
            Log.w(App.getTag(), "No return value for NextRecordElementId");
            lastId = 0;
        }
        endTransaction();
        return lastId + 1;
    }

    private SQLiteDatabase openDatabase() {
        return dbcreator.getWritableDatabase();
    }


    @Override
    public void closeDatabase() {
        dbcreator.close();
    }

    @Override
    public void updateRecord(IRecord record) {
        beginTransaction();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, record.getName());
        values.put(COLUMN_UPDATEDATE, SupportMethods.getUnixTime());
        String selection = COLUMN_ID + " =?";
        String[] selectionArgs = {String.valueOf(record.getId())};
        int count = database.update(
                TABLE_RECORD,
                values,
                selection,
                selectionArgs);
        if (count == 0) {
            Log.e(App.getTag(), "Nothing to update");
        } else {
            Log.v(App.getTag(), "Record Updatecount: " + count);
        }
        endTransaction();

    }

    @Override
    public void deleteRecordElements(TreeSet<IRecordElement> recordElements) {
        for (IRecordElement recordElement : recordElements) {
            deleteSegments(recordElement.getSegments());
            Log.v(App.getTag(), "Id of RecordElement to delete " + recordElement.getId());
            String selection = COLUMN_ID + " =?";
            String[] selectionArgs = {String.valueOf(recordElement.getId())};
            database.delete(TABLE_RECORDELEMENT, selection, selectionArgs);
            Log.v(App.getTag(), "RecordElement deleted ");
        }
    }


    @Override
    public void deleteRecord(IRecord record) {
        deleteRecordElements(record.getRecordElements());
        Log.v(App.getTag(), "Id of Record to delete " + record.getId());
        String selection = COLUMN_ID + " =?";
        String[] selectionArgs = {String.valueOf(record.getId())};
        database.delete(TABLE_RECORD, selection, selectionArgs);
        Log.v(App.getTag(), "Record deleted");
    }


    @Override
    public void deleteSegments(TreeSet<ISegment> segments) {
        for (ISegment segment : segments) {
            deleteTranscript(segment.getTranscript().getId());
            deleteFile(segment.getAudioPath());
            Log.v(App.getTag(), "Id of Segment to delete " + segment.getId());
            String selection = COLUMN_ID + " =?";
            String[] selectionArgs = {String.valueOf(segment.getId())};
            database.delete(TABLE_SEGMENTS, selection, selectionArgs);
            Log.v(App.getTag(), "Segment deleted ");
        }
    }

    private void deleteTranscript(int id) {
        Log.v(App.getTag(), "Id of Transcript to delete " + id);
        String selection = COLUMN_ID + " =?";
        String[] selectionArgs = {String.valueOf(id)};
        database.delete(TABLE_TRANSCRIPT, selection, selectionArgs);
        Log.v(App.getTag(), "Transcript deleted ");
    }

    private void deleteFile(String path) {
        Log.v(App.getTag(), "delete file: " + path);
        File file = new File(path);
        boolean result = file.delete();
        if (!result) {
            Log.e(App.getTag(), "Deletion failed");
        }
    }

    @Override
    public void saveRecordElement(IRecordElement recordElement) {
        beginTransaction();
        int recordElementKey = recordElementToDatabase(recordElement);
        TreeSet<ISegment> segments = recordElement.getSegments();
        Log.v(App.getTag(), "segment count: " + segments.size());
        for (ISegment segment : segments) {
            segment.setFkRecordElement(recordElementKey);
            ITranscript transcript = segment.getTranscript();
            int transcriptKey = transcriptToDatabase(transcript);
            transcript.setId(transcriptKey);
            segment.setTranscript(transcript);
            segmentToDatabse(segment);
        }
        endTransaction();
    }

    @Override
    public void updateTranscript(ITranscript transcript) {
        beginTransaction();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, transcript.getText());
        values.put(COLUMN_UPDATEDATE, SupportMethods.getUnixTime());
        String selection = COLUMN_ID + " =?";
        String[] selectionArgs = {String.valueOf(transcript.getId())};
        int count = database.update(
                TABLE_TRANSCRIPT,
                values,
                selection,
                selectionArgs);
        if (count == 0) {
            Log.e(App.getTag(), "Nothing to update");
        } else {
            Log.v(App.getTag(), "Updatecount: " + count);
        }
        endTransaction();
    }

    //use if segments get an recordElement wich is not in database
    @Override
    public void saveRecordElementWithOldSegments(IRecordElement recordElement) {
        beginTransaction();
        int recordElementKey = recordElementToDatabase(recordElement);
        TreeSet<ISegment> segments = recordElement.getSegments();
        Log.v(App.getTag(), "segment count: " + segments.size());
        for (ISegment segment : segments) {
            segment.setFkRecordElement(recordElementKey);
            updateSegmentPositionRecordElementFKPath(segment);
        }
        endTransaction();
    }

    //update path of audiofile
    private void updateSegmentPositionRecordElementFKPath(ISegment segment) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_POSITION, segment.getPosition());
        values.put(COLUMN_FK_RECORDELEMENT, segment.getFkRecordElement());
        values.put(COLUMN_AUDIODATAPATH, segment.getAudioPath());
        values.put(COLUMN_UPDATEDATE, SupportMethods.getUnixTime());
        String selection = COLUMN_ID + " =?";
        String[] selectionArgs = {String.valueOf(segment.getId())};
        int count = database.update(
                TABLE_SEGMENTS,
                values,
                selection,
                selectionArgs);
        if (count == 0) {
            Log.e(App.getTag(), "Nothing to update");
        } else {
            Log.v(App.getTag(), "Updatecount: " + count);
        }
    }

    //update name and position
    @Override
    public void updateRecordElements(TreeSet<IRecordElement> iRecordElements) {
        beginTransaction();
        Log.v(App.getTag(), "Update RecordElements Position for Itemcount: " + iRecordElements.size());
        for (IRecordElement iRecordElement : iRecordElements) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_POSITION, iRecordElement.getPosition());
            values.put(COLUMN_NAME, iRecordElement.getName());
            values.put(COLUMN_UPDATEDATE, SupportMethods.getUnixTime());
            String selection = COLUMN_ID + " =?";
            String[] selectionArgs = {String.valueOf(iRecordElement.getId())};
            int count = database.update(
                    TABLE_RECORDELEMENT,
                    values,
                    selection,
                    selectionArgs);
            if (count == 0) {
                Log.e(App.getTag(), "Nothing to update");
            } else {
                Log.v(App.getTag(), "Updatecount: " + count);
            }
        }
        endTransaction();
    }

    @Override
    public void saveRecord(IRecord record) {
        beginTransaction();
        //new record
        int recordKey = recordToDatabase();
        TreeSet<IRecordElement> recordElements = record.getRecordElements();
        for (IRecordElement recordElement : recordElements) {
            recordElement.setRecordFk(recordKey);
            //new recordElement
            int recordElementKey = recordElementToDatabase(recordElement);
            TreeSet<ISegment> segments = recordElement.getSegments();
            Log.v(App.getTag(), "segment count: " + segments.size());
            for (ISegment segment : segments) {
                segment.setFkRecordElement(recordElementKey);
                ITranscript transcript = segment.getTranscript();
                //new transkript
                int transcriptKey = transcriptToDatabase(transcript);
                transcript.setId(transcriptKey);
                segment.setTranscript(transcript);
                //new segment
                segmentToDatabse(segment);
            }
        }
        endTransaction();
    }

    @Override
    public TreeSet<IRecord> getAllRecords() {
        beginTransaction();
        TreeSet<IRecord> records = recordReadAllFromDatabase();
        for (IRecord record : records) {
            appendRecordElementsandSegmentsToRecord(record);
        }
        endTransaction();
        return records;
    }

    private IRecord appendRecordElementsandSegmentsToRecord(IRecord record) {
        Log.v(App.getTag(), "prepare SQL-Query to get all recordElements for specific record");
        String[] projection = {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_CREATIONDATE,
                COLUMN_UPDATEDATE,
                COLUMN_POSITION,
        };
        Log.v(App.getTag(), "execute SQL-Query to get all recordElements for specific record");
        String selection = COLUMN_FK_RECORD + " =?";
        String[] selectionArgs = {String.valueOf(record.getId())};
        Cursor cursor = database.query(
                TABLE_RECORDELEMENT,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        Log.v(App.getTag(), "Found recordElements count = " + cursor.getCount());
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            IRecordElement recordElement = cursorToRecordElement(cursor);
            recordElement = appendSegmentsToRecordElement(recordElement);
            record.addRecordElement(recordElement);
            cursor.moveToNext();
        }
        return record;
    }

    private IRecordElement appendSegmentsToRecordElement(IRecordElement recordElement) {
        Log.v(App.getTag(), "prepare SQL-Query to get all segments for specific recordElement");
        String[] projection = {
                COLUMN_ID,
                COLUMN_CREATIONDATE,
                COLUMN_UPDATEDATE,
                COLUMN_POSITION,
                COLUMN_FK_TRANSCRIPT,
                COLUMN_FK_RECORDELEMENT,
                COLUMN_DURATION,
                COLUMN_AUDIODATAPATH
        };
        Log.v(App.getTag(), "execute SQL-Query to get all segments for specific recordElement");
        String selection = COLUMN_FK_RECORDELEMENT + " =?";
        String[] selectionArgs = {String.valueOf(recordElement.getId())};
        Cursor cursor = database.query(
                TABLE_SEGMENTS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        Log.v(App.getTag(), "Found elementRecords count = " + cursor.getCount());
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ISegment segment = cursorToSegment(cursor);
            segment.setFkRecordElement(recordElement.getId());
            recordElement.addSegment(segment);
            cursor.moveToNext();
        }
        return recordElement;
    }

    private ISegment cursorToSegment(Cursor cursor) {
        ISegment segment = new Segment();
        segment.setId(cursor.getInt(0));
        segment.setCreationDate(cursor.getInt(1));
        segment.setUpdateDate(cursor.getInt(2));
        segment.setPosition(cursor.getInt(3));
        segment.setTranscript(getTranscriptbyId(cursor.getInt(4)));
        segment.setDuration(cursor.getInt(6));
        segment.setAudioPath(cursor.getString(7));
        return segment;
    }

    private ITranscript getTranscriptbyId(int id) {
        ITranscript transcript = new Transcript();
        Log.v(App.getTag(), "prepare SQL-Query to get transkript for as segment with id" + id);
        String[] projection = {
                COLUMN_ID,
                COLUMN_CREATIONDATE,
                COLUMN_UPDATEDATE,
                COLUMN_TEXT
        };
        Log.v(App.getTag(), "execute SQL-Query to get transkript");
        String selection = COLUMN_ID + " =?";
        String[] selectionArgs = {String.valueOf(id)};
        Cursor cursor = database.query(
                TABLE_TRANSCRIPT,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        Log.v(App.getTag(), "Found Transcript count = " + cursor.getCount());
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            transcript = cursorToTranscript(cursor);
            cursor.moveToNext();
        }
        return transcript;
    }

    private ITranscript cursorToTranscript(Cursor cursor) {
        ITranscript transcript = new Transcript();
        transcript.setId(cursor.getInt(0));
        transcript.setCreationDate(cursor.getInt(1));
        transcript.setUpdateDate(cursor.getInt(2));
        transcript.setText(cursor.getString(3));
        return transcript;
    }

    private IRecordElement cursorToRecordElement(Cursor cursor) {
        IRecordElement recordElement = new RecordElement();
        recordElement.setId(cursor.getInt(0));
        recordElement.setName(cursor.getString(1));
        recordElement.setCreationDate(cursor.getInt(2));
        recordElement.setUpdateDate(cursor.getInt(3));
        recordElement.setPosition(cursor.getInt(4));
        return recordElement;

    }

    private TreeSet<IRecord> recordReadAllFromDatabase() {
        Log.v(App.getTag(), "prepare SQL-Query to get all Records");
        String[] projection = {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_CREATIONDATE,
                COLUMN_UPDATEDATE,
        };
        //String sortOrder = "DESC ";
        Log.v(App.getTag(), "execute SQL-Query to get all Records");
        Cursor cursor = database.query(
                TABLE_RECORD,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        Log.v(App.getTag(), "Found records count = " + cursor.getCount());
        TreeSet<IRecord> records = new TreeSet<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            records.add(cursorToRecord(cursor));
            cursor.moveToNext();
        }
        return records;
    }

    private IRecord cursorToRecord(Cursor cursor) {
        IRecord record = new Record();
        record.setId(cursor.getInt(0));
        record.setName(cursor.getString(1));
        record.setCreationDate(cursor.getInt(2));
        record.setUpdateDate(cursor.getInt(3));
        return record;
    }

    private int recordToDatabase() {
        Log.v(App.getTag(), "prepare write record to database");
        ContentValues values = new ContentValues();
        long newRowId;
        int unixTime = SupportMethods.getUnixTime();
        values.put(COLUMN_NAME, SupportMethods.getTimeFormated());
        values.put(COLUMN_CREATIONDATE, unixTime);
        values.put(COLUMN_UPDATEDATE, unixTime);
        Log.v(App.getTag(), "execute record query");
        newRowId = database.insert(TABLE_RECORD, null, values);
        if (newRowId == -1) {
            Log.e(App.getTag(), "Error writing record in database");
        } else {
            Log.v(App.getTag(), "saved record in row id=" + newRowId);
        }
        return (int) newRowId;
    }

    private int recordElementToDatabase(IRecordElement recordElement) {
        Log.v(App.getTag(), "prepare write recordElement to database");
        ContentValues values = new ContentValues();
        long newRowId;
        int unixTime = SupportMethods.getUnixTime();
        values.put(COLUMN_NAME, SupportMethods.getTimeFormated());
        values.put(COLUMN_CREATIONDATE, unixTime);
        values.put(COLUMN_UPDATEDATE, unixTime);
        values.put(COLUMN_FK_RECORD, recordElement.getRecordFk());
        values.put(COLUMN_POSITION, recordElement.getPosition());
        Log.v(App.getTag(), "execute recordElement query");
        newRowId = database.insert(TABLE_RECORDELEMENT, null, values);
        if (newRowId == -1) {
            Log.e(App.getTag(), "Error writing recordElement in database");
        } else {
            Log.v(App.getTag(), "saved recordElement in row id=" + newRowId);
        }
        return (int) newRowId;
    }

    private int transcriptToDatabase(ITranscript transcript) {
        Log.v(App.getTag(), "prepare write transkript to database");
        ContentValues values = new ContentValues();
        long newRowId;
        int unixTime = SupportMethods.getUnixTime();
        values.put(COLUMN_CREATIONDATE, unixTime);
        values.put(COLUMN_UPDATEDATE, unixTime);
        values.put(COLUMN_TEXT, transcript.getText());
        Log.v(App.getTag(), "execute transkript query");
        newRowId = database.insert(TABLE_TRANSCRIPT, null, values);
        if (newRowId == -1) {
            Log.e(App.getTag(), "Error writing transkript in database");
        } else {
            Log.v(App.getTag(), "saved transkript in row id=" + newRowId);
        }
        return (int) newRowId;
    }

    private void segmentToDatabse(ISegment segment) {
        Log.v(App.getTag(), "prepare write segment to database");
        ContentValues values = new ContentValues();
        long newRowId;
        int unixTime = SupportMethods.getUnixTime();
        values.put(COLUMN_CREATIONDATE, unixTime);
        values.put(COLUMN_UPDATEDATE, unixTime);
        values.put(COLUMN_FK_TRANSCRIPT, segment.getTranscript().getId());
        values.put(COLUMN_AUDIODATAPATH, segment.getAudioPath());
        values.put(COLUMN_DURATION, segment.getDuration());
        values.put(COLUMN_FK_RECORDELEMENT, segment.getFkRecordElement());
        values.put(COLUMN_POSITION, segment.getPosition());
        Log.v(App.getTag(), "execute segment query");
        newRowId = database.insert(TABLE_SEGMENTS, null, values);
        if (newRowId == -1) {
            Log.e(App.getTag(), "Error writing segment in database");
        } else {
            Log.v(App.getTag(), "saved segment in row id=" + newRowId);
        }
    }
}



