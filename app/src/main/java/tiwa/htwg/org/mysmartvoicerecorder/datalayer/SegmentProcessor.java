package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.*;
import android.util.Log;
import android.widget.Toast;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;
import tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.ITranscriptApi;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.TranscriptApi;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

public class SegmentProcessor implements ISegmentProcessor {
    private final String appFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "voiceRecorder" + File.separator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Messenger mMainActivity = null;

    public SegmentProcessor(Messenger mMainActivity) {
        this.mMainActivity = mMainActivity;
    }

    @Override
    public void newRecord(final IDatabaseApi databaseApi, final int segmentCount, final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(App.getTag(), "start Segment Processor for new Record");

                int recordId = databaseApi.getNextRecordId();
                int elementRecordId = databaseApi.getNextRecordElementId();
                renameLastRecord(recordId, elementRecordId, segmentCount);
                TreeSet<ISegment> segments = getSegmentForEachFile(recordId, elementRecordId, segmentCount);
                ITranscriptApi transcriptApi = new TranscriptApi(context, segments.size());
                IRecord record = new Record();
                IRecordElement recordElement = new RecordElement();
                for (ISegment segment : segments) {
                    segment.setTranscript(transcriptApi.createTranscript(segment.getAudioPath()));
                    recordElement.addSegment(segment);
                }
                recordElement.setPosition(1);
                record.addRecordElement(recordElement);
                databaseApi.saveRecord(record);
                showToastRecordSaved(context);
            }
        }).start();
    }

    @Override
    public void appendRecordElementOnSegment(final IDatabaseApi databaseApi, final int segmentCount, final Context context, final ActiveRecordDTO activeRecordDTO) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(App.getTag(), "start Segment Processor AppenRecordElement");
                TreeSet<IRecordElement> recordElementsToUpdatePosition = new TreeSet<>();
                TreeSet<ISegment> segmentsToRenameAndUpdate = new TreeSet<>();
                boolean foundRecordElement = false;
                boolean foundSegment = false;
                int positionOfOldRecordElement = -1;
                //find record element for insertion
                for (IRecordElement iRecordElement : activeRecordDTO.getRecord().getRecordElements()) {
                    if (foundRecordElement) {
                        recordElementsToUpdatePosition.add(iRecordElement);
                    }
                    if (iRecordElement.getId() == activeRecordDTO.getRecordElementId()) {
                        foundRecordElement = true;
                        positionOfOldRecordElement = iRecordElement.getPosition();
                        TreeSet<ISegment> segments = iRecordElement.getSegments();
                        //find segment for insertion
                        for (ISegment segment : segments) {
                            if (foundSegment) {
                                segmentsToRenameAndUpdate.add(segment);
                            }
                            if (activeRecordDTO.getSegmentId() == segment.getId()) {
                                foundSegment = true;
                            }
                        }
                    }
                }
                int newIdForSplittedElementRecord = databaseApi.getNextRecordElementId();
                IRecordElement recordElement = new RecordElement();
                recordElement.setRecordFk(activeRecordDTO.getRecord().getId());
                recordElement.setPosition(positionOfOldRecordElement + 4);
                segmentsToRenameAndUpdate = renameSegments(activeRecordDTO.getRecord().getId(), newIdForSplittedElementRecord, segmentsToRenameAndUpdate);
                for (ISegment segment : segmentsToRenameAndUpdate) {
                    recordElement.addSegment(segment);
                }
                //write the rear part of  splitted recordElement to database
                databaseApi.saveRecordElementWithOldSegments(recordElement);
                int i = positionOfOldRecordElement + 6;
                for (IRecordElement iRecordElement : recordElementsToUpdatePosition) {
                    iRecordElement.setPosition(i);
                    i = i + 2;
                }
                //update all record elements after the splitted one
                databaseApi.updateRecordElements(recordElementsToUpdatePosition);
                //insert the record element on correct position
                makeRecordElement(databaseApi, segmentCount, context, activeRecordDTO, positionOfOldRecordElement + 2);
            }
        }).start();
    }

    //wenn record element am ende des records
    @Override
    public void newRecordElementAtEnd(final IDatabaseApi databaseApi, final int segmentCount, final Context context, final ActiveRecordDTO activeRecordDTO) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(App.getTag(), "start Segment Processor for newRecordElementAtEnd");
                makeRecordElement(databaseApi, segmentCount, context, activeRecordDTO, -1);
            }
        }).start();
    }

    //wenn das neue record element nicht am des records ist
    @Override
    public void appendRecordElementOnRecordElement(final IDatabaseApi databaseApi, final int segmentCount, final Context context, final ActiveRecordDTO activeRecordDTO) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(App.getTag(), "start Segment Processor for appendRecordElementOnRecordElement");
                TreeSet<IRecordElement> recordElementsToUpdatePosition = new TreeSet<>();
                boolean found = false;
                int position = -1;
                for (IRecordElement iRecordElement : activeRecordDTO.getRecord().getRecordElements()) {
                    if (found) {
                        recordElementsToUpdatePosition.add(iRecordElement);
                    }
                    if (iRecordElement.getId() == activeRecordDTO.getRecordElementId()) {
                        found = true;
                        position = iRecordElement.getPosition() + 2;
                    }
                }
                for (IRecordElement iRecordElement : recordElementsToUpdatePosition) {
                    iRecordElement.setPosition(iRecordElement.getPosition() + 2);
                }
                databaseApi.updateRecordElements(recordElementsToUpdatePosition);
                makeRecordElement(databaseApi, segmentCount, context, activeRecordDTO, position);
            }
        }).start();
    }

    //position -1 if position should be at the end
    private void makeRecordElement(final IDatabaseApi databaseApi, final int segmentCount, final Context context, final ActiveRecordDTO activeRecordDTO, final int position) {
        Log.d(App.getTag(), "make Record Element");

        int recordId = activeRecordDTO.getRecord().getId();
        int elementRecordId = databaseApi.getNextRecordElementId();
        renameLastRecord(recordId, elementRecordId, segmentCount);
        TreeSet<ISegment> segments = getSegmentForEachFile(recordId, elementRecordId, segmentCount);
        ITranscriptApi transcriptApi = new TranscriptApi(context, segments.size());
        IRecordElement recordElement = new RecordElement();
        int i = 1;
        for (ISegment segment : segments) {
            segment.setTranscript(transcriptApi.createTranscript(segment.getAudioPath()));
            recordElement.addSegment(segment);
            i++;
        }
        if (position == -1) {
            recordElement.setPosition(activeRecordDTO.getRecord().getRecordElements().last().getPosition() + 2);
        } else {
            recordElement.setPosition(position);
        }
        recordElement.setRecordFk(recordId);
        databaseApi.saveRecordElement(recordElement);
        showToastRecordSaved(context);
    }

    //rename every new record file,  named lastRecord+number to "recordId/recordElementid_SegmentPosition"
    private void renameLastRecord(int recordId, int elementRecordId, int segmentCount) {
        Log.v(App.getTag(), "Segments to Process " + segmentCount);
        SupportMethods.createFolderinAppFolder(String.valueOf(recordId));
        for (int i = 1; i < segmentCount; i++) {
            File file = new File(appFolder + "lastRecord" + i);
            Log.v(App.getTag(), "copy file to " + appFolder + recordId + File.separator + elementRecordId + "_" + i);
            File file2 = new File(appFolder + recordId + File.separator + elementRecordId + "_" + i);
            file.renameTo(file2);
        }
    }

    private TreeSet<ISegment> getSegmentForEachFile(int recordId, int recordElementId, int
            segmentCount) {
        TreeSet<ISegment> segments = new TreeSet<>();
        for (int i = 1; i < segmentCount; i++) {
            MediaPlayer mPlayer;
            try {
                ISegment segment = new Segment();
                segment.setPosition(i);
                String filePath = appFolder + recordId + File.separator + recordElementId + "_" + i;
                Log.v(App.getTag(), "file path" + filePath);
                mPlayer = new MediaPlayer();
                mPlayer.setDataSource(filePath);
                mPlayer.prepare();
                int duration = mPlayer.getDuration();
                if (duration < 50) {
                    Log.d(App.getTag(), "delete to short file " + filePath);
                    File file = new File(filePath);
                    file.delete();
                    continue;
                }
                mPlayer.release();
                segment.setAudioPath(filePath);
                segment.setDuration(duration);
                segments.add(segment);
            } catch (IOException e) {
                break;
            }
        }
        return segments;
    }

    private TreeSet<ISegment> renameSegments(int recordId, int recordElementId, TreeSet<ISegment> iSegments) {
        int i = 1;
        for (ISegment iSegment : iSegments) {
            File file = new File(iSegment.getAudioPath());
            String newFilePath = appFolder + recordId + File.separator + recordElementId + "_" + i;
            File file2 = new File(newFilePath);
            file.renameTo(file2);
            Log.v(App.getTag(), "rename: " + file.getPath() + " to " + file2.getPath());
            iSegment.setPosition(i);
            iSegment.setAudioPath(newFilePath);
            i++;
        }
        return iSegments;
    }

    private void reloadhUi() {
        Message message = Message.obtain();
        message.what = App.RELOADUI;
        try {
            mMainActivity.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void showToastRecordSaved(final Context context) {
        reloadhUi();
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Record saved", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
