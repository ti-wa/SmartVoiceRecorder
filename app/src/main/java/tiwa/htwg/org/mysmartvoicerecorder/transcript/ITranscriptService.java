package tiwa.htwg.org.mysmartvoicerecorder.transcript;

import java.io.File;

interface ITranscriptService {
    String createTranscript(File file, String[] settings);
}
