package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import android.content.Context;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.ITranscript;

import java.util.TreeSet;

public interface IDatabaseApi {
    void closeDatabase();

    void initDatabase(Context context);

    int getNextRecordId();

    int getNextRecordElementId();

    void saveRecord(IRecord record);

    TreeSet<IRecord> getAllRecords();

    void saveRecordElement(IRecordElement recordElement);

    void updateRecordElements(TreeSet<IRecordElement> iRecordElements);

    //old segments to new recordElement
    void saveRecordElementWithOldSegments(IRecordElement recordElement);

    void updateRecord(IRecord record);

    void updateTranscript(ITranscript transcript);

    void deleteSegments(TreeSet<ISegment> segments);

    void deleteRecordElements(TreeSet<IRecordElement> recordElements);

    void deleteRecord(IRecord record);

}
