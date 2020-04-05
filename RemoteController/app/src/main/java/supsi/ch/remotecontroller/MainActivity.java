package supsi.ch.remotecontroller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mSpinnerAdapter;

    private final int REQUEST_ENABLE_BT = 1;
    private final int PERMISSION_LOCATION_REQUEST = 2;
    private final UUID serviceUuid = UUID.fromString("04c6093b-0000-1000-8000-00805f9b34fb");

    private final Map<String, BluetoothDevice> mDevicesMap = new HashMap<>();

    private TextView connectionStatus;
    private Spinner selectDevice;
    private Button clickLeft;
    private Button clickRight;
    private Button manageConnection;
    private Button manageDiscovery;

    private BluetoothSocket clientSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        connectionStatus = findViewById(R.id.connection_status);
        selectDevice = findViewById(R.id.select_device);
        clickLeft = findViewById(R.id.click_left);
        clickRight = findViewById(R.id.click_right);
        manageDiscovery = findViewById(R.id.manage_discovery);
        manageConnection = findViewById(R.id.manage_connection);

        // If bluetooth is not supported stop the activity
        if(mBluetoothAdapter == null){
            finish();
            return;
        }

        // If bluetooth is disable perform a request in order to able this feature
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mSpinnerAdapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.simple_spinner_item
        );

        // Get all previous paired devices and store information in a map
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(!pairedDevices.isEmpty()){
            for(BluetoothDevice device : pairedDevices){
                mDevicesMap.put(device.getName(), device);
                mSpinnerAdapter.add(device.getName());
            }
        }

        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectDevice.setAdapter(mSpinnerAdapter);

        manageDiscovery.setOnClickListener(manageDiscoveryListener);
        manageConnection.setOnClickListener(manageConnectionListener);
        clickLeft.setOnClickListener(performClickListener);
        clickRight.setOnClickListener(performClickListener);

    }

    private void performClick(boolean left, boolean right){
        if (clientSocket == null || !clientSocket.isConnected()) return;

        try{
            OutputStream outputStream = clientSocket.getOutputStream();
            byte[] data = null;
            if(left){
                data = ByteBuffer.allocate(4).putInt(2).array();

            }else if(right){
                data = ByteBuffer.allocate(4).putInt(1).array();
            }
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    private final View.OnClickListener performClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.click_left){
                performClick(true, false);
            }else if (v.getId() == R.id.click_right){
                performClick(false, true);
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            performClick(true, false);
        }else if ( keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            performClick(false, true);
        }

        return true;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!mDevicesMap.containsKey(device.getName())){
                    mDevicesMap.put(device.getName(), device);
                    mSpinnerAdapter.add(device.getName());
                }
            }
        }
    };

    private final View.OnClickListener manageConnectionListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
                manageDiscovery.setText(getString(R.string.start_discovery));
            }
            if(!(clientSocket != null && clientSocket.isConnected())){
                Toast.makeText(MainActivity.this, getResources().getString(R.string.trying_open_connection), Toast.LENGTH_LONG).show();
                new connectDevice().execute(mDevicesMap.get(selectDevice.getSelectedItem().toString()));
            }else{
                try {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.trying_close_connection), Toast.LENGTH_LONG).show();
                    clientSocket.close();
                    connectionStatus.setText(getString(R.string.status_disconnected));
                    manageConnection.setText(getString(R.string.open_connection));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private final View.OnClickListener manageDiscoveryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mBluetoothAdapter.isDiscovering()) {
                manageDiscovery.setText(getString(R.string.stop_discovery));
                Toast.makeText(MainActivity.this, getResources().getString(R.string.start_discovery), Toast.LENGTH_LONG).show();

                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver,filter);
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_REQUEST);
                    }
                }else {
                    if (!mBluetoothAdapter.startDiscovery()) {
                        Log.e("Discovery", "failed");
                    }
                }
            }else{
                manageDiscovery.setText(getString(R.string.stop_discovery));
                mBluetoothAdapter.cancelDiscovery();
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_ENABLE_BT){
            // This is the result for the intent Bluetooth request to enable
            if(resultCode == RESULT_OK){
                // Bluetooth is enabled, we can start the activity
            }else{
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!mBluetoothAdapter.startDiscovery()) {
                        Log.e("Discovery", "failed");
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        if(clientSocket.isConnected()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final class connectDevice extends AsyncTask<BluetoothDevice, Void, Boolean>{
        @Override
        protected Boolean doInBackground(BluetoothDevice... devices) {

            try {
                clientSocket = devices[0].createRfcommSocketToServiceRecord(serviceUuid);
                try {
                    clientSocket.connect();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean isConnected) {
            if(isConnected){
                Toast.makeText(MainActivity.this, getResources().getString(R.string.device_connected), Toast.LENGTH_LONG).show();
                manageConnection.setText(getString(R.string.close_connection));
                connectionStatus.setText(getString(R.string.status_connected));
            }
            else{
                Toast.makeText(MainActivity.this, getResources().getString(R.string.device_not_connected), Toast.LENGTH_LONG).show();
                manageConnection.setText(getString(R.string.open_connection));
                connectionStatus.setText(getString(R.string.status_disconnected));
            }
        }
    };
}
