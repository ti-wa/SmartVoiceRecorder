package tiwa.htwg.org.mysmartvoicerecorder.datalayer;

import java.io.Serializable;

public class ActiveRecordDTO implements Serializable {
    private IRecord record = null;
    private int recordElementId = -1;
    private int segmentId = -1;

    public IRecord getRecord() {
        return record;
    }

    public void setRecord(IRecord record) {
        this.record = record;
    }

    public int getRecordElementId() {
        return recordElementId;
    }

    public void setRecordElementId(int recordElementId) {
        this.recordElementId = recordElementId;
    }

    public int getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(int segmentId) {
        this.segmentId = segmentId;
    }

    @Override
    public ActiveRecordDTO clone() {
        ActiveRecordDTO activeRecordDTO = new ActiveRecordDTO();
        activeRecordDTO.setSegmentId(segmentId);
        activeRecordDTO.setRecordElementId(recordElementId);
        activeRecordDTO.setRecord(record);
        return activeRecordDTO;
    }
}
