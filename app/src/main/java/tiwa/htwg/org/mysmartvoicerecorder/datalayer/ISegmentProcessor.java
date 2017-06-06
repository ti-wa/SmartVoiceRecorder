package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import android.content.Context;

public interface ISegmentProcessor {
    void newRecord(IDatabaseApi databaseApi, int segmentCount, final Context context);

    void newRecordElementAtEnd(final IDatabaseApi databaseApi, final int segmentCount, final Context context, final ActiveRecordDTO activeRecordDTO);

    void appendRecordElementOnRecordElement(IDatabaseApi databaseApi, int segmentCount, final Context context, final ActiveRecordDTO activeRecordDTO);

    void appendRecordElementOnSegment(IDatabaseApi databaseApi, int segmentCount, final Context context, final ActiveRecordDTO activeRecordDTO);

}
