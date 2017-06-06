package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import android.support.annotation.NonNull;

import java.util.TreeSet;

public class RecordElement implements IRecordElement, Comparable<IRecordElement> {
    private final TreeSet<ISegment> segments = new TreeSet<>();
    private int id;
    private int creationDate;
    private int updateDate;
    private String name;
    private int position;
    private int recordFk;

    @Override
    public int getRecordFk() {
        return recordFk;
    }

    @Override
    public void setRecordFk(int recordFk) {
        this.recordFk = recordFk;
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void addSegment(ISegment segment) {
        segments.add(segment);
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordElement that = (RecordElement) o;
        if (id != that.id) return false;
        if (creationDate != that.creationDate) return false;
        if (updateDate != that.updateDate) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return !(segments != null ? !segments.equals(that.segments) : that.segments != null);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + creationDate;
        result = 31 * result + updateDate;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (segments != null ? segments.hashCode() : 0);
        return result;
    }

    @Override
    public TreeSet<ISegment> getSegments() {
        return segments;
    }

    @Override
    public int compareTo(@NonNull IRecordElement another) {
        return this.getPosition() - another.getPosition();
    }
}
