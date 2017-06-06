package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import tiwa.htwg.org.mysmartvoicerecorder.transcript.ITranscript;

public interface ISegment {
    int getDuration();

    void setDuration(int duration);

    int getCreationDate();

    void setCreationDate(int creationDate);

    int getUpdateDate();

    void setUpdateDate(int updateDate);

    ITranscript getTranscript();

    void setTranscript(ITranscript transcript);

    String getAudioPath();

    void setAudioPath(String audioPath);

    int getId();

    void setId(int id);

    int getFkRecordElement();

    void setFkRecordElement(int fkRecordElement);

    int getPosition();

    void setPosition(int position);
}
