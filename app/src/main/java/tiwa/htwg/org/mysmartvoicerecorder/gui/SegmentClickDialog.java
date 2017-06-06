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
import tiwa.htwg.org.mysmartvoicerecorder.datalayer.ISegment;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;
import tiwa.htwg.org.mysmartvoicerecorder.helper.SupportMethods;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.ITranscript;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.ITranscriptApi;
import tiwa.htwg.org.mysmartvoicerecorder.transcript.TranscriptApi;

import java.util.TreeSet;

public class SegmentClickDialog extends DialogFragment {
    private Messenger mMainActivity;
    private String title = "";
    private ActiveRecordDTO activeRecordDTO;
    private ISegment segment;
    private Messenger mService;
    private IDatabaseApi database;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setItems(R.array.onLongClickSegment, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                newRecordElementAfterSegment();
                                Log.i(App.getTag(), "append pressed");
                                break;
                            case 1:
                                Log.i(App.getTag(), "rename pressed");
                                rename();
                                break;
                            case 2:
                                Log.i(App.getTag(), "transcribe pressed");
                                transcribe();
                                break;
                            case 3:
                                Log.i(App.getTag(), "details pressed");
                                details();
                                break;
                            case 4:
                                Log.i(App.getTag(), "delete pressed");
                                delete();
                                break;
                        }
                    }
                });
        return builder.create();
    }

    public void addData(String title, ActiveRecordDTO activeRecordDTO, Messenger mService, ISegment segment, IDatabaseApi databaseApi, Messenger mMainActivity) {
        this.activeRecordDTO = activeRecordDTO;
        this.segment = segment;
        this.mService = mService;
        this.database = databaseApi;
        this.mMainActivity = mMainActivity;
        if (title.length() > 27) {
            this.title = title.substring(0, 27) + " ...";
        } else {
            this.title = title;
        }
    }

    private void newRecordElementAfterSegment() {
        Bundle bundle = new Bundle();
        bundle.putSerializable("activeRecordDTO", activeRecordDTO);
        sendMessage(App.RECORD, 1, App.APPENDSEGMENT, bundle);
    }

    //transcribe button
    private void transcribe() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ITranscriptApi transcriptApi = new TranscriptApi(getActivity(), 1);
                ITranscript apiTranscript = transcriptApi.createTranscript(segment.getAudioPath());
                ITranscript segmentTranscript = segment.getTranscript();
                segmentTranscript.setText(apiTranscript.getText());
                database.updateTranscript(segmentTranscript);
                reloadUi();
            }
        }).start();
    }

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
                Log.d(App.getTag(), "rename: " + segment.getTranscript().getText() + " to " + newname);
                ITranscript transcript = segment.getTranscript();
                transcript.setText(newname);
                database.updateTranscript(transcript);
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
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
                Log.d(App.getTag(), "delete " + segment.getId());
                TreeSet<ISegment> segments = new TreeSet<>();
                segments.add(segment);
                database.deleteSegments(segments);
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

    //textView for details dialog
    private TextView getTextView(String text) {
        TextView textView = new TextView(getActivity());
        textView.setPadding(50, 20, 0, 0);
        textView.setText(text);
        return textView;
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
        linearLayoutValues.addView(getTextView(SupportMethods.unixTimeToFormated(segment.getUpdateDate())));
        linearLayoutValues.addView(getTextView(SupportMethods.unixTimeToFormated(segment.getCreationDate())));
        linearLayoutValues.addView(getTextView(SupportMethods.millisecondsToHMS(segment.getDuration())));
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