package tiwa.htwg.org.mysmartvoicerecorder.transcript;

public interface ITranscript {
    String getText();

    void setText(String text);

    int getCreationDate();

    void setCreationDate(int date);

    int getUpdateDate();

    void setUpdateDate(int date);

    int getId();

    void setId(int id);
}
