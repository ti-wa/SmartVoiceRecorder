package tiwa.htwg.org.mysmartvoicerecorder.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.writer.WriterProcessor;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;


class Recorder {
    private static int segmentCounter = 1;
    private final int SAMPLESIZE = 16;
    private final int BUFFEROVERLAP = 0;
    private final int CHANNELS = 1;
    private int SAMPLERATE = 22000;
    private int BUFFERSIZE = 1024;
    private Thread segmentFinder;
    private TarsosDSPAudioFormat tarsosDSPAudioFormat;
    private AudioDispatcher dispatcher;
    private SilenceDetector silenceDetector;
    private WriterProcessor writerProcessor;
    private boolean segmenter;
    private String fileName;
    private int PAUSEDURATIONFORSEGMENT = 350;
    private double THRESHOLDSEGMENT = -80.0;
    private int MINIMUMSEGMENTDURATION = 2000;

    private RandomAccessFile createFileForRecord() throws FileNotFoundException {
        fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        fileName += File.separator + "voiceRecorder" + File.separator + "lastRecord" + segmentCounter;
        Log.d(App.getTag(), "Segmentcounter " + segmentCounter);
        File file = new File(fileName);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        segmentCounter++;
        return raf;
    }

    private void findSegments() {
        try {
            Thread.sleep(MINIMUMSEGMENTDURATION);
            while (segmenter) {
                if (silenceDetector.currentSPL() < THRESHOLDSEGMENT) {
                    for (int j = 0; j < PAUSEDURATIONFORSEGMENT + 5 && segmenter; j = j + 5) {
                        Thread.sleep(5);
                        if (silenceDetector.currentSPL() > THRESHOLDSEGMENT) {
                            break;
                        }
                        if (j > PAUSEDURATIONFORSEGMENT - 1) {
                            Log.d(App.getTag(), "PAUSE");
                            dispatcher.removeAudioProcessor(writerProcessor);
                            writerProcessor = new WriterProcessor(tarsosDSPAudioFormat, createFileForRecord());
                            dispatcher.addAudioProcessor(writerProcessor);
                            Thread.sleep(MINIMUMSEGMENTDURATION);
                        }
                    }
                }
                Thread.sleep(10);
            }
        } catch (Exception e) {
            Log.e(App.getTag(), e.toString());
        }

    }


    void record(Context context) {
        Log.d(App.getTag(), "init record settings");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        THRESHOLDSEGMENT = Double.valueOf(sharedPref.getString("segmentation_dzb", "-80"));
        PAUSEDURATIONFORSEGMENT = Integer.valueOf(sharedPref.getString("pause_for_segment", "350"));
        MINIMUMSEGMENTDURATION = Integer.valueOf(sharedPref.getString("minimum_duration", "2000"));
        Log.d(App.getTag(), "threshold " + THRESHOLDSEGMENT);
        Log.d(App.getTag(), "pauseduration " + PAUSEDURATIONFORSEGMENT);
        Log.d(App.getTag(), "minimumsegmentduration " + MINIMUMSEGMENTDURATION);
        segmentCounter = 1;
        boolean started = false;
        int i = 0;
        while (!started && i < 3) {
            try {
                Log.d(App.getTag(), "Samplerate is: " + SAMPLERATE);
                dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLERATE, BUFFERSIZE, BUFFEROVERLAP);
                silenceDetector = new SilenceDetector();
                tarsosDSPAudioFormat = new TarsosDSPAudioFormat((float) SAMPLERATE, SAMPLESIZE, CHANNELS, true, true);
                writerProcessor = new WriterProcessor(tarsosDSPAudioFormat, createFileForRecord());
                dispatcher.addAudioProcessor(writerProcessor);
                dispatcher.addAudioProcessor(silenceDetector);
                segmenter = true;
                new Thread(dispatcher, "Audio Dispatcher").start();
                segmentFinder = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        findSegments();
                    }
                });
                segmentFinder.start();
                started = true;
            } catch (Exception e) {
                i++;
                SAMPLERATE = 44100;
                BUFFERSIZE = 8192;
                Log.e(App.getTag(), "Record fail: " + e.toString());

            }
        }
    }

    int stopRecord() {
        try {
            Log.d(App.getTag(), "Stop Record");
            segmenter = false;
            segmentFinder.interrupt();
            dispatcher.stop();
        } catch (NullPointerException e) {
            Log.w(App.getTag(), "Got Nullpointer");
        }
        return segmentCounter;
    }


}
