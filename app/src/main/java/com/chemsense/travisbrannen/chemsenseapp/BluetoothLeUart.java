package com.chemsense.travisbrannen.chemsenseapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.WeakHashMap;
import java.lang.String;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BluetoothLeUart extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {

    public ArrayList mNewPoints;
    int mNumBadPackets = 0;
    int mNumPackets = 0;
    int mMissedPackets = 1;
    int mGottenPackets = 0;
    private boolean mConnected = false;
    private boolean mJustConnected = false;
    private boolean mJustDisconnected = false;
    private boolean mJustFoundDevice = false;
    private boolean mJustGotDeviceInfo = false;
    private boolean mJustFailedConnection = false;
    // UUIDs for UART service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID   = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID   = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    // UUID for the UART BTLE client characteristic which is necessary for notifications.
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Device Information service and associated characeristics.
    public static UUID DIS_UUID       = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_MANUF_UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_MODEL_UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_HWREV_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_SWREV_UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");

    // Internal UART state.
    private Context context;
    private WeakHashMap<Callback, Object> callbacks;
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    private boolean connectFirst;
    private boolean writeInProgress; // Flag to indicate a write is currently in progress

    // Device Information state.
    private BluetoothGattCharacteristic disManuf;
    private BluetoothGattCharacteristic disModel;
    private BluetoothGattCharacteristic disHWRev;
    private BluetoothGattCharacteristic disSWRev;
    private boolean disAvailable;

    // Queues for characteristic read (synchronous)
    private Queue<BluetoothGattCharacteristic> readQueue;

    // Interface for a BluetoothLeUart client to be notified of UART actions.
    public interface Callback {
        public void onConnected(BluetoothLeUart uart);
        public void onConnectFailed(BluetoothLeUart uart);
        public void onDisconnected(BluetoothLeUart uart);
        public void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx);
        public void onDeviceFound(BluetoothDevice device);
        public void onDeviceInfoAvailable();
    }

    public BluetoothLeUart(Context context) {
        super();
        this.context = context;
        this.callbacks = new WeakHashMap<Callback, Object>();
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.gatt = null;
        this.tx = null;
        this.rx = null;
        this.disManuf = null;
        this.disModel = null;
        this.disHWRev = null;
        this.disSWRev = null;
        this.disAvailable = false;
        this.connectFirst = false;
        this.writeInProgress = false;
        this.readQueue = new ConcurrentLinkedQueue<BluetoothGattCharacteristic>();
        mNewPoints = new ArrayList();
    }

    // Return instance of BluetoothGatt.
    public BluetoothGatt getGatt() {
        return gatt;
    }

    // Return true if connected to UART device, false otherwise.
    public boolean isConnected() {
        return (tx != null && rx != null);
    }

    public String getDeviceInfo() {
        if (tx == null || !disAvailable ) {
            // Do nothing if there is no connection.
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Manufacturer : " + disManuf.getStringValue(0) + "\n");
        sb.append("Model        : " + disModel.getStringValue(0) + "\n");
        sb.append("Firmware     : " + disSWRev.getStringValue(0) + "\n");
        return sb.toString();
    };

    public boolean deviceInfoAvailable() { return disAvailable; }

    // Send data to connected UART device.
    public void send(byte[] data) {
        if (tx == null || data == null || data.length == 0) {
            // Do nothing if there is no connection or message to send.
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        tx.setValue(data);
        writeInProgress = true; // Set the write in progress flag
        gatt.writeCharacteristic(tx);
        // ToDo: Update to include a timeout in case this goes into the weeds
        while (writeInProgress); // Wait for the flag to clear in onCharacteristicWrite
    }

    // Send data to connected UART device.
    public void send(String data) {
        if (data != null && !data.isEmpty()) {
            send(data.getBytes(Charset.forName("UTF-8")));
        }
    }

    // Register the specified callback to receive UART callbacks.
    public void registerCallback(Callback callback) {
        callbacks.put(callback, null);
    }

    // Unregister the specified callback.
    public void unregisterCallback(Callback callback) {
        callbacks.remove(callback);
    }

    // Disconnect to a device if currently connected.
    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
        }
        gatt = null;
        tx = null;
        rx = null;
    }

    // Stop any in progress UART device scan.
    public void stopScan() {
        if (adapter != null) {
            adapter.stopLeScan(this);
        }
    }

    // Start scanning for BLE UART devices.  Registered callback's onDeviceFound method will be called
    // when devices are found during scanning.
    public void startScan() {
        if (adapter != null) {
            adapter.startLeScan(this);
        }
    }

    // Connect to the first available UART device.
    public void connectFirstAvailable() {
        // Disconnect to any connected device.
        disconnect();
        // Stop any in progress device scan.
        stopScan();
        // Start scan and connect to first available device.
        connectFirst = true;
        startScan();
    }

    // Handlers for BluetoothGatt and LeScan events.
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Connected to device, start discovering services.
                if (!gatt.discoverServices()) {
                    // Error starting service discovery.
                    connectFailure();
                }
            }
            else {
                // Error connecting to device.
                connectFailure();
            }
        }
        else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            // Disconnected, notify callbacks of disconnection.
            rx = null;
            tx = null;
            notifyOnDisconnected(this);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        // Notify connection failure if service discovery failed.
        if (status == BluetoothGatt.GATT_FAILURE) {
            connectFailure();
            return;
        }

        // Save reference to each UART characteristic.
        tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
        rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);

        // Save reference to each DIS characteristic.
        //disManuf = gatt.getService(DIS_UUID).getCharacteristic(DIS_MANUF_UUID);
        //disModel = gatt.getService(DIS_UUID).getCharacteristic(DIS_MODEL_UUID);
        //disHWRev = gatt.getService(DIS_UUID).getCharacteristic(DIS_HWREV_UUID);
        //disSWRev = gatt.getService(DIS_UUID).getCharacteristic(DIS_SWREV_UUID);

        // Add device information characteristics to the read queue
        // These need to be queued because we have to wait for the response to the first
        // read request before a second one can be processed (which makes you wonder why they
        // implemented this with async logic to begin with???)
        //readQueue.offer(disManuf);
        //readQueue.offer(disModel);
        //readQueue.offer(disHWRev);
        //readQueue.offer(disSWRev);

        // Request a dummy read to get the device information queue going
        //gatt.readCharacteristic(disManuf);

        // Setup notifications on RX characteristic changes (i.e. data received).
        // First call setCharacteristicNotification to enable notification.
        if (!gatt.setCharacteristicNotification(rx, true)) {
            // Stop if the characteristic notification setup failed.
            connectFailure();
            return;
        }
        // Next update the RX characteristic's client descriptor to enable notifications.
        BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
        if (desc == null) {
            // Stop if the RX characteristic has no client descriptor.
            connectFailure();
            return;
        }
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(desc)) {
            // Stop if the client descriptor could not be written.
            connectFailure();
            return;
        }
        // Notify of connection completion.
        notifyOnConnected(this);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        notifyOnReceive(this, characteristic);
    }

    @Override
    public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            //Log.w("DIS", characteristic.getStringValue(0));
            // Check if there is anything left in the queue
            BluetoothGattCharacteristic nextRequest = readQueue.poll();
            if(nextRequest != null){
                // Send a read request for the next item in the queue
                gatt.readCharacteristic(nextRequest);
            }
            else {
                // We've reached the end of the queue
                disAvailable = true;
                notifyOnDeviceInfoAvailable();
            }
        }
        else {
            //Log.w("DIS", "Failed reading characteristic " + characteristic.getUuid().toString());
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            // Log.d(TAG,"Characteristic write successful");
        }
        writeInProgress = false;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // Stop if the device doesn't have the UART service.
        if (!parseUUIDs(scanRecord).contains(UART_UUID)) {
            return;
        }
        // Notify registered callbacks of found device.
        notifyOnDeviceFound(device);
        // Connect to first found device if required.
        if (connectFirst) {
            // Stop scanning for devices.
            stopScan();
            // Prevent connections to future found devices.
            connectFirst = false;
            // Connect to device.
            gatt = device.connectGatt(context, true, this);
        }
    }

    // Private functions to simplify the notification of all callbacks of a certain event.
    private void notifyOnConnected(BluetoothLeUart uart) {
        mConnected = true;
        mJustConnected = true;
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onConnected(uart);
            }
        }
    }

    private void notifyOnConnectFailed(BluetoothLeUart uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onConnectFailed(uart);
            }
        }
    }

    private void notifyOnDisconnected(BluetoothLeUart uart) {
        mConnected = false;
        mJustDisconnected = true;
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onDisconnected(uart);
            }
        }
    }

    private void notifyOnReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
        mNumPackets += 1;
        String msg = rx.getStringValue(0);
        byte bob2[] = rx.getValue();
        int command_num = 0;//Short.decode(command);
        if (bob2[0] == (byte)'s'){
            try {
                command_num |= (bob2[1]) << 8;
                command_num |= bob2[2] & 0x0ff;
            }catch(ArrayIndexOutOfBoundsException e1){
                mNumBadPackets += 1;
                if (mNumBadPackets%50==0){
                    System.out.println(mNumPackets + " RECEIVED. " + (float)mNumBadPackets/(float)mNumPackets*100 + "% WERE BAD.");
                }
            }
        }
        mGottenPackets += 1;
        if (!mNewPoints.isEmpty()){
            if(Math.abs((float)command_num)!=(float)mNewPoints.get(mNewPoints.size()-1)-1.0){
                mMissedPackets += 1;
            }
        }
        String prefix = msg.substring(0, 1);
        //System.out.println("Received msg type: " + prefix);
        mNewPoints.add(Math.abs((float)command_num));
        //System.out.println("Received datapoint: " + Integer.toString(command_num));
        for (Callback cb : callbacks.keySet()) {

            if (cb != null ) {
                cb.onReceive(uart, rx);
            }
        }
    }

    private void notifyOnDeviceFound(BluetoothDevice device) {
        mJustFoundDevice = true;
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                //cb.onDeviceFound(device);
            }
        }
    }

    private void notifyOnDeviceInfoAvailable() {
        mJustGotDeviceInfo = true;
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onDeviceInfoAvailable();
            }
        }
    }

    // Notify callbacks of connection failure, and reset connection state.
    private void connectFailure() {
        rx = null;
        tx = null;
        notifyOnConnectFailed(this);
    }

    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    //   http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the SO thread to manually parse advertisement data.
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            //Log.e(LOG_TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }
        return uuids;
    }
    public void showConnectedToast(Context activity){
        Toast.makeText(activity, "Connected", Toast.LENGTH_SHORT).show();
        mJustConnected = false;
    }
    public void showDisconnectedToast(Context activity){
        Toast.makeText(activity, "Disconnected", Toast.LENGTH_SHORT).show();
        mJustDisconnected = false;
    }

    public void showFoundDeviceToast(Context activity){
        Toast.makeText(activity, "Found device.  Waiting for connection...", Toast.LENGTH_SHORT).show();
        mJustFoundDevice = false;
    }
    public void showGotDeviceInfoToast(Context activity){
        Toast.makeText(activity, getDeviceInfo(), Toast.LENGTH_SHORT).show();
        mJustGotDeviceInfo = false;
    }
    public void showConnectionFailedToast(Context activity){
        Toast.makeText(activity, "Connection failed", Toast.LENGTH_SHORT).show();
        mJustFailedConnection = false;
    }
    public boolean hasNewPoint(){
        return mNewPoints.size()!=0;
    }

    public float newPoint(){
        //must verify if we have new point first
        return (float)mNewPoints.remove(0);
    }

    public void connectIfNeeded(Context activity){
        if (!mConnected) {
            connectFirstAvailable();
            Toast.makeText(activity, "Scanning for devices...", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean justConnected(){
        return mJustConnected;
    }
    public boolean justDisconnected(){
        return mJustDisconnected;
    }
    public boolean justFoundDevice(){
        return mJustFoundDevice;
    }

    public boolean justGotDeviceInfo(){
        return mJustGotDeviceInfo;
    }
    public boolean justFailedConnection(){
        return mJustFailedConnection;
    }
}
