package tiwa.htwg.org.mysmartvoicerecorder.gui;


import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import tiwa.htwg.org.mysmartvoicerecorder.R;
import tiwa.htwg.org.mysmartvoicerecorder.helper.App;

public class SettingsActivity extends PreferenceActivity {
    private Messenger mMainActivity = null;

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Settings");
        addPreferencesFromResource(R.xml.preferences);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        mMainActivity = (Messenger) extras.get("MESSENGER");
    }

    @Override
    public void onBackPressed() {
        reloadUi();
        super.onBackPressed();
    }

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
