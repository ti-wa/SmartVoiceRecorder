package tiwa.htwg.org.mysmartvoicerecorder.datalayer;


import java.util.TreeSet;

public interface IRecord {
    int getId();

    void setId(int id);

    int getCreationDate();

    void setCreationDate(int creationDate);

    int getUpdateDate();

    void setUpdateDate(int updateDate);

    String getName();

    void setName(String name);

    void addRecordElement(IRecordElement record);

    TreeSet<IRecordElement> getRecordElements();
}
