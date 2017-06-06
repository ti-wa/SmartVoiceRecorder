package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import android.support.annotation.NonNull;

import java.util.TreeSet;

public class Record implements IRecord, Comparable<IRecord> {
    private final TreeSet<IRecordElement> recordElements = new TreeSet<>();
    private int id;
    private int creationDate;
    private int updateDate;
    private String name;

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
    public TreeSet<IRecordElement> getRecordElements() {
        return recordElements;
    }

    @Override
    public void addRecordElement(IRecordElement recordElement) {
        recordElements.add(recordElement);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + creationDate;
        result = 31 * result + updateDate;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (recordElements != null ? recordElements.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@NonNull IRecord another) {
        return another.getUpdateDate() - this.getUpdateDate();
    }


}
