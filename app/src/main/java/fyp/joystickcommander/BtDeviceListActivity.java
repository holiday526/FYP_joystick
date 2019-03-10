package fyp.joystickcommander;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Set;

public class BtDeviceListActivity extends Activity {

    private static final String TAG = "BtDeviceListActivity";

    // BluetoothAdapter widget
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    private int REQUEST_ENABLE_BT = 1; // Any positive integer should work.

    // EXTRA string to send to MainActivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // textView or btn widgets

    TextView tv_connecting;


    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);
    }

    @Override
    public void onResume() {
        super.onResume();

        checkBtState();

        // TODO : create the bluetooth connection list

        tv_connecting = (TextView) findViewById(R.id.connecting);
        tv_connecting.setTextSize(40);
        tv_connecting.setText(" ");

        // Initialize array adapter for paired devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices and append to 'pairedDevices'
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Add previosuly paired devices to the array
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);//make title viewable
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.dialog_fragment_no_bt_client).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }

    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            tv_connecting.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Make an intent to start next activity while taking an extra which is the MAC address.
            Intent i = new Intent(BtDeviceListActivity.this, MainActivity.class);
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(i);
        }
    };

    private void checkBtState () {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if( mBtAdapter == null ) {
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.alert_dialog_bt_no_bt_module_title))
                    .setMessage(getResources().getString(R.string.alert_dialog_bt_no_bt_module_msg))
                    .setPositiveButton(
                            getResources().getString(R.string.alert_dialog_exit_btn),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    System.exit(0);
                                }
                            }
                    )
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            if (!mBtAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }
}
