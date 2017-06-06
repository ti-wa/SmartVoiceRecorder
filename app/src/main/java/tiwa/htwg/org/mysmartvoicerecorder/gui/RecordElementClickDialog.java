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
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.ActiveRecordDTO;
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.IDatabaseApi;
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.IRecordElement;
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.ISegment;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;
import tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods;

import java.util.TreeSet;

public class RecordElementClickDialog extends DialogFragment {
    private IDatabaseApi database;
    private Messenger mMainActivity;
    private String title = "";
    private ActiveRecordDTO activeRecordDTO;
    private Messenger mService;
    private IRecordElement recordElement;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setItems(R.array.onLongClickRecordElement, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Log.i(App.getTag(), "append pressed");
                                newRecordElementAfterThisRecordElement();
                                break;
                            case 1:
                                Log.i(App.getTag(), "rename pressed");
                                rename();
                                break;
                            case 2:
                                Log.i(App.getTag(), "details pressed");
                                details();
                                break;
                            case 3:
                                Log.i(App.getTag(), "delete pressed");
                                delete();
                                break;
                        }
                    }
                });
        return builder.create();
    }

    public void addData(String title, IRecordElement recordElement, IDatabaseApi database, Messenger mMainActivity, Messenger mService, ActiveRecordDTO activeRecordDTO) {
        this.recordElement = recordElement;
        this.database = database;
        this.mMainActivity = mMainActivity;
        this.mService = mService;
        this.activeRecordDTO = activeRecordDTO;
        if (title.length() > 27) {
            this.title = title.substring(0, 27) + " ...";
        } else {
            this.title = title;
        }
    }

    //wrapp message
    private void newRecordElementAfterThisRecordElement() {
        Bundle bundle = new Bundle();
        bundle.putSerializable("activeRecordDTO", activeRecordDTO);
        sendMessage(App.RECORD, 1, App.APPENDELEMENT, bundle);
    }

    //message to Service
    private void sendMessage(int statusCode, int value1, int value2, Bundle bundle) {
        Log.d(App.getTag(), "prepare Message with code " + statusCode);
        Message message = Message.obtain();
        message.what = statusCode;
        message.arg1 = value1;
        message.arg2 = value2;
        message.setData(bundle);
        try {
            mService.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //delete button
    private void delete() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(R.string.deleteQ);
        alert.setTitle(title);
        alert.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d(App.getTag(), "delete: " + recordElement.getId());
                TreeSet<IRecordElement> recordElements = new TreeSet<>();
                recordElements.add(recordElement);
                database.deleteRecordElements(recordElements);
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
                Log.d(App.getTag(), "rename: " + recordElement.getName() + " to " + newname);
                recordElement.setName(newname);
                TreeSet<IRecordElement> recordElements = new TreeSet<>();
                recordElements.add(recordElement);
                database.updateRecordElements(recordElements);
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

    //textView for details dialog
    private TextView getTextView(String text) {
        TextView textView = new TextView(getActivity());
        textView.setPadding(50, 20, 0, 0);
        textView.setText(text);
        return textView;
    }

    //duration of record for details dialog
    private int calculateDuration() {
        TreeSet<ISegment> segments = recordElement.getSegments();
        int duration = 0;
        for (ISegment segment : segments) {
            duration += segment.getDuration();

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
        linearLayoutValues.addView(getTextView(SupportMethods.unixTimeToFormated(recordElement.getUpdateDate())));
        linearLayoutValues.addView(getTextView(SupportMethods.unixTimeToFormated(recordElement.getCreationDate())));
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
}
