/*
 * MIT License
 *
 * Copyright (c) 2023 VeryRandomCreator
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.veryrandomcreator.interprofiletransferrer;

import static com.veryrandomcreator.interprofiletransferrer.ClientFragment.CLIENT_STATE_SHARED_PREF_KEY;
import static com.veryrandomcreator.interprofiletransferrer.MainActivity.SAVED_PREFERENCES_KEY;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;

/**
 * A service that runs the client
 */
public class ClientService extends Service {
    /**
     * The port for the client to connect to
     */
    private int port;

    /**
     * The {@link Uri} of the file to transfer
     */
    private Uri inputFileUri;

    /**
     * The buffer size for the {@link InputStream} reading the file from storage
     */
    private int bufferSize;

    /**
     * The size of the file
     */
    private int size;

    /**
     * The name of the file to be transferred
     */
    private String name;

    /**
     * A placeholder file size which is only accessed if something strangely goes wrong while passing the intent data to service.
     */
    public static final int DEFAULT_FILE_SIZE = 1024;

    /**
     * A placeholder file name which is only accessed if something strangely goes wrong while passing the intent data to service.
     */
    public static final String DEFAULT_FILE_NAME = "null";

    /**
     * Notification information
     */
    private NotificationCompat.Builder clientNotificationBuilder;
    public static String CLIENT_CHANNEL_ID = "21";
    public static int CLIENT_NOTIFICATION_ID = 21;

    /**
     * Required method
     *
     * @param intent default param
     * @return null
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Start command. Starts foreground service and runs client. Marks the client service as running in the app's shared preferences.
     *
     * @param intent  default param
     * @param flags   default param
     * @param startId default param
     * @return default param
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            inputFileUri = intent.getParcelableExtra("input_dir_uri", Uri.class);
        }
        port = intent.getIntExtra("port", -1);
        bufferSize = intent.getIntExtra("buffer_size", -1);
        size = intent.getIntExtra("size", -1);
        name = intent.getStringExtra("name");
        if (port == -1) {
            MainActivity.sendToast(this, "Error! service problem (port)!");
            port = ServerFragment.DEFAULT_PORT;
        }
        if (bufferSize == -1) {
            MainActivity.sendToast(this, "Error! service problem (buffer size)!");
            bufferSize = ClientFragment.DEFAULT_READ_BUFFER_SIZE;
        }
        if (size == -1) {
            MainActivity.sendToast(this, "Error! service problem (input file size)!");
            size = DEFAULT_FILE_SIZE;
        }
        if (name == null) {
            MainActivity.sendToast(this, "Error! service problem (name)!");
            name = DEFAULT_FILE_NAME;
        }
        initNotification();
        saveServiceState(true);
        initClient();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Stores whether client service is running in {@link SharedPreferences}
     *
     * The <a href="https://developer.android.com/training/data-storage/shared-preferences#java">android developer website</a> provides useful information about reading and writing data into shared preferences.
     *
     * @param state the state of the client service (a boolean representing whether it is running)
     */
    public void saveServiceState(boolean state) {
        SharedPreferences sharedPref = getSharedPreferences(SAVED_PREFERENCES_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(CLIENT_STATE_SHARED_PREF_KEY, state);
        editor.apply();
    }

    /**
     * The <a href="https://developer.android.com/develop/ui/views/notifications/build-notification">android developer website</a> provides useful information about creating notifications, as applied here.
     *
     * Creates the notification to run the client foreground service
     */
    public void initNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel(CLIENT_CHANNEL_ID, "Interprofile Transferrer Client Channel", NotificationManager.IMPORTANCE_DEFAULT));
        }
        clientNotificationBuilder = new NotificationCompat.Builder(this, CLIENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.server_icon)
                .setContentTitle("Client Pending Server Connection on Port " + port)
                .setContentText("Input file: " + name)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).setOngoing(true);
        Notification notification =  clientNotificationBuilder.build();
        notificationManager.notify(CLIENT_NOTIFICATION_ID, notification);
        startForeground(CLIENT_NOTIFICATION_ID, notification);
    }

    /**
     * The <a href="https://developer.android.com/develop/ui/views/notifications/build-notification">android developer website</a> provides useful information about creating notifications, as applied here.
     *
     * Starts a thread to open a {@link Socket} to connect to the server. Once connected, it automatically sends the file.
     */
    public void initClient() {
        Thread thread = new Thread(() -> {
            boolean success = false;
            try (Socket socket = new Socket("127.0.0.1", port)) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(String.valueOf(size).getBytes());
                outputStream.write('\n');
                outputStream.write(name.getBytes());
                outputStream.write('\n');
                sendChunks(outputStream, inputFileUri);
                clientNotificationBuilder.setContentTitle("File Sent!");
                clientNotificationBuilder.setContentText("Sent.");
                clientNotificationBuilder.setOngoing(false);
                notificationManager.notify(CLIENT_NOTIFICATION_ID, clientNotificationBuilder.build());
                outputStream.close();
                saveServiceState(false);
                success = true;
            } catch (IOException e) {
                if (e.getCause() instanceof ConnectException) {
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                    clientNotificationBuilder.setContentTitle("Connection Error");
                    clientNotificationBuilder.setContentText("Make sure server is on!");
                    clientNotificationBuilder.setOngoing(false);
                    notificationManager.notify(CLIENT_NOTIFICATION_ID, clientNotificationBuilder.build());
                    saveServiceState(false);
                    return;
                }
            }
            if (!success) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                clientNotificationBuilder.setContentTitle("Error");
                clientNotificationBuilder.setContentText("error.");
                clientNotificationBuilder.setOngoing(false);
                notificationManager.notify(CLIENT_NOTIFICATION_ID, clientNotificationBuilder.build());
                saveServiceState(false);
            }
        });
        thread.start();
    }

    /**
     * Sends the file at the specified {@link Uri} in pieces using the set buffer.
     *
     * @param outputStream The client's {@link Socket}'s {@link OutputStream}
     * @param uri          The {@link Uri} of the file to send
     * @throws IOException from {@link InputStream#read(byte[])}
     */
    public void sendChunks(OutputStream outputStream, Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (bufferSize != -1) {
            byte[] result = new byte[bufferSize];
            while (inputStream.read(result) != -1) {
                outputStream.write(result);
            }
        }
        inputStream.close();
    }
}

/*
 * Portions of this page are modifications based on work created and shared by the Android Open Source Project and used according to terms described in the Creative Commons 2.5 Attribution License.
 */