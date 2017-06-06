package tiwa.htwg.org.mysmartvoicerecorder.transcript;

import android.util.Log;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;
import tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;

class NuanceWebService implements ITranscriptService {
    @Override
    public String createTranscript(File file, String[] settings) {
        URL url;
        String appid = settings[0];
        String key = settings[1];
        String text = "Error";
        String uri = "https://dictation.nuancemobility.net:443/NMDPAsrCmdServlet/dictation?appId=" + appid + "&appKey=" + key;
        HttpsURLConnection https = null;
        InputStream in;
        try {
            url = new URL(uri);
            https = (HttpsURLConnection) url.openConnection();
            https.setDoOutput(true);
            https.setUseCaches(false);
            https.setRequestProperty("TransferEncoding", "chunked");
            https.setRequestProperty("Content-Type", "audio/x-wav;codec=pcm;bit=16;rate=22000");
            https.setRequestProperty("Accept-Topic", "Dictation");
            https.setRequestProperty("Accept-Language", "de_DE");
            https.setConnectTimeout(5000);
            DataOutputStream request = new DataOutputStream(https.getOutputStream());
            request.write(SupportMethods.readFile(file));
            request.flush();
            request.close();
            in = new BufferedInputStream(https.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            text = reader.readLine();
            reader.close();
            Log.d(App.getTag(), "transcript nuace result: " + text);
        } catch (IOException e) {
            Log.e(App.getTag(), e.toString());
            if (https == null) {
                Log.e(App.getTag(), "prevent nullpointer");
                return text;
            }
            Log.e(App.getTag(), "error: " + https.getErrorStream());
            try {
                Log.e(App.getTag(), "HTTP-Code: " + https.getResponseCode());
            } catch (IOException e1) {
                Log.e(App.getTag(), e1.toString());
            }
        }
        return text;
    }
}
