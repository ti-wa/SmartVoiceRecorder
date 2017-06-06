package tiwa.htwg.org.mysmartvoicerecorder.audio;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.*;
import android.util.Log;
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.*;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;

import java.util.TreeSet;

public class PlayerService extends Service {
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private int playPoint = 0;
    private MediaPlayer mPlayer = null;
    private MediaPlayer nextMplayer = null;
    private AudioManager myAudioManager;
    private Messenger mMainActivity;
    private boolean recording = false;
    private boolean playing = false;
    private ISegmentProcessor segmentProcesser;
    private DatabaseApi database;
    private Recorder recorder;
    private ActiveRecordDTO activeRecordDTO;
    private Thread watchPlayProgess;
    private Thread play;
    private int recordOperation;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(App.getTag(), "PlayService started");
        Bundle extras = intent.getExtras();
        mMainActivity = (Messenger) extras.get("MESSENGER");
        database = new DatabaseApi();
        database.initDatabase(this);
        myAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        segmentProcesser = new SegmentProcessor(mMainActivity);
        recorder = new Recorder();
        return Service.START_REDELIVER_INTENT;
    }

    //messages to Activity, only allowed way to do this
    private void sendMessage(int statusCode, int value1, int value2, Bundle bundle) {
        Log.v(App.getTag(), "prepare Message with code " + statusCode);
        Message message = Message.obtain();
        message.what = statusCode;
        message.arg1 = value1;
        message.arg2 = value2;
        message.setData(bundle);
        try {
            mMainActivity.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //wrapper
    private void sendMessage(int statusCode) {
        sendMessage(statusCode, -1, -1, null);
    }

    //wrapper
    private void sendMessage(int statusCode, int value1) {
        sendMessage(statusCode, value1, -1, null);
    }

    //wrapper
    private void sendMessage(int statusCode, int value1, Bundle bundle) {
        sendMessage(statusCode, value1, -1, bundle);
    }


    private void startRecording() {
        Log.d(App.getTag(), "Start Recording");
        startWakelock();
        recording = true;
        sendMessage(App.RECORD);
        recorder.record(getApplicationContext());
        new Thread(new Runnable() {
            @Override
            public void run() {
                watchRecordProgress();
            }
        }).start();
    }

    private void stopRecording() {
        Log.d(App.getTag(), "Stop Recording");
        if (recording) {
            sendMessage(App.STOP);
            int segmentCount = recorder.stopRecord();
            recording = false;
            switch (recordOperation) {
                case App.NEWRECORD:
                    Log.d(App.getTag(), "new record");
                    segmentProcesser.newRecord(database, segmentCount, getApplicationContext());
                    break;
                case App.NEWELEMENT:
                    Log.d(App.getTag(), "new element");
                    segmentProcesser.newRecordElementAtEnd(database, segmentCount, getApplicationContext(), activeRecordDTO);
                    break;
                case App.APPENDELEMENT:
                    Log.d(App.getTag(), "append on recordElement");
                    segmentProcesser.appendRecordElementOnRecordElement(database, segmentCount, getApplicationContext(), activeRecordDTO);
                    break;
                case App.APPENDSEGMENT:
                    Log.d(App.getTag(), "append on segment");
                    segmentProcesser.appendRecordElementOnSegment(database, segmentCount, getApplicationContext(), activeRecordDTO);
                    break;
            }
            stopWakelock();
        }
    }

    //collect infos once every playback to send to GUI - atm only duration
    private void collectPlayingInfos() {
        int duration = 0;
        IRecord record = activeRecordDTO.getRecord();
        TreeSet<IRecordElement> recordElements = record.getRecordElements();
        for (IRecordElement recordElement : recordElements) {
            TreeSet<ISegment> segments = recordElement.getSegments();
            for (ISegment segment : segments) {
                duration += segment.getDuration();
            }
        }
        Log.v(App.getTag(), "duration of playback count " + duration);
        sendMessage(App.DURATION, duration);
    }

    private void startPlaying() {
        Log.v(App.getTag(), "start playing");
        startWakelock();
        playing = true;
        sendMessage(App.PLAY);
        play = new Thread(new Runnable() {
            @Override
            public void run() {
                playDisposer();
            }
        });
        play.start();
        watchPlayProgess = new Thread(new Runnable() {
            @Override
            public void run() {
                watchPlayProgress();
            }
        });
        watchPlayProgess.start();
        collectPlayingInfos();
    }

    //start the media player of each segment and prepare next mediaplayer for following segment
    private void playDisposer() {
        Log.v(App.getTag(), "Start play Disposer");
        try {
            boolean startFound = false;
            IRecord record = activeRecordDTO.getRecord();
            ActiveRecordDTO playingRecordDTO = activeRecordDTO.clone();
            TreeSet<IRecordElement> recordElements = record.getRecordElements();
            for (IRecordElement recordElement : recordElements) {
                playingRecordDTO.setRecordElementId(recordElement.getId());
                if (recordElement.getId() == activeRecordDTO.getRecordElementId() || startFound) {
                    TreeSet<ISegment> segments = recordElement.getSegments();
                    for (ISegment segment : segments) {
                        playingRecordDTO.setSegmentId(segment.getId());
                        if (segment.getId() == activeRecordDTO.getSegmentId() || startFound) {
                            if (!startFound) {
                                mPlayer = new MediaPlayer();
                                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                mPlayer.setDataSource(segment.getAudioPath());
                                mPlayer.prepare();
                                mPlayer.setVolume(1.0f, 1.0f);
                                myAudioManager.setSpeakerphoneOn(true);
                                myAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, myAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
                                Log.d(App.getTag(), "start playback of first segment");
                                mPlayer.start();
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("playingRecordDTO", playingRecordDTO);
                                sendMessage(App.NEWSEGMENTPLAYING, 1, bundle);
                                Thread.sleep(100);
                                startFound = true;
                                continue;
                            }
                            //always true if reached but easyer to understand
                            if (startFound) {
                                Log.d(App.getTag(), "prepare mediaplayer for next segment for playback");
                                nextMplayer = new MediaPlayer();
                                nextMplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                nextMplayer.setDataSource(segment.getAudioPath());
                                nextMplayer.prepare();
                                mPlayer.setNextMediaPlayer(nextMplayer);
                                while (!nextMplayer.isPlaying()) {
                                    Thread.sleep(10);
                                }
                                Log.d(App.getTag(), "next mediaplayer started");
                                mPlayer = nextMplayer;
                                nextMplayer = null;
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("playingRecordDTO", playingRecordDTO);
                                sendMessage(App.NEWSEGMENTPLAYING, 1, bundle);
                                Thread.sleep(100);
                            }
                        }
                    }
                }
            }
            boolean check = true;
            while (mPlayer.isPlaying() || check) {
                Thread.sleep(10);
                if (!mPlayer.isPlaying()) {
                    Thread.sleep(10);
                    check = false;
                    continue;
                }
                check = true;
            }
            stopPlaying();

        } catch (Exception e) {
            Log.e(App.getTag(), e.toString());
        }

    }

    //set time point of playback and send to service. synchronized to avoid race conditions in feature
    private synchronized void changePlayPoint(int changeValue) {
        playPoint = playPoint + changeValue;
        sendMessage(App.PLAYEDSECOND, playPoint);
    }

    //should start wakelock not implemented
    private void startWakelock() {
     /*   powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();*/
    }

    private void stopWakelock() {
        //wakeLock.release();

    }

    //seconds counter of playback for gui
    private void watchPlayProgress() {
        int point = 0;
        boolean sumAll = false;
        IRecord record = activeRecordDTO.getRecord();
        TreeSet<IRecordElement> recordElements = record.getRecordElements();
        for (IRecordElement recordElement : recordElements) {
            if (sumAll) {
                break;
            }
            TreeSet<ISegment> segments = recordElement.getSegments();
            for (ISegment segment : segments) {
                if (segment.getId() == activeRecordDTO.getSegmentId()) {
                    sumAll = true;
                    break;
                }
                point += segment.getDuration();
            }
        }
        changePlayPoint(point);
        try {
            while (playing) {
                Thread.sleep(1000L);
                changePlayPoint(1000);
            }
        } catch (RuntimeException e) {
            Log.e(App.getTag(), e.toString());
        } catch (InterruptedException e) {
            Log.d(App.getTag(), "WatchRecordThread stopped");
        }
    }

    //seconds counter of recording for gui
    private void watchRecordProgress() {
        try {
            int seconds = 0;
            while (recording) {
                sendMessage(App.RECORDSECONDS, seconds);
                seconds = seconds + 1000;
                Thread.sleep(1000L);
            }
        } catch (RuntimeException e) {
            Log.e(App.getTag(), e.toString());
        } catch (InterruptedException e) {
            Log.d(App.getTag(), "WatchRecordThread stopped");
        }
    }

    private void stopPlaying() {
        Log.d(App.getTag(), "stop Playing");
        try {
            sendMessage(App.STOP);
            stopWakelock();
            playing = false;
            watchPlayProgess.interrupt();
            play.interrupt();
            mPlayer.stop();
            nextMplayer.stop();
            mPlayer.release();
            nextMplayer.release();
            mPlayer = null;
            nextMplayer = null;

        } catch (Exception e) {
            Log.e(App.getTag(), e.toString());
        }
    }

    //called if no segment is set. return first segment of record element
    private void setMissingSegmentIdToDTO() {
        Log.v(App.getTag(), "set Missing Segment id");
        IRecord record = activeRecordDTO.getRecord();
        TreeSet<IRecordElement> recordElements = record.getRecordElements();
        for (IRecordElement recordElement : recordElements) {
            if (recordElement.getId() == activeRecordDTO.getRecordElementId()) {
                TreeSet<ISegment> segments = recordElement.getSegments();
                activeRecordDTO.setSegmentId(segments.first().getId());
                return;
            }
        }
        Log.e(App.getTag(), "No Segment in DTO set");
    }

    //called if no record element is set. return first record element of record
    private void setMissingRecordElementIdToDTO() {
        Log.v(App.getTag(), "set Missing RecordElement id");
        IRecord record = activeRecordDTO.getRecord();
        TreeSet<IRecordElement> recordElements = record.getRecordElements();
        activeRecordDTO.setRecordElementId(recordElements.first().getId());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(App.getTag(), "in onBind");
        return mMessenger.getBinder();
    }

    //messages from Activity
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.what;
            Log.v(App.getTag(), "Got message with state: " + state);
            switch (state) {
                case App.PLAY:
                    if (recording) {
                        return;
                    }
                    if (playing) {
                        stopPlaying();
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    playPoint = 0;
                    if (message.arg1 == 1) {
                        Bundle bundle = message.getData();
                        activeRecordDTO = (ActiveRecordDTO) bundle.getSerializable("activeRecordDTO");
                        if (activeRecordDTO == null) {
                            Log.e(App.getTag(), "prevent nullpointer");
                            return;
                        }
                        if (activeRecordDTO.getRecordElementId() == -1) {
                            setMissingRecordElementIdToDTO();
                        }
                        if (activeRecordDTO.getSegmentId() == -1) {
                            setMissingSegmentIdToDTO();
                        }
                    }
                    startPlaying();
                    break;
                case App.RECORD:
                    if (message.arg1 == 1 && !recording) {
                        Bundle bundle = message.getData();
                        recordOperation = message.arg2;
                        activeRecordDTO = (ActiveRecordDTO) bundle.getSerializable("activeRecordDTO");
                        startRecording();
                    }
                    break;
                case App.STOP:
                    if (recording) {
                        stopRecording();
                    }
                    if (playing) {
                        stopPlaying();
                    }
                    break;
                case App.DURATION:
                    collectPlayingInfos();
                default:
                    Log.e(App.getTag(), "Invalid message sent to service");
                    break;
            }
        }
    }
}

