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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Toast;

/*
TODO: FEATURES
 - Send multiple files by keeping the server side open until the user presses button
 - Open like to git readme with info button
 */
public class MainActivity extends AppCompatActivity {
    /**
     * The number of pages for the viewpager.
     */
    public static final int NUM_OF_PAGES = 2;

    /**
     * Error Code Types
     */
    public static final String PORT_EXCEPTION = "1";
    public static final String SERVER_EXCEPTION = "2";
    public static final String DIRECTORY_EXCEPTION = "3";
    public static final String BUFFER_SIZE_EXCEPTION = "4";
    public static final String VERSION_EXCEPTION = "5";
    public static final String PERMISSION_EXCEPTION = "6";
    public static final String FILE_EXCEPTION = "7";
    public static final String SERVICE_EXCEPTION = "8";

    /**
     * Port Error Codes
     */
    public static final String PORT_FORMAT_EXCEPTION = "1";
    public static final String PORT_RANGE_EXCEPTION = "2";
    public static final String PORT_TAKEN_EXCEPTION = "3";

    /**
     * Server exceptions
     */
    public static final String SERVER_GENERAL_EXCEPTION = "1";

    /**
     * Directory exceptions
     */
    public static final String DIRECTORY_NOT_SELECTED_EXCEPTION = "1";
    public static final String DIRECTORY_PARSE_EXCEPTION = "2";
    public static final String CREATE_FILE_EXCEPTION = "3";

    /**
     * Buffer size exceptions
     */
    public static final String BUFFER_SIZE_FORMAT_EXCEPTION = "1";
    public static final String BUFFER_SIZE_RANGE_EXCEPTION = "2";

    /**
     * Version code exceptions
     */
    public static final String VERSION_INCORRECT_EXCEPTION = "1";

    /**
     * Permission exceptions
     */
    public static final String NOTIFICATION_PERMISSION_EXCEPTION = "1";

    /**
     * File exceptions
     */
    public static final String FILE_NOT_SELECTED_EXCEPTION = "1";

    /**
     * Service exceptions
     */
    public static final String CLIENT_SERVICE_RUNNING_EXCEPTION = "1";
    public static final String SERVER_SERVICE_RUNNING_EXCEPTION = "2";

    /**
     * Position of server and client fragment in {@link MainPagerAdapter}.
     */
    public static final int SERVER_FRAGMENT = 0;
    public static final int CLIENT_FRAGMENT = 1;

    /**
     * Saved
     */
    public static final String SAVED_PREFERENCES_KEY = "com.veryrandomcreator.interprofiletransferrer.PREFERENCE_FILE_KEY";

    /**
     * Fragments
     */
    private ClientFragment clientFragment;
    private ServerFragment serverFragment;

    /**
     * The <a href="https://developer.android.com/guide/navigation/advanced/swipe-view">android developer website</a> provided sample code and useful information for creating a swipe view, as applied here.
     *
     * Default {@link AppCompatActivity#onCreate(Bundle)} method. Initializes fragments, pager, and hides the navigation bar for complete fullscreen.
     * Using {@link View#SYSTEM_UI_FLAG_HIDE_NAVIGATION} flag was referenced from the default Android Studio native c++ template,
     * which uses some of these flags to make the app fullscreen.
     *
     * The idea of using {@link View#setSystemUiVisibility(int)} with {@link View#SYSTEM_UI_FLAG_HIDE_NAVIGATION} was a small snippet from Android Studio's c++ Game Activity template, to make the activity fullscreen.
     *
     * @param savedInstanceState default param
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clientFragment = new ClientFragment();
        clientFragment.setContext(this);
        serverFragment = new ServerFragment();
        serverFragment.setContext(this);

        ViewPager2 mainPager = findViewById(R.id.mainPager);
        FragmentStateAdapter mainPagerAdapter = new MainPagerAdapter(this);
        mainPager.setAdapter(mainPagerAdapter);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    /**
     * Creates a toast containing specified message.
     * The <a href="https://developer.android.com/guide/topics/ui/notifiers/toasts">android developer website</a> provides useful information about creating toasts, as applied here.
     *
     * @param context {@link Context} needed to call {@link Toast#makeText(Context, int, int)}
     * @param message The message to display
     */
    public static void sendToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    /**
     * Vibrates device. Code from a <a href="https://stackoverflow.com/questions/13950338/how-to-make-an-android-device-vibrate-with-different-frequency">stackoverflow form</a> was used for this method
     * 
     * @param context {@link Context} needed to call {@link Context#getSystemService(String)}
     */
    public static void vibrate(Context context) {
        if (context != null) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }

    /**
     * Sends a toast displaying the specified error message.
     * The <a href="https://developer.android.com/guide/topics/ui/notifiers/toasts">android developer website</a> provides useful information about creating toasts, as applied here.
     *
     * @param context {@link Context} needed to call {@link Toast#makeText(Context, int, int)}
     * @param errorCode A concatenated string of an error type, and a specific exception of that type. Ex: "{@link MainActivity#PORT_EXCEPTION} + {@link MainActivity#PORT_FORMAT_EXCEPTION}" (11)
     */
    public static void sendErrorToast(Context context, String errorCode) {
        String errorType = String.valueOf((char) errorCode.getBytes()[0]);
        String specificError = String.valueOf((char) errorCode.getBytes()[1]);

        String toastText = "Errorcode: " + errorCode;

        switch (errorType) {
            case PORT_EXCEPTION:
                switch (specificError) {
                    case PORT_FORMAT_EXCEPTION:
                        toastText = "Port error: Format error! Confirm port syntax (Ex: 3333)";
                        break;
                    case PORT_RANGE_EXCEPTION:
                        toastText = "Port error: Invalid range! Port must be between 1024 and 65535!";
                        break;
                    case PORT_TAKEN_EXCEPTION:
                        toastText = "Port error: Port in use! Please input different port!";
                        break;
                }
                break;
            case SERVER_EXCEPTION:
                if (specificError.equals(SERVER_GENERAL_EXCEPTION)) {
                    toastText = "Server error: Error!";
                }
                break;
            case DIRECTORY_EXCEPTION:
                switch (specificError) {
                    case DIRECTORY_NOT_SELECTED_EXCEPTION:
                        toastText = "Directory error: Directory not selected!";
                        break;
                    case DIRECTORY_PARSE_EXCEPTION:
                        toastText = "Directory error: Directory parse exception!";
                        break;
                    case CREATE_FILE_EXCEPTION:
                        toastText = "Directory error: Problem while creating file!";
                        break;
                }
                break;
            case BUFFER_SIZE_EXCEPTION:
                switch (specificError) {
                    case BUFFER_SIZE_RANGE_EXCEPTION:
                        toastText = "Buffer size error: Buffer size must be from 64 - 65535!";
                        break;
                    case BUFFER_SIZE_FORMAT_EXCEPTION:
                        toastText = "Buffer size error: Format error, confirm positive integer input within proper range (Ex: 1024)";
                        break;
                }
                break;
            case VERSION_EXCEPTION:
                if (specificError.equals(VERSION_INCORRECT_EXCEPTION)) {
                    toastText = "Version error: Incorrect sdk version!";
                }
                break;
            case PERMISSION_EXCEPTION:
                if (specificError.equals(NOTIFICATION_PERMISSION_EXCEPTION)) {
                    toastText = "Permission error: Notification permission required to launch service!";
                }
                break;
            case FILE_EXCEPTION:
                if (specificError.equals(FILE_NOT_SELECTED_EXCEPTION)) {
                    toastText = "File error: File to transfer is not selected!";
                }
                break;
            case SERVICE_EXCEPTION:
                if (specificError.equals(CLIENT_SERVICE_RUNNING_EXCEPTION)) {
                    toastText = "Service error: Client service is already running! (If incorrect, clear app storage)";
                } else if (specificError.equals(SERVER_SERVICE_RUNNING_EXCEPTION)) {
                    toastText = "Service error: Server service is already running! (If incorrect, clear app storage)";
                }
                break;
        }
        vibrate(context);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        toast.show();
    }

    /**
     * Pager adapter for the main viewpager
     */
    private class MainPagerAdapter extends FragmentStateAdapter {
        public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        /**
         * Code from <a href="https://stackoverflow.com/questions/55728719/get-current-fragment-with-viewpager2">stackoverflow</a> was modified to find specific fragments.
         *
         * Detects when there might be changes to the current fragments, allowing the context to be passed to any new fragment instances.
         *
         * @param recyclerView default param (ignored)
         */
        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            ServerFragment serverFragment = (ServerFragment) getSupportFragmentManager().findFragmentByTag("f" + SERVER_FRAGMENT);
            if (serverFragment != null) {
                serverFragment.setContext(getApplicationContext());
            }
            ClientFragment clientFragment = (ClientFragment) getSupportFragmentManager().findFragmentByTag("f" + CLIENT_FRAGMENT);
            if (clientFragment != null) {
                clientFragment.setContext(getApplicationContext());
            }
        }

        /**
         * Default method. Creates/opens a fragment depending on the position.
         *
         * @param position The position of the selected fragment
         * @return The selected {@link Fragment}
         */
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == SERVER_FRAGMENT) {
                serverFragment.onAttach(getApplicationContext());
                return serverFragment;
            } else {
                clientFragment.onAttach(getApplicationContext());
                return clientFragment;
            }
        }

        /**
         * Default method.
         * @return Number of pages in this viewpager
         */
        @Override
        public int getItemCount() {
            return NUM_OF_PAGES;
        }
    }
}

/*
 * Portions of this page are modifications based on work created and shared by the Android Open Source Project and used according to terms described in the Creative Commons 2.5 Attribution License.
 */
