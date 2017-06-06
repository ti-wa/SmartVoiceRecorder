package tiwa.htwg.org.mysmartvoicerecorder.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import tiwa.htwg.org.mysmartvoicerecorder.R;
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.IDatabaseApi;
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.IRecord;
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.IRecordElement;
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.ISegment;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;
import tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods;

import java.util.TreeSet;

public class RecordClickDialog extends DialogFragment {

    private IRecord record;
    private IDatabaseApi database;
    private Messenger mMainActivity;
    private String title = "";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setItems(R.array.onLongClickRecord, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Log.i(App.getTag(), "rename pressed");
                                rename();
                                break;
                            case 1:
                                Log.i(App.getTag(), "details pressed");
                                details();
                                break;
                            case 2:
                                Log.i(App.getTag(), "delete pressed");
                                delete();
                                break;
                        }
                    }
                });
        return builder.create();
    }

    public void addData(String title, IRecord record, IDatabaseApi database, Messenger mMainActivity) {
        this.record = record;
        this.database = database;
        this.mMainActivity = mMainActivity;
        if (title.length() > 27) {
            this.title = title.substring(0, 27) + " ...";
        } else {
            this.title = title;
        }
    }

    //textView for details dialog
    private TextView getTextView(String text) {
        TextView textView = new TextView(getActivity());
        textView.setPadding(50, 20, 0, 0);
        textView.setText(text);
        return textView;
    }

    //duration of record for details dialog
    private int calculateDuration() {
        int duration = 0;
        TreeSet<IRecordElement> recordElements = record.getRecordElements();
        for (IRecordElement recordElement : recordElements) {
            TreeSet<ISegment> segments = recordElement.getSegments();
            for (ISegment segment : segments) {
                duration += segment.getDuration();
            }
        }
        return duration;

    }

    //details button
    private void details() {
        LinearLayout linearLayoutKeys = new LinearLayout(getActivity());
        LinearLayout linearLayoutValues = new LinearLayout(getActivity());
        linearLayoutKeys.setOrientation(LinearLayout.VERTICAL);
        linearLayoutValues.setOrientation(LinearLayout.VERTICAL);
        LinearLayout linearLayoutKeyValues = new LinearLayout(getActivity());
        linearLayoutKeys.addView(getTextView(getResources().getString(R.string.changed)));
        linearLayoutKeys.addView(getTextView(getResources().getString(R.string.created)));
        linearLayoutKeys.addView(getTextView(getResources().getString(R.string.duration)));
        linearLayoutValues.addView(getTextView(SupportMethods.unixTimeToFormated(record.getUpdateDate())));
        linearLayoutValues.addView(getTextView(SupportMethods.unixTimeToFormated(record.getCreationDate())));
        linearLayoutValues.addView(getTextView(SupportMethods.millisecondsToHMS(calculateDuration())));
        linearLayoutKeyValues.addView(linearLayoutKeys);
        linearLayoutKeyValues.addView(linearLayoutValues);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(title);
        alert.setView(linearLayoutKeyValues);
        alert.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                return;
            }
        });
        alert.show();

    }

    //delete button
    private void delete() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(R.string.deleteQ);
        alert.setTitle(title);
        alert.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d(App.getTag(), "delete: " + record.getId());
                database.deleteRecord(record);
                reloadUi();
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();
    }

    //rename button
    private void rename() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText edittext = new EditText(getActivity());
        alert.setMessage(R.string.newname);
        alert.setTitle(title);
        alert.setView(edittext);
        alert.setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String newname = edittext.getText().toString();
                Log.d(App.getTag(), "rename: " + record.getName() + " to " + newname);
                record.setName(newname);
                database.updateRecord(record);
                reloadUi();
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();
    }

    //message to ui to reload to see changes
    private void reloadUi() {
        Message message = Message.obtain();
        message.what = App.RELOADUI;
        try {
            mMainActivity.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
