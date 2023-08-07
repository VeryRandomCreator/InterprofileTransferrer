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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * The fragment for the server. This fragment allows the user to customize and start the server service.
 */
public class ServerFragment extends Fragment {
    /**
     * Default values
     */
    public static final int DEFAULT_PORT = 3333;
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 512;

    /**
     * The key for the storing the server state in shared preferences
     */
    public static final String SERVER_STATE_SHARED_PREF_KEY = "is_server_running";

    /**
     * Server Data
     */
    private Uri outputDirectoryUri;

    /**
     * Fragment views
     */
    private FloatingActionButton selectDirectoryBtn;
    private FloatingActionButton openServerBtn;

    private EditText setPortEdtTxt;
    private TextView selectedDirectoryTxt;
    private EditText receiveBufferSizeEdtTxt;

    /**
     * The {@link ActivityResultLauncher} for the directory chooser
     */
    private ActivityResultLauncher<Uri> launchDirectoryChooser;

    /**
     * The {@link ActivityResultLauncher} to launch the permission request
     */
    private ActivityResultLauncher<String> requestPermissionLauncher;

    /**
     * The parent activity's context
     */
    private Context context;


    /**
     * Required empty constructor
     */
    public ServerFragment() {

    }

    /**
     * Initializes the view fields and {@link ActivityResultLauncher}s. The <a href="https://developer.android.com/training/basics/intents/result">android developer website</a> provides useful information about getting results after launching activities, as applied here.
     *
     * @param inflater default param
     * @param container default param
     * @param savedInstanceState default param
     * @return the server fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.fragment_server, container, false);
        selectDirectoryBtn = fragment.findViewById(R.id.selectDirectoryBtn);
        openServerBtn = fragment.findViewById(R.id.openServerBtn);
        setPortEdtTxt = fragment.findViewById(R.id.setPortEdtTxt);
        selectedDirectoryTxt = fragment.findViewById(R.id.selectedDirectoryTxt);
        receiveBufferSizeEdtTxt = fragment.findViewById(R.id.receiveBufferSizeEdtTxt);
        launchDirectoryChooser = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), result -> {
            outputDirectoryUri = result;
            if (outputDirectoryUri == null) {
                return;
            }
            selectedDirectoryTxt.setText(getDirectoryName());
            vibrate(context);
        });
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isAllowed -> {
            if (!isAllowed) {
                sendErrorToast(context, PERMISSION_EXCEPTION + NOTIFICATION_PERMISSION_EXCEPTION);
            } else {
                sendToast(context, "Notifications permission successfully granted! You may now start the server!");
            }
            vibrate(context);
        });
        init();
        return fragment;
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
     * A method to restore the saved instance state. Checks if the bundle is not null, and restores the state of the views.
     *
     * @param savedInstanceState default param
     */
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            outputDirectoryUri = savedInstanceState.getParcelable("output_directory_uri");
            setPortEdtTxt.setText(String.valueOf(savedInstanceState.getInt("port")));
            receiveBufferSizeEdtTxt.setText(String.valueOf(savedInstanceState.getInt("buffer_size")));

            if (outputDirectoryUri != null) {
                selectedDirectoryTxt.setText(getDirectoryName());
            }
        }
    }

    /**
     * A method to save the instance state
     *
     * @param outState default param
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable("output_directory_uri", outputDirectoryUri);
        try {
            outState.putInt("port", Integer.parseInt(setPortEdtTxt.getText().toString()));
        } catch (NumberFormatException e) {
            outState.putInt("port", ServerFragment.DEFAULT_PORT);
        }
        try {
            outState.putInt("buffer_size", Integer.parseInt(receiveBufferSizeEdtTxt.getText().toString()));
        } catch (NumberFormatException e) {
            outState.putInt("buffer_size", ServerFragment.DEFAULT_RECEIVE_BUFFER_SIZE);
        }
    }

    /**
     * Initializes the click listeners. The <a href="https://developer.android.com/training/basics/intents/result">android developer website</a> provides useful information about getting results after launching activities, as applied here.
     */
    public void init() {
        selectDirectoryBtn.setOnClickListener(view -> {
            vibrate(context);
            launchDirectoryChooser.launch(null);
        });
        openServerBtn.setOnClickListener(view -> {
            vibrate(context);
            openServer();
        });
    }

    /**
     * Receives the value in the port input area, confirms it is valid, and returns it. If the value is invalid {@link MainActivity#sendErrorToast(Context, String)} will be called
     * @return An integer representing the port, or -1, meaning there was a problem while retrieving the port
     */
    public int getPort() {
        String portTxt = setPortEdtTxt.getText().toString();
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
     * Attempts to start the server service. It first goes through a series of checks to make sure the required values (buffer, port, permission) are set. It also confirms that the client service is not already running, by checking the saved preferences.
     *
     * The <a href="https://developer.android.com/training/data-storage/shared-preferences#java">android developer website</a> provides useful information about reading and writing data into shared preferences.
     */
    public void openServer() {
        if (shouldLaunchNotificationPermission()) {
            return;
        }
        int port = getPort();
        if (port == -1) {
            return;
        }
        int buffer = getBufferSize();
        if (buffer == -1) {
            return;
        }
        if (outputDirectoryUri == null) {
            sendErrorToast(context, DIRECTORY_EXCEPTION + DIRECTORY_NOT_SELECTED_EXCEPTION);
            return;
        }
        Intent serverServiceIntent = new Intent(context, ServerService.class);
        serverServiceIntent.putExtra("port", port);
        serverServiceIntent.putExtra("buffer", buffer);
        serverServiceIntent.putExtra("output_dir_uri", outputDirectoryUri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SAVED_PREFERENCES_KEY, Context.MODE_PRIVATE);
            boolean isRunning = sharedPreferences.getBoolean(SERVER_STATE_SHARED_PREF_KEY, false);
            if (!isRunning) {
                context.startForegroundService(serverServiceIntent);
            } else {
                MainActivity.sendErrorToast(context, SERVICE_EXCEPTION + SERVER_SERVICE_RUNNING_EXCEPTION);
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
        String bufferSizeTxt = receiveBufferSizeEdtTxt.getText().toString();
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
            bufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;
        }
        return bufferSize;
    }

    /**
     * Sets all of the Views in fields to null to avoid problems while closing.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        selectDirectoryBtn = null;
        openServerBtn = null;
        setPortEdtTxt = null;
        selectedDirectoryTxt = null;
        receiveBufferSizeEdtTxt = null;
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