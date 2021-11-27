package com.mirea.AndreyZ.audiorecord;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_PERMISSION = 100;
    private String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    private boolean isWork;
    private Button startRecordButton;
    private Button stopRecordButton;
    private Button pauseRecordButton;
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private int pushedPause = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startRecordButton = findViewById(R.id.btnStart);
        stopRecordButton = findViewById(R.id.btnStop);
        pauseRecordButton = findViewById(R.id.btnPause);

        startRecordButton.setEnabled(true);
        stopRecordButton.setEnabled(false);
        pauseRecordButton.setEnabled(false);

        mediaRecorder = new MediaRecorder();
        isWork = hasPermissions(this, PERMISSIONS);
        if (!isWork)
        {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSION);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            isWork = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void onRecordStart(View view) {
        try {
            startRecordButton.setEnabled(false);
            stopRecordButton.setEnabled(true);
            pauseRecordButton.setEnabled(true);
            stopRecordButton.requestFocus();
            startRecording();
        }
        catch (Exception e)
        {
            Log.e(TAG, "Caught io exception " + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onPause(View view) {
        startRecordButton.setEnabled(false);
        pauseRecordButton.setEnabled(true);
        stopRecordButton.setEnabled(true);
        startRecordButton.requestFocus();
        pauseRecording();
        if(pushedPause == 0)
        {
            pushedPause = 1;
        }
        else
        {
            pushedPause = 0;
        }
        processAudioFile();
    }

    public void onStopRecord(View view) {
        startRecordButton.setEnabled(true);
        pauseRecordButton.setEnabled(false);
        stopRecordButton.setEnabled(false);
        startRecordButton.requestFocus();
        stopRecording();
        processAudioFile();
    }

    private void startRecording() throws IOException {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            Log.d(TAG, "sd-card success");
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            if (audioFile == null) {
                audioFile = new File(this.getExternalFilesDir(
                        Environment.DIRECTORY_MUSIC), "mirea.3gp");
            }
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void pauseRecording()
    {
        if (pushedPause == 0)
        {
            mediaRecorder.pause();
            Toast.makeText(this, "You paused your recording!",
                    Toast.LENGTH_SHORT).show();
        }
        else
        {
            mediaRecorder.resume();
            Toast.makeText(this, "You resumed your recording!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            Log.d(TAG, "stopRecording");
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            Toast.makeText(this, "You are not recording right now!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void processAudioFile() {
        Log.d(TAG, "processAudioFile");
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();
        values.put(MediaStore.Audio.Media.TITLE, "audio" + audioFile.getName());
        values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
        values.put(MediaStore.Audio.Media.DATA, audioFile.getAbsolutePath());
        ContentResolver contentResolver = getContentResolver();
        Log.d(TAG, "audioFile: " + audioFile.canRead());

        Uri baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri newUri = contentResolver.insert(baseUri, values);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
    }
}