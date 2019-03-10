package fyp.joystickcommander;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // JoystickView widget
    static JoystickView leftJoystickView;
    static JoystickView rightJoystickView;

    // widgets
    Button btn_on, btn_off;
    TextView tv_one, tv_two, tv_three, tv_four;
    Handler handler_btIn;

    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder dataReceiveString = new StringBuilder();

    final char STX = 0x02;
    final char ETX = 0x03;

    private ConnectedThread mConnectedThread;

    private static final UUID MYBTUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String btConnectedAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // linkage: findViewById declaration
        leftJoystickView = (JoystickView) findViewById(R.id.content_main_left_joystick_view);
        rightJoystickView = (JoystickView) findViewById(R.id.content_main_right_joystick_view);

        btn_on = (Button) findViewById(R.id.content_main_btn_on);
        btn_off = (Button) findViewById(R.id.content_main_btn_off);
        tv_one = (TextView) findViewById(R.id.content_main_textview_one);
        tv_two = (TextView) findViewById(R.id.content_main_textview_two);
        tv_three = (TextView) findViewById(R.id.content_main_textview_three);
        tv_four = (TextView) findViewById(R.id.content_main_textview_four);

        leftJoystickView.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                // TODO: set the left joystick onMove Listener
            }
        }, 17);

        rightJoystickView.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                // TODO: set the right joystick onMove Listener
            }
        }, 17);

        handler_btIn = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    dataReceiveString.append(readMessage);
                    tv_one.setText("Data Received = " + dataReceiveString.toString());
                    tv_two.setText("String length = " + String.valueOf(readMessage.length()));
                }
                dataReceiveString.delete(0, dataReceiveString.length());
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        // Set up onClick listeners for buttons to send A or B to turn on/off LED
        btn_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ByteArrayOutputStream output = new ByteArrayOutputStream(3);
                output.write(STX);
                output.write('B');
                output.write(ETX);
                mConnectedThread.write(output.toByteArray());
                Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });

        btn_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ByteArrayOutputStream output = new ByteArrayOutputStream(3);
                output.write(STX);
                output.write('A');
                output.write(ETX);
                mConnectedThread.write(output.toByteArray());
                Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        // creates secure outgoing connection with BT device using UUID
        return device.createRfcommSocketToServiceRecord(MYBTUUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Get MAC address from BtDeviceListActivity via intent
        Intent intent = getIntent();

        // Get the MAC address from the BtDeviceListActivity via EXTRA
        btConnectedAddress = intent.getStringExtra(BtDeviceListActivity.EXTRA_DEVICE_ADDRESS);

        // create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(btConnectedAddress);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }

        // Establish the Bluetooth socket connection
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e_second) {
                // TODO: let see need to deal with the socket closed?
            }
        }

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        // "123456" is to check the connection is establish or not
        //If it is not an exception will be thrown in the write method and finish() will be called
        StringBuilder temp = new StringBuilder("123456");
        ByteArrayOutputStream tempOutput = new ByteArrayOutputStream(6);
        mConnectedThread.write(tempOutput.toByteArray());

    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            btSocket.close();
        } catch (IOException e) {
            // TODO: let see need to deal with the socket closed?
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.bt_connection) {
            // Handle the camera action
        } /*else if (id == R.id.nav_gallery) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void checkBTState() {

        if (btAdapter == null) {
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
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        // create the connect thread
        public ConnectedThread (BluetoothSocket socket) {
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                // create I/O streams for BT connections
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                // TODO: deal with the I/O streams cannot be used
            }
            mmInStream = tempIn;
            mmOutStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for the messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);  // read bytes from input buffer
                    String readMessage = new String(buffer, 0 , bytes);
                    // Send the bytes back to the handler
                    handler_btIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    // ???? really need to break?
                    break;
                }
            }
        }

        public void write(byte[] input) {
            byte[] msgBuffer = input;
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), R.string.dialog_fragment_connection_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}