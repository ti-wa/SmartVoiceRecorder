package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import java.util.TreeSet;

public interface IRecordElement {
    int getId();

    void setId(int id);

    int getCreationDate();

    void setCreationDate(int creationDate);

    int getUpdateDate();

    void setUpdateDate(int updateDate);

    String getName();

    void setName(String name);

    void addSegment(ISegment segment);

    TreeSet<ISegment> getSegments();

    int getPosition();

    void setPosition(int position);

    int getRecordFk();

    void setRecordFk(int recordFk);
}
