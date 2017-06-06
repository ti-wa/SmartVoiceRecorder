package tiwa.htwg.org.mysmartvoicerecorder.gui;

import android.app.FragmentManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import tiwa.htwg.org.mysmartvoicerecorder.R;
import tiwa.htwg.org.mysmartvoicerecorder.audio.PlayerService;
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.*;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;
import tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods;

import java.util.HashSet;
import java.util.TreeSet;

import static tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods.millisecondsToHMS;

public class MainActivity extends AppCompatActivity {

    private final HashSet<TextView> segmentsTvOnScreen = new HashSet<>();
    private final ActiveRecordDTO activeRecordDTO = new ActiveRecordDTO();
    private final FragmentManager manager = getFragmentManager();
    private TreeSet<IRecord> records = new TreeSet<>();
    private Messenger mService = null;
    //record Button
    private final View.OnClickListener recordListner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(App.getTag(), "pressed record");
            sendRecordMessage();
        }
    };
    //play Button
    private final View.OnClickListener playListner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(App.getTag(), "pressed play");
            sendPlayMessage();
        }
    };
    //stop Button
    private final View.OnClickListener stopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(App.getTag(), "pressed stop");
            sendMessage(App.STOP);
        }
    };
    //Segment TextView Listener
    private final View.OnClickListener textViewsSegmentListner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(App.getTag(), "pressed Segment: " + v.getId());
            ISegment segment = getSegmentById(v.getId());
            if (segment == null) {
                Log.e(App.getTag(), "clicked on not existing segment");
                return;
            }
            activeRecordDTO.setRecordElementId(segment.getFkRecordElement());
            activeRecordDTO.setSegmentId(v.getId());
            sendPlayMessage();
        }
    };
    private Messenger mMainActivity = null;
    private boolean mBound;
    //service connection
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mBound = false;
        }
    };
    private boolean recording;
    private boolean playing;
    private Button mRecordButton = null;
    private Button mPlayButton = null;
    private Button mStopButton = null;
    private Button mFbButton = null;
    private Button mFfButton = null;
    private TextView durationTextView = null;
    private TextView audioProgressTextView = null;
    private SeekBar audioProgressBar = null;
    private IDatabaseApi database;

    //Segment TextView LongClickListener
    private final View.OnLongClickListener textViewsSegmentLongListner = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Log.i(App.getTag(), "pressed Long Segment: " + v.getId());
            if (playing || recording) {
                Log.w(App.getTag(), "Cancel LongClick because of playing or recording");
                return false;
            }
            ISegment segment = getSegmentById(v.getId());
            if (segment == null) {
                Log.e(App.getTag(), "clicked on not existing segment");
                return false;
            }
            activeRecordDTO.setRecordElementId(segment.getFkRecordElement());
            activeRecordDTO.setSegmentId(v.getId());
            SegmentClickDialog segmentClickDialog = new SegmentClickDialog();
            segmentClickDialog.addData(segment.getTranscript().getText(), activeRecordDTO, mService, segment, database, mMainActivity);
            segmentClickDialog.show(manager, "segment");
            return true;
        }
    };
    //Segment TextView LongClickListener
    private final View.OnLongClickListener texViewsRecordElementLongListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Log.i(App.getTag(), "pressed Long RecordElement: " + v.getId());
            if (playing || recording) {
                Log.w(App.getTag(), "Cancel LongClick because of playing or recording");
                return false;
            }
            IRecordElement recordElement = getRecordElementById(v.getId());
            if (recordElement == null) {
                Log.e(App.getTag(), "clicked on not existing recordElement");
                return false;
            }
            RecordElementClickDialog recordClickDialog = new RecordElementClickDialog();
            activeRecordDTO.setRecordElementId(v.getId());
            activeRecordDTO.setSegmentId(-1);
            recordClickDialog.addData(recordElement.getName(), recordElement, database, mMainActivity, mService, activeRecordDTO);
            recordClickDialog.show(manager, "recordElement");
            return true;
        }
    };
    //Record TextView LongClickListener
    private final View.OnLongClickListener texViewsRecordLongListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Log.i(App.getTag(), "pressed Long Record: " + v.getId());
            if (playing || recording) {
                Log.w(App.getTag(), "Cancel LongClick because of playing or recording");
                return false;
            }
            IRecord record = getRecordById(v.getId());
            if (record == null) {
                Log.e(App.getTag(), "clicked on not existing record");
                return false;
            }
            RecordClickDialog recordClickDialog = new RecordClickDialog();
            recordClickDialog.addData(record.getName(), record, database, mMainActivity);
            recordClickDialog.show(manager, "record");
            return true;
        }
    };
    private ScrollView scrollView;
    private LinearLayout scrollViewLayout;
    private ActiveRecordDTO playingRecordDTO = new ActiveRecordDTO();
    //back Button
    private final View.OnClickListener fbListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(App.getTag(), "pressed FastBackward");
            playNextSegment(false);
        }
    };
    //forward Button
    private final View.OnClickListener ffListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(App.getTag(), "pressed FastForward");
            playNextSegment(true);
        }
    };
    private boolean toHomeScreen = false;
    private SharedPreferences sharedPref;
    private TextView scrollToTextView = null;
    //RecordElements TextView Listener
    private final View.OnClickListener textViewsRecordElementsListner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(App.getTag(), "pressed RecordElement: " + v.getId());
            activeRecordDTO.setRecordElementId(v.getId());
            activeRecordDTO.setSegmentId(-1);
            showSegmentsOnScreen(v.getId());
        }
    };
    //Record TextView Listener
    private final View.OnClickListener textViewsRecordsListner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(App.getTag(), "pressed Record: " + v.getId());
            showOpenRecordWithRecordElementsOnScreen(v.getId());
            activeRecordDTO.setRecordElementId(-1);
            activeRecordDTO.setSegmentId(-1);
        }
    };
    //screen Text Record BACK
    private final View.OnClickListener textViewsRecordsListnerBack = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(App.getTag(), "pressed on open Record: " + v.getId());
            showRecordsOnScreen();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(App.getTag(), "OnCreate Main Activity");
        playing = false;
        recording = false;
        setContentView(R.layout.activity_main);
        database = new DatabaseApi();
        database.initDatabase(this);
        SupportMethods.createAppFolder();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mRecordButton = (Button) findViewById(R.id.record_button);
        mPlayButton = (Button) findViewById(R.id.play_button);
        mStopButton = (Button) findViewById(R.id.stop_button);
        mFbButton = (Button) findViewById(R.id.fb_button);
        mFfButton = (Button) findViewById(R.id.ff_button);
        durationTextView = (TextView) findViewById(R.id.duration);
        audioProgressTextView = (TextView) findViewById(R.id.time);
        audioProgressBar = (SeekBar) findViewById(R.id.seekBar);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollViewLayout = (LinearLayout) findViewById(R.id.scrollViewLayout);
        audioProgressBar.setClickable(false);
        mPlayButton.setOnClickListener(playListner);
        mRecordButton.setOnClickListener(recordListner);
        mStopButton.setOnClickListener(stopListener);
        mFbButton.setOnClickListener(fbListener);
        mFfButton.setOnClickListener(ffListener);
        setSupportActionBar(toolbar);
        setDefaultWidgetState();
        Handler messageHandler = new MessageHandler();
        Intent startPlayerService = new Intent(this, PlayerService.class);
        mMainActivity = new Messenger(messageHandler);
        startPlayerService.putExtra("MESSENGER", mMainActivity);
        startService(startPlayerService);
        showRecordsOnScreen();
        Log.d(App.getTag(), "finished on Create");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service
        bindService(new Intent(this, PlayerService.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    //Set Button-Design Clickable
    private void activateButton(Button button) {
        button.setClickable(true);
        button.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
    }

    //Set Button-Design unclickable
    private void deactivateButton(Button button) {
        button.setClickable(false);
        button.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
    }

    //Set default design state for each Widget
    private void setDefaultWidgetState() {
        audioProgressTextView.setText("00:00:00");
        audioProgressBar.setProgress(0);
        activateButton(mRecordButton);
        activateButton(mPlayButton);
        deactivateButton(mStopButton);
        deactivateButton(mFbButton);
        deactivateButton(mFfButton);
    }

    //set design state while playing for each widget
    private void setPlayingWidgetState() {
        activateButton(mStopButton);
        deactivateButton(mRecordButton);
        activateButton(mFfButton);
        activateButton(mFbButton);
        playing = true;
    }

    //set duration of playing media in bar and text widgets
    private void setDuration(int duration) {
        Log.v(App.getTag(), "set duration of audio: " + duration);
        audioProgressBar.setMax(duration);
        durationTextView.setText(millisecondsToHMS(duration));
    }

    //set play progress on widgets, every second called
    private void setAudioProgress(int time) {
        Log.v(App.getTag(), "set Progress to" + time);
        int max = audioProgressBar.getMax();
        if (max > time) {
            audioProgressBar.setProgress(time);
            audioProgressTextView.setText(millisecondsToHMS(time));
        } else {
            Log.w(App.getTag(), "playback time >= duration: playback should be finished but media is still running");
            audioProgressBar.setProgress(max);
            audioProgressTextView.setText(millisecondsToHMS(max));
        }
    }

    //second counter while recording
    private void setAudioProgressRecords(int time) {
        audioProgressTextView.setText(millisecondsToHMS(time));
    }

    //empty textviews on top for better layout
    private void putEmptyTextViewsOnTopOfTheScreen() {
        for (int i = 0; i < 3; i++) {
            TextView tv = new TextView(this);
            tv.setText("");
            scrollViewLayout.addView(tv);
            if (i == 2) {
                scrollToTextView = tv;
            }

        }
    }

    private TextView createRecordTextView(IRecord record) {
        TextView tv = new TextView(this);
        tv.setText(record.getName() + "\n");
        tv.setTextSize(30);
        tv.setId(record.getId());
        tv.setOnClickListener(textViewsRecordsListner);
        tv.setOnLongClickListener(texViewsRecordLongListener);
        return tv;
    }

    private TextView createRecordElementTextView(IRecordElement recordElement) {
        TextView tv = new TextView(this);
        tv.setText(recordElement.getName() + "\n");
        tv.setTextSize(15);
        tv.setId(recordElement.getId());
        tv.setPadding(50, 0, 0, 0);
        tv.setOnClickListener(textViewsRecordElementsListner);
        tv.setOnLongClickListener(texViewsRecordElementLongListener);
        return tv;
    }

    private TextView createSegmentTextView(ISegment segment, int color) {
        String text;
        if (sharedPref.getBoolean("dateInsteadOfTranscriptSwitch", false)) {
            text = SupportMethods.unixTimeToFormated(segment.getUpdateDate()) + "\t - \t" + millisecondsToHMS(segment.getDuration());
        } else {
            text = segment.getTranscript().getText();
        }
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.BOTTOM);
        tv.setPadding(15, 0, 15, 0);
        tv.setId(segment.getId());
        tv.setTextColor(color);
        tv.setOnLongClickListener(textViewsSegmentLongListner);
        segmentsTvOnScreen.add(tv);
        tv.setOnClickListener(textViewsSegmentListner);
        return tv;
    }

    //show list of records
    private void showRecordsOnScreen() {
        Log.v(App.getTag(), "show all records on screen");
        activeRecordDTO.setSegmentId(-1);
        activeRecordDTO.setRecordElementId(-1);
        activeRecordDTO.setRecord(null);
        toHomeScreen = true;
        scrollViewLayout.removeAllViews();
        putEmptyTextViewsOnTopOfTheScreen();
        records = database.getAllRecords();
        for (IRecord record : records) {
            TextView tv = createRecordTextView(record);
            scrollViewLayout.addView(tv);
        }
        focusOnView();
    }

    //show list of records, one open with his elements
    private void showOpenRecordWithRecordElementsOnScreen(int textViewId) {
        Log.v(App.getTag(), "show recordElement for record: " + textViewId);
        toHomeScreen = false;
        scrollViewLayout.removeAllViews();
        putEmptyTextViewsOnTopOfTheScreen();
        for (IRecord record : records) {
            TextView tv = createRecordTextView(record);
            scrollViewLayout.addView(tv);
            if (record.getId() == textViewId) {
                tv.setOnClickListener(textViewsRecordsListnerBack);
                activeRecordDTO.setRecord(record);
                TreeSet<IRecordElement> recordElements = record.getRecordElements();
                for (IRecordElement recordElement : recordElements) {
                    TextView tv2 = createRecordElementTextView(recordElement);
                    scrollViewLayout.addView(tv2);
                }
            }
        }
    }

    //show list of segments for one record
    private void showSegmentsOnScreen(int recordElementId) {
        Log.v(App.getTag(), "show segments on screen of recordElement: " + recordElementId);
        toHomeScreen = false;
        boolean firstSegment = false;
        scrollViewLayout.removeAllViews();
        segmentsTvOnScreen.clear();
        putEmptyTextViewsOnTopOfTheScreen();
        IRecord activeRecord = activeRecordDTO.getRecord();
        IRecord record = activeRecord;
        TreeSet<IRecordElement> recordElements = record.getRecordElements();
        for (IRecordElement recordElement : recordElements) {
            if (recordElement.getId() == recordElementId) {
                firstSegment = true;
            }
            TreeSet<ISegment> segments = recordElement.getSegments();
            int color = SupportMethods.getColor();
            for (ISegment segment : segments) {
                TextView tv = createSegmentTextView(segment, color);
                //first Segment is to focus scrollview on correct recordElement
                if (firstSegment) {
                    scrollToTextView = tv;
                    firstSegment = false;
                }
                scrollViewLayout.addView(tv);
            }
        }
        focusOnView();
    }

    private void markPlayingSegmentTextView(int segmentId) {
        Log.v(App.getTag(), "mark Segment: " + segmentId);
        if (segmentsTvOnScreen.isEmpty()) {
            Log.e(App.getTag(), "List of segments in screen is empty");
            return;
        }
        for (TextView textView : segmentsTvOnScreen) {
            textView.setTypeface(null, Typeface.NORMAL);
            if (textView.getId() == segmentId) {
                textView.setTypeface(null, Typeface.BOLD);
            }
        }
    }

    private ISegment getSegmentById(int id) {
        Log.v(App.getTag(), "find Segment with id" + id);
        IRecord record = activeRecordDTO.getRecord();
        TreeSet<IRecordElement> recordElements = record.getRecordElements();
        for (IRecordElement recordElement : recordElements) {
            TreeSet<ISegment> segments = recordElement.getSegments();
            for (ISegment segment : segments) {
                if (segment.getId() == id) {
                    return segment;
                }
            }
        }
        Log.e(App.getTag(), "Segment not found with id: " + id);
        return null;
    }

    private IRecordElement getRecordElementById(int id) {
        Log.v(App.getTag(), "find RecordElement with id" + id);
        IRecord record = activeRecordDTO.getRecord();
        TreeSet<IRecordElement> recordElements = record.getRecordElements();
        for (IRecordElement recordElement : recordElements) {
            if (recordElement.getId() == id) {
                return recordElement;
            }
        }
        Log.e(App.getTag(), "RecordElement not found with id: " + id);
        return null;
    }

    private IRecord getRecordById(int id) {
        Log.v(App.getTag(), "find Record with id" + id);
        for (IRecord record : records) {
            if (record.getId() == id) {
                return record;
            }
        }
        Log.e(App.getTag(), "Record not found with id: " + id);
        return null;
    }

    //send play message to play last or next segment: next segment = true , last segment = false
    private void playNextSegment(boolean next) {
        if (playingRecordDTO != null && playingRecordDTO.getRecord() != null && playingRecordDTO.getSegmentId() != -1 && playingRecordDTO.getRecordElementId() != -1) {
            Log.v(App.getTag(), "switch playing segment forward or backward " + next);
            IRecord record = playingRecordDTO.getRecord();
            ISegment lastSegment = null;
            ISegment lastLastSegment = null;
            IRecordElement lastLastRecordElement = null;
            IRecordElement lastRecordElement = null;
            TreeSet<IRecordElement> recordElements = record.getRecordElements();
            for (IRecordElement recordElement : recordElements) {
                TreeSet<ISegment> segments = recordElement.getSegments();
                for (ISegment segment : segments) {
                    if (segment.getId() == playingRecordDTO.getSegmentId()) {
                        if (!next) {
                            Log.e(App.getTag(), "Back");
                            activeRecordDTO.setRecord(record);
                            if (lastLastSegment != null) {
                                activeRecordDTO.setSegmentId(lastLastSegment.getId());
                                activeRecordDTO.setRecordElementId(lastLastRecordElement.getId());
                            } else {
                                if (lastSegment == null || lastRecordElement == null) {
                                    Log.e(App.getTag(), "prevent nullpointer");
                                    return;
                                }
                                activeRecordDTO.setSegmentId(lastSegment.getId());
                                activeRecordDTO.setRecordElementId(lastRecordElement.getId());
                            }
                        } else {
                            activeRecordDTO.setRecord(record);
                            activeRecordDTO.setRecordElementId(recordElement.getId());
                            activeRecordDTO.setSegmentId(segment.getId());
                        }
                        Log.d(App.getTag(), "Go to Segment" + activeRecordDTO.getSegmentId());
                        sendPlayMessage();
                        return;

                    }
                    if (!next) {
                        lastLastSegment = lastSegment;
                        lastLastRecordElement = lastRecordElement;
                        lastSegment = segment;
                        lastRecordElement = recordElement;
                    }
                }
            }
        }
    }

    //option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //on option menu item select
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Log.i(App.getTag(), "Pressed Settings");
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("MESSENGER", mMainActivity);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.i(App.getTag(), "Pressed back button");
        if (!toHomeScreen) {
            Log.v(App.getTag(), "to record list");
            showRecordsOnScreen();
        } else {
            Log.v(App.getTag(), "to previous Activity");
            super.onBackPressed();
        }
    }

    //send message to PlayerService. Only allowed way to send something to Service from MainActivity
    private void sendMessage(int statusCode, int value1, int value2, Bundle bundle) {
        Log.v(App.getTag(), "prepare Message with code " + statusCode);
        Message message = Message.obtain();
        message.what = statusCode;
        message.arg1 = value1;
        message.arg2 = value2;
        message.setData(bundle);
        try {
            mService.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //wrapper
    private void sendMessage(int statusCode) {
        sendMessage(statusCode, -1, -1, null);
    }

    //wrapper
    private void sendMessage(int statusCode, int value1, Bundle bundle) {
        sendMessage(statusCode, value1, -1, bundle);
    }

    //send message to service to start record
    private void sendRecordMessage() {
        Bundle bundle = new Bundle();
        int recordOperation;
        bundle.putSerializable("activeRecordDTO", activeRecordDTO);
        if (activeRecordDTO.getRecord() == null) {
            recordOperation = App.NEWRECORD;
        } else {
            recordOperation = App.NEWELEMENT;
        }
        sendMessage(App.RECORD, 1, recordOperation, bundle);
    }

    //send message to service start playback
    private void sendPlayMessage() {
        if (!records.isEmpty() && activeRecordDTO.getRecord() == null) {
            activeRecordDTO.setRecord(records.first());
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable("activeRecordDTO", activeRecordDTO);
        sendMessage(App.PLAY, 1, bundle);
    }

    private void setRecordingWidgetState() {
        activateButton(mStopButton);
        deactivateButton(mPlayButton);
    }


    @Override
    public void onResume() {
        Log.v(App.getTag(), "onResume");
        super.onResume();
        if (playing) {
            Log.d(App.getTag(), "app in playing state");
            setPlayingWidgetState();
            showSegmentsOnScreen(activeRecordDTO.getRecordElementId());
        }
        if (recording) {
            Log.d(App.getTag(), "app in recording state");
            setRecordingWidgetState();
        }
    }

    //focus on specific textview
    private void focusOnView() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (scrollToTextView != null) {
                    Log.v(App.getTag(), "focus on segment textview: " + scrollToTextView.getId());
                    scrollView.scrollTo(0, scrollToTextView.getBottom());
                }
            }
        });
    }

    //handler to get messages from service
    private class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.what;
            Log.v(App.getTag(), "got message with state " + state);
            switch (state) {
                case App.PLAY:
                    setPlayingWidgetState();
                    break;
                case App.RECORD:
                    activateButton(mStopButton);
                    deactivateButton(mPlayButton);
                    recording = true;
                    break;
                case App.STOP:
                    setDefaultWidgetState();
                    playing = false;
                    recording = false;
                    break;
                case App.PLAYEDSECOND:
                    setPlayingWidgetState();
                    setAudioProgress(message.arg1);
                    break;
                case App.DURATION:
                    setDuration(message.arg1);
                    break;
                case App.RECORDSECONDS:
                    setRecordingWidgetState();
                    setAudioProgressRecords(message.arg1);
                    break;
                case App.RELOADUI:
                    Log.v(App.getTag(), "ReloadUi Show: " + "segmentid " + activeRecordDTO.getSegmentId() + "elemendid " + activeRecordDTO.getRecordElementId());
                    records = database.getAllRecords();
                    if (activeRecordDTO.getRecord() != null) {
                        for (IRecord record : records) {
                            if (activeRecordDTO.getRecord().getId() == record.getId()) {
                                activeRecordDTO.setRecord(record);
                            }
                        }
                        if (activeRecordDTO.getRecordElementId() != -1) {
                            showSegmentsOnScreen(activeRecordDTO.getRecordElementId());
                            break;
                        }
                        showOpenRecordWithRecordElementsOnScreen(activeRecordDTO.getRecord().getId());
                        break;
                    }
                    showRecordsOnScreen();
                    break;
                case App.NEWSEGMENTPLAYING:
                    if (message.arg1 == 1 && !recording) {
                        setPlayingWidgetState();
                        Bundle bundle = message.getData();
                        playingRecordDTO = (ActiveRecordDTO) bundle.getSerializable("playingRecordDTO");
                        if (playingRecordDTO == null) {
                            Log.e(App.getTag(), "prevent nullpointer");
                            return;
                        }
                        markPlayingSegmentTextView(playingRecordDTO.getSegmentId());
                    }
                    break;
                default:
                    Log.e(App.getTag(), "Invalid Message Value");
            }
        }
    }
}
