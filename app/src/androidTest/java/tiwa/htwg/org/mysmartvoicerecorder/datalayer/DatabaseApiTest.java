package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import tiwa.htwg.org.mysmartvoicerecorder.gui.EmptyActivity;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.ITranscript;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.Transcript;

/**
 * Created by tim on 18.11.15.
 */
public class DatabaseApiTest extends ActivityInstrumentationTestCase2<EmptyActivity> {
    private DatabaseApi database;
    private EmptyActivity activity;
    private Context mContext = null;
    private final String NAME = "Party";
    private final int CREATIONDATE = 424242;
    private final double DURATION = 23.42;
    private final String AUDIOPATH = "asdf";
    private final int UPDATEDATE = 23455667;
    private ISegment testSegmentStore;

    public DatabaseApiTest(Class<EmptyActivity> activityClass) {
        super(activityClass);
    }
   public DatabaseApiTest(){
       super(EmptyActivity.class);
   }

    public void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
        mContext = activity;
        database = new DatabaseApi();
        database.initDatabase(mContext);
    }

    public void tearDown() throws Exception {
        database.closeDatabase();
    //    mContext.deleteDatabase("MySmartVoiceRecorder.db");

    }
    public void testDatabase() throws Exception {
        IRecord record = createRecord();
        database.saveRecord(record);
        String transcriptValue = database.getAllRecords().first().getRecordElements().first().getSegments().first().getTranscript().getText();
        if(!transcriptValue.contentEquals(transcriptValue)){
            fail();
        }
        transcriptValue = database.getAllRecords().last().getRecordElements().first().getSegments().first().getTranscript().getText();
        if(!transcriptValue.contentEquals(transcriptValue)){
            fail();
        }

      /*  saveSegmentsTest();
        if(!getAllSegmentsTest()){
            fail("Segment not in database");
        }
        deleteSegmentsTest();
        if(getAllSegmentsTest()){
            fail("Segment still there");
        }
        saveSegmentsTest();
        updateSegmentTest();
        if(getAllSegmentsTest()){
            fail("Segment update failed");
        }*/
    }
    public IRecord createRecord(){
        IRecord record = new Record();
        IRecordElement recordElement1 = new RecordElement();
        IRecordElement recordElement2 = new RecordElement();
        ITranscript transcript = new Transcript();
        transcript.setText("test");
        ISegment segment1 = new Segment();
        ISegment segment2 = new Segment();
        ISegment segment3 = new Segment();
        ISegment segment4 = new Segment();
        segment1.setAudioPath("/1");
        segment2.setAudioPath("/2");
        segment3.setAudioPath("/3");
        segment4.setAudioPath("/4");
        segment1.setDuration(1);
        segment2.setDuration(2);
        segment3.setDuration(3);
        segment4.setDuration(4);
        segment1.setPosition(2);
        segment2.setPosition(1);
        segment3.setPosition(1);
        segment4.setPosition(2);
        segment1.setTranscript(transcript);
        segment2.setTranscript(transcript);
        segment3.setTranscript(transcript);
        segment4.setTranscript(transcript);
        recordElement1.addSegment(segment1);
        recordElement1.addSegment(segment2);
        recordElement2.addSegment(segment3);
        recordElement2.addSegment(segment4);
        recordElement1.setPosition(1);
        recordElement2.setPosition(2);
        record.addRecordElement(recordElement1);
        record.addRecordElement(recordElement2);

        return record;
    }

  /*  public void saveSegmentsTest() throws Exception {

        TreeSet<ISegment> segments = new TreeSet<ISegment>();
        ISegment segment = new Segment();
        segment.setName(NAME);
        segment.setCreationDate(CREATIONDATE);
        segment.setTranscript(new Transcript());
        segment.setDuration(DURATION);
        segment.setAudioPath(AUDIOPATH);
        segment.setUpdateDate(UPDATEDATE);

        segments.add(segment);
        database.saveSegments(segments);

    }
    public boolean getAllSegmentsTest(){
        TreeSet<ISegment> segments = database.getAllSegments();
        for(ISegment seg : segments){
            if(seg.getName().contentEquals(NAME) && seg.getAudioPath().contentEquals(AUDIOPATH)&& seg.getDuration() == DURATION){
                testSegmentStore = seg;
                return true;
            }
        }
        return false;
    }
    public void deleteSegmentsTest(){
        TreeSet<ISegment> segments = new TreeSet<ISegment>();
        segments.add(testSegmentStore);
        database.deleteSegments(segments);
    }
    public void updateSegmentTest(){
        ISegment segment = testSegmentStore;
        segment.setName("anderer Name");
        segment.setId(2);
        database.updateSegment(segment);
    }*/
}