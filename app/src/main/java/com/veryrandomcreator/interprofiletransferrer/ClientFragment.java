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

import static com.veryrandomcreator.interprofiletransferrer.MainActivity.*;
import static com.veryrandomcreator.interprofiletransferrer.ServerFragment.DEFAULT_PORT;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * The fragment for the client. This client all*ows the user to customize and start the client service.
 */
public class ClientFragment extends Fragment {
    /**
     * The default buffer size while reading the file from storage
     */
    public static int DEFAULT_READ_BUFFER_SIZE = 512;

    /**
     * The key for the storing the client state in shared preferences
     */
    public static final String CLIENT_STATE_SHARED_PREF_KEY = "is_client_running";

    /**
     * The fragment's views
     */
    private FloatingActionButton sendFileBtn;
    private FloatingActionButton selectFileBtn;
    private EditText setConnectPortEdtTxt;
    private EditText sendBufferSizeEdtTxt;
    private TextView selectedFileTxt;

    /**
     * The {@link ActivityResultLauncher} for the file chooser
     */
    private ActivityResultLauncher<String[]> launchFileChooser;

    /**
     * The {@link ActivityResultLauncher} to launch the permission request
     */
    private ActivityResultLauncher<String> requestPermissionLauncher;

    /**
     * The parent activity's context
     */
    private Context context;

    /**
     * Transferring data
     */
    private Uri selectedFileUri;
    private String name;
    private int size;

    /**
     * Required empty constructor
     */
    public ClientFragment() {

    }

    /**
     * Initializes the {@link ActivityResultLauncher}s.
     * The <a href="https://developer.android.com/training/basics/intents/result">android developer website</a> provides useful information about getting results after launching activities, as applied here.
     * The <a href="https://developer.android.com/training/secure-file-sharing/retrieve-info">android developer website</a> also contains useful code regarding retrieving file information.
     *
     * @param context default param
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        launchFileChooser = registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
            selectedFileUri = result;
            if (selectedFileUri == null) {
                return;
            }
            Cursor cursor = context.getContentResolver().query(result, null, null, null, null);
            int nameColumnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeColumnIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            name = cursor.getString(nameColumnIndex);
            size = cursor.getInt(sizeColumnIndex);

            selectedFileTxt.setText(name);
            cursor.close();
            vibrate(context);
        });
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isAllowed -> {
            if (!isAllowed) {
                sendErrorToast(context, PERMISSION_EXCEPTION + NOTIFICATION_PERMISSION_EXCEPTION);
            } else {
                sendToast(context, "Notifications permission successfully granted! You may now start the client!");
            }
            vibrate(context);
        });
    }

    /**
     * A method to restore the saved instance state. Checks if the bundle is not null, and restores the state of the views.
     *
     * @param savedInstanceState default param
     */
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            selectedFileUri = savedInstanceState.getParcelable("selected_file_uri");
            setConnectPortEdtTxt.setText(String.valueOf(savedInstanceState.getInt("connect_port")));
            sendBufferSizeEdtTxt.setText(String.valueOf(savedInstanceState.getInt("send_buffer_size")));
            size = savedInstanceState.getInt("size");
            name = savedInstanceState.getString("name");

            selectedFileTxt.setText(name);
        }
    }

    /**
     * A method to save the instance state
     *
     * @param outState default param
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable("selected_file_uri", selectedFileUri);
        try {
            outState.putInt("connect_port", Integer.parseInt(setConnectPortEdtTxt.getText().toString()));
        } catch (NumberFormatException e) {
            outState.putInt("connect_port", DEFAULT_PORT);
        }
        try {
            outState.putInt("send_buffer_size", Integer.parseInt(sendBufferSizeEdtTxt.getText().toString()));
        } catch (NumberFormatException e) {
            outState.putInt("send_buffer_size", ClientFragment.DEFAULT_READ_BUFFER_SIZE);
        }
        outState.putInt("size", size);
        outState.putString("name", name);
    }

    /**
     * Initializes the fragment's views.
     *
     * @param inflater default param
     * @param container default param
     * @param savedInstanceState default param
     * @return the client fragment view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.fragment_client, container, false);
        sendFileBtn = fragment.findViewById(R.id.sendFileBtn);
        selectFileBtn = fragment.findViewById(R.id.selectFileBtn);
        setConnectPortEdtTxt = fragment.findViewById(R.id.setConnectPortEdtTxt);
        sendBufferSizeEdtTxt = fragment.findViewById(R.id.readBufferSizeEdtTxt);
        selectedFileTxt = fragment.findViewById(R.id.selectedFileTxt);
        init();
        return fragment;
    }

    /**
     * Establishes the on click listeners
     */
    public void init() {
        selectFileBtn.setOnClickListener(view -> {
            vibrate(context);
            launchFileChooser.launch(new String[]{"*/*"});
        });
        sendFileBtn.setOnClickListener(view -> {
            vibrate(context);
            sendToServer();
        });
    }

    /**
     * Checks if the app has the required notification permission, and launches the request if the app does not.
     * The <a href="https://developer.android.com/training/permissions/requesting">android developer website</a> provides useful information involving checking and requesting permissions from the user.
     *
     * @return Whether or not the app has the notification permission.
     */
    public boolean shouldLaunchNotificationPermission() {
        int result = ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS");
        if (result != 0) {
            requestPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS");
            return true;
        }
        return false;
    }

    /**
     * Attempts to start the client service. It first goes through a series of checks to make sure the required values (buffer, port, permission) are set. It also confirms tha the client service is not already running, by checking the saved preferences.
     *
     * The <a href="https://developer.android.com/training/data-storage/shared-preferences#java">android developer website</a> provides useful information about reading and writing data into shared preferences.
     */
    public void sendToServer() {
        if (shouldLaunchNotificationPermission()) {
            return;
        }
        if (selectedFileUri == null) {
            sendErrorToast(context, FILE_EXCEPTION + FILE_NOT_SELECTED_EXCEPTION);
            return;
        }
        int port = getPort();
        if (port == -1) {
            return;
        }
        int bufferSize = getBufferSize();
        if (bufferSize == -1) {
            return;
        }
        Intent clientServiceIntent = new Intent(context, ClientService.class);
        clientServiceIntent.putExtra("input_dir_uri", selectedFileUri);
        clientServiceIntent.putExtra("buffer_size", bufferSize);
        clientServiceIntent.putExtra("size", size);
        clientServiceIntent.putExtra("name", name);
        clientServiceIntent.putExtra("port", port);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SAVED_PREFERENCES_KEY, Context.MODE_PRIVATE);
            boolean isRunning = sharedPreferences.getBoolean(CLIENT_STATE_SHARED_PREF_KEY, false);
            if (!isRunning) {
                context.startForegroundService(clientServiceIntent);
            } else {
                MainActivity.sendErrorToast(context, SERVICE_EXCEPTION + CLIENT_SERVICE_RUNNING_EXCEPTION);
            }
        } else {
            sendErrorToast(context, VERSION_EXCEPTION + VERSION_INCORRECT_EXCEPTION);
        }
    }

    /**
     * Receives the value in the buffer size input area, confirms it is valid, and returns it. If the value is invalid {@link MainActivity#sendErrorToast(Context, String)} will be called
     * @return An integer representing the buffer size, or -1, meaning there was a problem while retrieving the buffer size.
     */
    public int getBufferSize() {
        String bufferSizeTxt = sendBufferSizeEdtTxt.getText().toString();
        int bufferSize = -1;
        if (!bufferSizeTxt.isEmpty()) {
            try {
                try {
                    int tempBufferSize = Integer.parseInt(bufferSizeTxt);
                    if (tempBufferSize >= 64 && tempBufferSize <= 65535) {
                        bufferSize = tempBufferSize;
                    } else {
                        MainActivity.sendErrorToast(context, BUFFER_SIZE_EXCEPTION + BUFFER_SIZE_RANGE_EXCEPTION);
                    }
                } catch (NumberFormatException ex) {
                    MainActivity.sendErrorToast(context, BUFFER_SIZE_EXCEPTION + BUFFER_SIZE_RANGE_EXCEPTION);
                }
            } catch (NumberFormatException e) {
                MainActivity.sendErrorToast(context, BUFFER_SIZE_EXCEPTION + BUFFER_SIZE_FORMAT_EXCEPTION);
            }
        } else {
            bufferSize = DEFAULT_READ_BUFFER_SIZE;
        }
        return bufferSize;
    }

    /**
     * Receives the value in the port input area, confirms it is valid, and returns it. If the value is invalid {@link MainActivity#sendErrorToast(Context, String)} will be called
     * @return An integer representing the port, or -1, meaning there was a problem while retrieving the port
     */
    public int getPort() {
        String portTxt = setConnectPortEdtTxt.getText().toString();
        int port = -1;
        if (!portTxt.isEmpty()) {
            try {
                try {
                    int tempPort = Integer.parseInt(portTxt);
                    if (tempPort >= 1024 && tempPort <= 65535) {
                        port = tempPort;
                    } else {
                        MainActivity.sendErrorToast(context, PORT_EXCEPTION + PORT_RANGE_EXCEPTION);
                    }
                } catch (NumberFormatException ex) {
                    MainActivity.sendErrorToast(context, PORT_EXCEPTION + PORT_RANGE_EXCEPTION);
                }
            } catch (NumberFormatException e) {
                MainActivity.sendErrorToast(context, PORT_EXCEPTION + PORT_FORMAT_EXCEPTION);
            }
        } else {
            port = DEFAULT_PORT;
        }
        return port;
    }

    /**
     * Sets all of the Views in fields to null to avoid problems while closing.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sendFileBtn = null;
        selectFileBtn = null;
        setConnectPortEdtTxt = null;
        sendBufferSizeEdtTxt = null;
        selectedFileTxt = null;
    }

    /**
     * Provides the {@link MainActivity}'s context to be used in the fragment
     *
     * @param context the context
     */
    public void setContext(Context context) {
        this.context = context;
    }
}

/*
 * Portions of this page are modifications based on work created and shared by the Android Open Source Project and used according to terms described in the Creative Commons 2.5 Attribution License.
 */