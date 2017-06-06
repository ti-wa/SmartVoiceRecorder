package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import android.support.annotation.NonNull;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.ITranscript;

public class Segment implements ISegment, Comparable<Segment> {
    private int id = -1;
    private int duration;
    private int creationDate;
    private int updateDate;
    private ITranscript transcript;
    private String audioPath;
    private int Position;
    private int fkRecordElement;

    @Override
    public int getFkRecordElement() {
        return fkRecordElement;
    }

    @Override
    public void setFkRecordElement(int fkRecordElement) {
        this.fkRecordElement = fkRecordElement;
    }

    @Override
    public int getPosition() {
        return Position;
    }

    @Override
    public void setPosition(int position) {
        Position = position;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public int getCreationDate() {
        return creationDate;
    }

    @Override
    public void setCreationDate(int creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public int getUpdateDate() {
        return updateDate;
    }

    @Override
    public void setUpdateDate(int updateDate) {
        this.updateDate = updateDate;
    }

    @Override
    public ITranscript getTranscript() {
        return transcript;
    }

    @Override
    public void setTranscript(ITranscript transcript) {
        this.transcript = transcript;
    }

    @Override
    public String getAudioPath() {
        return audioPath;
    }

    @Override
    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Segment segment = (Segment) o;
        if (id != segment.id) return false;
        if (Double.compare(segment.duration, duration) != 0) return false;
        if (creationDate != segment.creationDate) return false;
        if (updateDate != segment.updateDate) return false;
        return !(transcript != null ? !transcript.equals(segment.transcript) : segment.transcript != null);

    }


    public int hashCode() {
        int result;
        long temp;
        result = id;
        temp = Double.doubleToLongBits(duration);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + creationDate;
        result = 31 * result + updateDate;
        result = 31 * result + (transcript != null ? transcript.hashCode() : 0);
        return result;
    }


    @Override
    public int compareTo(@NonNull Segment another) {
        return this.getPosition() - another.getPosition();
    }
}
