package tiwa.htwg.org.mysmartvoicerecorder.transcript;

import android.util.Log;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;
import tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

class GoogleWebService implements ITranscriptService {
    @Override
    public String createTranscript(File file, String... settings) {
        String transcriptText = "Error";
        URL url;
        String output = "json";
        String lang = "de";
        String key = settings[0];
        String uri = "https://www.google.com/speech-api/v2/recognize?output=" + output + "&lang=" + lang + "&key=" + key;
        URLConnection urlConnection;
        InputStream in;
        try {
            url = new URL(uri);
            urlConnection = url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Cache-Control", "no-cache");
            urlConnection.setRequestProperty("Content-Type", "audio/l16; rate=22050;");
            urlConnection.setRequestProperty("User-Agent", "Chrome 41.0.2227.1");
            urlConnection.setConnectTimeout(5000);
            DataOutputStream request = new DataOutputStream(urlConnection.getOutputStream());
            request.write(SupportMethods.readFile(file));
            request.flush();
            request.close();
            in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            String rawResponse = result.toString();
            if (!rawResponse.contains("\"result\"") || !rawResponse.contains("transcript")) {
                throw new IOException();
            }
            int beginningOfTranscript = rawResponse.indexOf("transcript");
            beginningOfTranscript = beginningOfTranscript + 13;
            int endOfTranscript = rawResponse.indexOf(",", beginningOfTranscript);
            endOfTranscript = endOfTranscript - 1;
            transcriptText = rawResponse.substring(beginningOfTranscript, endOfTranscript);
            char lastChar = transcriptText.charAt(transcriptText.length() - 1);
            if (lastChar == '"') {
                transcriptText = transcriptText.substring(0, transcriptText.length() - 1);
            }
            transcriptText.trim();
            Log.d(App.getTag(), "transcript google result: " + transcriptText);
        } catch (Exception e) {
            Log.e(App.getTag(), e.toString());
        }
        return transcriptText;

    }


}
