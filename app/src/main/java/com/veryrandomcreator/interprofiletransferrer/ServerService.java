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

import static com.veryrandomcreator.interprofiletransferrer.MainActivity.CREATE_FILE_EXCEPTION;
import static com.veryrandomcreator.interprofiletransferrer.MainActivity.DIRECTORY_EXCEPTION;
import static com.veryrandomcreator.interprofiletransferrer.MainActivity.DIRECTORY_PARSE_EXCEPTION;
import static com.veryrandomcreator.interprofiletransferrer.MainActivity.SAVED_PREFERENCES_KEY;
import static com.veryrandomcreator.interprofiletransferrer.MainActivity.SERVER_SERVICE_RUNNING_EXCEPTION;
import static com.veryrandomcreator.interprofiletransferrer.MainActivity.SERVICE_EXCEPTION;
import static com.veryrandomcreator.interprofiletransferrer.ServerFragment.SERVER_STATE_SHARED_PREF_KEY;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service that runs the server.
 */
public class ServerService extends Service {
    /**
     * Port for server to run on
     */
    private int port;

    /**
     * Output directory for files
     */
    private Uri outputDirectoryUri;

    /**
     * Buffer to read data from client
     */
    private int buffer = 512;

    /**
     * Notification information
     */
    public static String SERVER_CHANNEL_ID = "20";
    public static int SERVER_NOTIFICATION_ID = 20;
    public static int MAX_PROGRESS_UPDATE_SIZE = 524288;
    private NotificationCompat.Builder serverNotificationBuilder;

    /**
     * Thread to stop server while waiting for client connection
     */
    private AtomicReference<Thread> acceptThread;

    /**
     * Close server action
     */
    public static final String ACTION_CLOSE_SERVER = "com.veryrandomcreator.action.CLOSE_SERVER";

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
     * Start command. Starts the foreground service, and contains the action to close the server. Confirms that the server service is not already running. Marks the client service as running in the app's shared preferences.
     *
     * @param intent  default param
     * @param flags   default param
     * @param startId default param
     * @return default param (success)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_CLOSE_SERVER)) {
                acceptThread.get().interrupt();
                stopForeground(Service.STOP_FOREGROUND_REMOVE);
                stopSelf();
                return Service.START_NOT_STICKY;
            }
        }
        SharedPreferences sharedPreferences = getSharedPreferences(SAVED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        boolean isRunning = sharedPreferences.getBoolean(SERVER_STATE_SHARED_PREF_KEY, false);
        if (isRunning) {
            MainActivity.sendErrorToast(this, SERVICE_EXCEPTION + SERVER_SERVICE_RUNNING_EXCEPTION);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            outputDirectoryUri = intent.getParcelableExtra("output_dir_uri", Uri.class);
        }
        if (outputDirectoryUri == null) {
            MainActivity.sendToast(this, "Error! service problem (uri null)!");
        }
        port = intent.getIntExtra("port", -1);
        if (port == -1) {
            MainActivity.sendToast(this, "Error! service problem (port)!");
        }
        buffer = intent.getIntExtra("buffer", -1);
        if (buffer == -1) {
            MainActivity.sendToast(this, "Error! service problem (buffer)!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            initNotification();
        }
        saveServiceState(true);
        openServer();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Stores whether server service is running in {@link SharedPreferences}
     *
     * The <a href="https://developer.android.com/training/data-storage/shared-preferences#java">android developer website</a> provides useful information about reading and writing data into shared preferences.
     *
     * @param state the state of the server service (a boolean representing whether it is running)
     */
    public void saveServiceState(boolean state) {
        SharedPreferences sharedPref = getSharedPreferences(SAVED_PREFERENCES_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SERVER_STATE_SHARED_PREF_KEY, state);
        editor.apply();
    }

    /**
     * The <a href="https://developer.android.com/develop/ui/views/notifications/build-notification">android developer website</a> provides useful information about creating notifications, and adding actions to notifications, as applied here.
     *
     * Creates the notification to run the server foreground service
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void initNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel(SERVER_CHANNEL_ID, "Interprofile Transferrer Server Channel", NotificationManager.IMPORTANCE_DEFAULT));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serverNotificationBuilder = new NotificationCompat.Builder(this, SERVER_CHANNEL_ID)
                    .setSmallIcon(R.drawable.server_icon)
                    .setContentTitle("Server Running on port " + port)
                    .setContentText("Output directory: " + getDirectoryName() + "\nPending client connection...")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .addAction(new NotificationCompat.Action(R.drawable.server_icon, "Close Server", PendingIntent.getForegroundService(getBaseContext(), 3, new Intent(this, ServerService.class).setAction(ACTION_CLOSE_SERVER), PendingIntent.FLAG_IMMUTABLE)));
        }
        serverNotificationBuilder.setProgress(100, 0, false);
        Notification notification = serverNotificationBuilder.build();
        notificationManager.notify(SERVER_NOTIFICATION_ID, notification);
        startForeground(SERVER_NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveServiceState(false);
    }

    /**
     * The <a href="https://developer.android.com/develop/ui/views/notifications/build-notification">android developer website</a> provides useful information about creating notifications, as applied here.
     *
     * Creates a thread to open the server.
     * {@link ServerService#acceptThread} is started just before the server thread makes the {@link ServerSocket#accept()} blocking call.
     * The {@link ServerService#acceptThread} closes the main server socket if it is interrupted.
     * An interrupt may occur if commanded to by an action.
     */
    public void openServer() {
        acceptThread = new AtomicReference<>();
        Runnable runnable = () -> {
            try (ServerSocket serverSocket = new ServerSocket()) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                SocketAddress socketAddress = new InetSocketAddress("127.0.0.1", port);
                serverSocket.bind(socketAddress);
                Runnable acceptRunnable = () -> {
                    while (true) {
                        if (Thread.currentThread().isInterrupted()) {
                            try {
                                serverSocket.close();
                                saveServiceState(false);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                };
                acceptThread.set(new Thread(acceptRunnable));
                acceptThread.get().start();

                Socket socket = serverSocket.accept();
                InputStream input = socket.getInputStream();
                StringBuilder receivedSizeStr = new StringBuilder();

                int size;
                byte[] content;
                String name;

                int temp;
                while (true) {
                    temp = input.read();
                    if (((char) temp) == '\n') {
                        size = Integer.parseInt(receivedSizeStr.toString());
                        content = new byte[size];
                        break;
                    }
                    receivedSizeStr.append((char) temp);
                }
                StringBuilder receivedNameStr = new StringBuilder();
                while (true) {
                    temp = input.read();
                    if (((char) temp) == '\n') {
                        name = receivedNameStr.toString();
                        break;
                    }
                    receivedNameStr.append((char) temp);
                }
                ByteArrayOutputStream total = new ByteArrayOutputStream();
                byte[] buffer = new byte[this.buffer];
                int progressUpdateSize = Math.min(this.buffer, MAX_PROGRESS_UPDATE_SIZE);
                int previous = -1;

                int currentLen;
                while (input.read(buffer) != -1) {
                    total.write(buffer);
                    currentLen = total.toByteArray().length;
                    if (currentLen >= (previous + progressUpdateSize)) {
                        int previousProgressPercent = (int) (((float) previous / (float) size) * 100f);
                        int currentProgressPercent = (int) (((float) currentLen / (float) size) * 100f);
                        previous = currentLen;
                        if (currentProgressPercent - previousProgressPercent != 0) {
                            serverNotificationBuilder.setProgress(100, (int) (((float) currentLen / (float) size) * 100f), false);
                            notificationManager.notify(SERVER_NOTIFICATION_ID, serverNotificationBuilder.build());
                        }
                    }
                    if (currentLen == size) {
                        break;
                    }
                }
                content = total.toByteArray();
                serverNotificationBuilder.setProgress(0, 0, false);
                serverNotificationBuilder.setContentText("File received!");
                notificationManager.notify(SERVER_NOTIFICATION_ID, serverNotificationBuilder.build());
                writeToFile(name, content);
                input.close();
                saveServiceState(false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        };
        Thread serverThread = new Thread(runnable);
        serverThread.start();
    }

    /**
     * Gets the name of the output directory, to display the selected directory
     *
     * @return the name of the output directory (not the full path)
     */
    public String getDirectoryName() {
        return outputDirectoryUri.getLastPathSegment().contains(":") ? outputDirectoryUri.getLastPathSegment().split(":")[outputDirectoryUri.getLastPathSegment().split(":").length - 1] : outputDirectoryUri.getLastPathSegment();
    }

    /**
     * Saves the specified content to the server's output directory.
     * Code from <a href="https://medium.com/@vivekvashistha/how-to-save-a-file-in-shared-storage-location-in-android-13-c1e4fdf3d2cb">this</a> source was used to create this method
     *
     * @param name    The name of the file to save to the directory
     * @param content The content of the file
     */
    public void writeToFile(String name, byte[] content) {
        DocumentFile directory = DocumentFile.fromTreeUri(this, outputDirectoryUri);
        if (directory == null) {
            MainActivity.sendErrorToast(this, DIRECTORY_EXCEPTION + DIRECTORY_PARSE_EXCEPTION);
            return;
        }
        DocumentFile file = directory.createFile("*/*", name);
        if (file == null) {
            MainActivity.sendErrorToast(this, DIRECTORY_EXCEPTION + CREATE_FILE_EXCEPTION);
            return;
        }
        ParcelFileDescriptor parcelFileDescriptor;
        try {
            parcelFileDescriptor = this.getContentResolver().openFileDescriptor(file.getUri(), "w");
            FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
            fileOutputStream.write(content);
            fileOutputStream.flush();
            fileOutputStream.close();
            parcelFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/*
 * Portions of this page are modifications based on work created and shared by the Android Open Source Project and used according to terms described in the Creative Commons 2.5 Attribution License.
 */