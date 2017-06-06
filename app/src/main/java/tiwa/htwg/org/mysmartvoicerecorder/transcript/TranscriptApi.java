package tiwa.htwg.org.mysmartvoicerecorder.transcript;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import tiwa.htwg.org.mysmartvoicerecorder.R;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;
import tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods;

import java.io.File;

public class TranscriptApi implements ITranscriptApi {
    private final SharedPreferences sharedPref;
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int operation;
    private String nuaceAppId;
    private String nuaceKey;
    private String googleKey;
    private int segmentCount;
    private int counter;

    public TranscriptApi(Context context, int segmentCount) {
        this.context = context;
        this.segmentCount = segmentCount;
        counter = 0;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        operation = Integer.valueOf(sharedPref.getString("transcripe_service", "1"));
        nuaceAppId = sharedPref.getString("nuance_app_id", "error");
        nuaceKey = sharedPref.getString("nuance_key", "error");
        googleKey = sharedPref.getString("google_key", "error");
        if (operation != 0) {
            if (!isNetworkAvailable(context)) {
                Log.w(App.getTag(), "network not available");
                toastMessage(context, context.getResources().getString(R.string.noNetwork));
                operation = 0;
            }
        }
    }

    @Override
    public ITranscript createTranscript(String path) {
        Log.v(App.getTag(), "create Transcript for: " + path);
        boolean configError = false;
        ITranscript transcript = new Transcript();
        transcript.setText(SupportMethods.getTimeFormated());
        File audiofile = new File(path);
        Log.d(App.getTag(), "operation for transcription " + operation);
        if (operation != 0) {
            if (isNetworkAvailable(context)) {
                Log.v(App.getTag(), "network available");
                ITranscriptService transcriptService;
                switch (operation) {
                    case 1:
                        if (googleKey.contentEquals("error")) {
                            configError = true;
                            break;
                        }
                        transcriptService = new GoogleWebService();
                        transcript.setText(transcriptService.createTranscript(audiofile, new String[]{googleKey}));
                        break;
                    case 2:
                        if (nuaceKey.contentEquals("error") || nuaceAppId.contentEquals("error")) {
                            configError = true;
                            break;
                        }
                        transcriptService = new NuanceWebService();
                        transcript.setText(transcriptService.createTranscript(audiofile, new String[]{nuaceAppId, nuaceKey}));
                        break;
                }
                if (configError) {
                    toastMessage(context, context.getResources().getString(R.string.configErrorTrancript));
                    Log.e(App.getTag(), "Transcription service not configured: " + operation);
                    operation = 0;
                    return transcript;
                }
                counter++;
                showToastTranscriptProgress(context, counter, segmentCount);
            } else {
                Log.w(App.getTag(), "network not available");
                toastMessage(context, context.getResources().getString(R.string.noNetwork));
                operation = 0;
            }
        }
        return transcript;
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void toastMessage(final Context context, final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showToastTranscriptProgress(final Context context, final int counter, final int sum) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, counter + " of " + sum + " transcribed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
