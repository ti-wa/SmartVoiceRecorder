package tiwa.htwg.org.mysmartvoicerecorder.transcript;

public class Transcript implements ITranscript {


    private int id = -1;
    private String text;
    private int updateDate;
    private int creationDate;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
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
    public int getCreationDate() {
        return creationDate;
    }

    @Override
    public void setCreationDate(int creationDate) {
        this.creationDate = creationDate;
    }
}
