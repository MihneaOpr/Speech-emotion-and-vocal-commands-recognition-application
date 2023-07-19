package com.example.applicenta;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 5;
    private static final int RECORDER_SAMPLE_RATE = 44000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private ImageView emojiEmotionImageView;
    private ProgressBar progressBarNeutrality;

    private ProgressBar progressBarCalmness;
    private ProgressBar progressBarHappiness;
    private ProgressBar progressBarSadness;
    private ProgressBar progressBarAnger;
    private ProgressBar progressBarFear;
    private ProgressBar progressBarDisgust;
    private ProgressBar progressBarSurprised;

    private TextView textViewNeutrality;
    private TextView textViewCalmness;
    private TextView textViewHappiness;
    private TextView textViewSadness;
    private TextView textViewAnger;
    private TextView textViewFear;
    private TextView textViewDisgust;
    private TextView textViewSurprised;

    private ProgressBar progressBarAvarii;
    private ProgressBar progressBarClaxon;
    private ProgressBar progressBarFrana;
    private ProgressBar progressBarInainte;
    private ProgressBar progressBarInapoi;
    private ProgressBar progressBarLumini;
    private ProgressBar progressBarRadio;
    private ProgressBar progressBarStart;
    private ProgressBar progressBarStop;

    private TextView textViewAvarii;
    private TextView textViewClaxon;
    private TextView textViewFrana;
    private TextView textViewInainte;
    private TextView textViewInapoi;
    private TextView textViewLumini;
    private TextView textViewRadio;
    private TextView textViewStart;
    private TextView textViewStop;


    private Button startRecordingButton;
    private Button switchButton;
    private LinearLayout page1Layout;
    private LinearLayout page2Layout;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;


    private int currentPage = 1;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchButton = findViewById(R.id.switchButton);
        page1Layout = findViewById(R.id.page1Layout);
        page2Layout = findViewById(R.id.page2Layout);

        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the visibility of the page layouts
                if (page1Layout.getVisibility() == View.VISIBLE) {
                    page1Layout.setVisibility(View.GONE);
                    page2Layout.setVisibility(View.VISIBLE);
                    currentPage = 2; // Update current page identifier when switching to page 2
                } else {
                    page1Layout.setVisibility(View.VISIBLE);
                    page2Layout.setVisibility(View.GONE);
                    currentPage = 1; // Update current page identifier when switching to page 1
                }
            }
        });

        startRecordingButton = findViewById(R.id.startRecordingButton);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_CODE);
        }

        startRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecordingButton.setText("Stop Recording");
                    startRecording();
                } else {
                    startRecordingButton.setText("Start Recording");
                    stopRecording();
                }
            }
        });

        initializeViews();
    }

    private void startRecording() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_CODE);
            return;
        }

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

        String fileName = "recording_" + currentPage + ".wav";
        File outputFile = new File(getFilesDir(), fileName);

        final int bufferSizeInBytes = bufferSize;
        final File finalOutputFile = outputFile;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile(finalOutputFile, bufferSizeInBytes);
            }
        });

        audioRecord.startRecording();
        isRecording = true;
        recordingThread.start();
    }

    private void stopRecording() {
        Log.d(TAG, "stopRecording() called");
        if (isRecording) {
            audioRecord.stop();
            isRecording = false;
            recordingThread = null;

            OkHttpClient client = new OkHttpClient();

            File audioFile = new File(getFilesDir(), "recording_" + currentPage + ".wav");

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.getName(), RequestBody.create(audioFile, MediaType.get("audio/wav; charset=utf-8")))
                    .build();

            Request request = new Request.Builder()
                    .url("http://10.0.2.2:5000/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Handle request failure
                    Log.e(TAG, "Network request failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // Handle request success
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Network request successful");
                        String responseBody = response.body().string();
                        if (currentPage == 1) {
                            try {
                                JSONObject jsonObj = new JSONObject(responseBody);
                                EmotionProbabilities emotions = new EmotionProbabilities(
                                        (Double) jsonObj.get("neutral"),
                                        (Double) jsonObj.get("happy"),
                                        (Double) jsonObj.get("sad"),
                                        (Double) jsonObj.get("angry"),
                                        (Double) jsonObj.get("fearful"),
                                        (Double) jsonObj.get("calm"),
                                        (Double) jsonObj.get("disgust"),
                                        (Double) jsonObj.get("surprised")
                                );
                                showMetrics(emotions);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        } else if (currentPage == 2) {
                            try {
                                JSONObject jsonObj = new JSONObject(responseBody);
                                CommandProbabilities commands = new CommandProbabilities(
                                        (Double) jsonObj.get("avarii"),
                                        (Double) jsonObj.get("claxon"),
                                        (Double) jsonObj.get("frana"),
                                        (Double) jsonObj.get("inainte"),
                                        (Double) jsonObj.get("inapoi"),
                                        (Double) jsonObj.get("lumini"),
                                        (Double) jsonObj.get("radio"),
                                        (Double) jsonObj.get("start"),
                                        (Double) jsonObj.get("stop")
                                );
                                showMetrics2(commands);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        Log.d(TAG, "Response body: " + responseBody);
                    } else {
                        Log.e(TAG, "Network request unsuccessful. Response code: " + response.code());
                    }
                }
            });
        }
    }

    private void writeAudioDataToFile(File outputFile, int bufferSizeInBytes) {
        byte[] buffer = new byte[bufferSizeInBytes];
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (os != null) {
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, bufferSizeInBytes);
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        os.write(buffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
                Log.d(TAG, "Audio file saved: " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class EmotionProbabilities
    {
        private final Double neutrality, happiness, sadness, anger, fear, calmness, disgust, surprised;

        public EmotionProbabilities(Double neutrality, Double happiness, Double sadness, Double anger, Double fear, Double calmness, Double disgust, Double surprised) {
            this.neutrality = neutrality;
            this.happiness = happiness;
            this.sadness = sadness;
            this.anger = anger;
            this.fear = fear;
            this.calmness = calmness;
            this.disgust = disgust;
            this.surprised = surprised;
        }

        public Double getNeutrality() {
            return neutrality;
        }

        public Double getHappiness() {
            return happiness;
        }

        public Double getSadness() {
            return sadness;
        }

        public Double getAnger() {
            return anger;
        }

        public Double getFear() {
            return fear;
        }

        public Double getCalmness() {
            return calmness;
        }

        public Double getDisgust() {
            return disgust;
        }

        public Double getSurprised() {
            return surprised;
        }
    }

    private class CommandProbabilities
    {
        private final Double avarii, claxon, frana, inainte, inapoi, lumini, radio, start, stop;

        public CommandProbabilities(Double avarii, Double claxon, Double frana, Double inainte, Double inapoi, Double lumini, Double radio, Double start, Double stop) {
            this.avarii = avarii;
            this.claxon = claxon;
            this.frana = frana;
            this.inainte = inainte;
            this.inapoi = inapoi;
            this.lumini = lumini;
            this.radio = radio;
            this.start = start;
            this.stop = stop;
        }

        public Double getAvarii() {
            return avarii;
        }

        public Double getClaxon() {
            return claxon;
        }

        public Double getFrana() {
            return frana;
        }

        public Double getInainte() {
            return inainte;
        }

        public Double getInapoi() {
            return inapoi;
        }

        public Double getLumini() {
            return lumini;
        }

        public Double getRadio() {
            return radio;
        }

        public Double getStart() {
            return start;
        }

        public Double getStop() {
            return stop;
        }
    }

    private void showMetrics(EmotionProbabilities emotionProbabilities)
    {
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                textViewNeutrality.setText("Neutrality: " + emotionProbabilities.getNeutrality() + "%");
                textViewHappiness.setText("Happiness: " + emotionProbabilities.getHappiness() + "%");
                textViewSadness.setText("Sadness: " + emotionProbabilities.getSadness() + "%");
                textViewAnger.setText("Anger: " + emotionProbabilities.getAnger() + "%");
                textViewFear.setText("Fear: " + emotionProbabilities.getFear() + "%");
                textViewCalmness.setText("Calmness: " + emotionProbabilities.getCalmness() + "%");
                textViewDisgust.setText("Disgust: " + emotionProbabilities.getDisgust() + "%");
                textViewSurprised.setText("Surprised: " + emotionProbabilities.getSurprised() + "%");

                progressBarNeutrality.setProgress(emotionProbabilities.getNeutrality().intValue());
                progressBarHappiness.setProgress(emotionProbabilities.getHappiness().intValue());
                progressBarSadness.setProgress(emotionProbabilities.getSadness().intValue());
                progressBarAnger.setProgress(emotionProbabilities.getAnger().intValue());
                progressBarFear.setProgress(emotionProbabilities.getFear().intValue());
                progressBarCalmness.setProgress(emotionProbabilities.getCalmness().intValue());
                progressBarDisgust.setProgress(emotionProbabilities.getDisgust().intValue());
                progressBarSurprised.setProgress(emotionProbabilities.getSurprised().intValue());
            }
        });

    }

    private void showMetrics2(CommandProbabilities commandProbabilities)
    {
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                textViewAvarii.setText("Avarii: " + commandProbabilities.getAvarii() + "%");
                textViewClaxon.setText("Claxon: " + commandProbabilities.getClaxon() + "%");
                textViewFrana.setText("Frana: " + commandProbabilities.getFrana() + "%");
                textViewInainte.setText("Inainte: " + commandProbabilities.getInainte() + "%");
                textViewInapoi.setText("Inapoi: " + commandProbabilities.getInapoi() + "%");
                textViewLumini.setText("Lumini: " + commandProbabilities.getLumini() + "%");
                textViewRadio.setText("Radio: " + commandProbabilities.getRadio() + "%");
                textViewStart.setText("Start: " + commandProbabilities.getStart() + "%");
                textViewStop.setText("Stop: " + commandProbabilities.getStop() + "%");


                progressBarAvarii.setProgress(commandProbabilities.getAvarii().intValue());
                progressBarClaxon.setProgress(commandProbabilities.getClaxon().intValue());
                progressBarFrana.setProgress(commandProbabilities.getFrana().intValue());
                progressBarInainte.setProgress(commandProbabilities.getInainte().intValue());
                progressBarInapoi.setProgress(commandProbabilities.getInapoi().intValue());
                progressBarLumini.setProgress(commandProbabilities.getLumini().intValue());
                progressBarRadio.setProgress(commandProbabilities.getRadio().intValue());
                progressBarStart.setProgress(commandProbabilities.getStart().intValue());
                progressBarStop.setProgress(commandProbabilities.getStop().intValue());
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, perform your microphone-related functionality here
        }
    }

    private void initializeViews() {
        emojiEmotionImageView = (ImageView) findViewById(R.id.emojiEmotionImageView);
        progressBarNeutrality = (ProgressBar) findViewById(R.id.progressBarNeutrality);
        progressBarCalmness = (ProgressBar) findViewById(R.id.progressBarCalmness);
        progressBarHappiness = (ProgressBar) findViewById(R.id.progressBarHappiness);
        progressBarSadness = (ProgressBar) findViewById(R.id.progressBarSadness);
        progressBarAnger = (ProgressBar) findViewById(R.id.progressBarAnger);
        progressBarFear = (ProgressBar) findViewById(R.id.progressBarFear);
        progressBarDisgust = (ProgressBar) findViewById(R.id.progressBarDisgust);
        progressBarSurprised = (ProgressBar) findViewById(R.id.progressBarSurprised);

        textViewNeutrality = (TextView) findViewById(R.id.textViewNeutrality);
        textViewCalmness = (TextView) findViewById(R.id.textViewCalmness);
        textViewHappiness = (TextView) findViewById(R.id.textViewHappiness);
        textViewSadness = (TextView) findViewById(R.id.textViewSadness);
        textViewAnger = (TextView) findViewById(R.id.textViewAnger);
        textViewFear = (TextView) findViewById(R.id.textViewFear);
        textViewDisgust = (TextView) findViewById(R.id.textViewDisgust);
        textViewSurprised = (TextView) findViewById(R.id.textViewSurprised);

        progressBarAvarii = (ProgressBar) findViewById(R.id.progressBarAvarii);
        progressBarClaxon = (ProgressBar) findViewById(R.id.progressBarClaxon);
        progressBarFrana = (ProgressBar) findViewById(R.id.progressBarFrana);
        progressBarInainte = (ProgressBar) findViewById(R.id.progressBarInainte);
        progressBarInapoi = (ProgressBar) findViewById(R.id.progressBarInapoi);
        progressBarLumini = (ProgressBar) findViewById(R.id.progressBarLumini);
        progressBarRadio = (ProgressBar) findViewById(R.id.progressBarRadio);
        progressBarStart = (ProgressBar) findViewById(R.id.progressBarStart);
        progressBarStop = (ProgressBar) findViewById(R.id.progressBarStop);

        textViewAvarii = (TextView) findViewById(R.id.textViewAvarii);
        textViewClaxon = (TextView) findViewById(R.id.textViewClaxon);
        textViewFrana = (TextView) findViewById(R.id.textViewFrana);
        textViewInainte = (TextView) findViewById(R.id.textViewInainte);
        textViewInapoi = (TextView) findViewById(R.id.textViewInapoi);
        textViewLumini = (TextView) findViewById(R.id.textViewLumini);
        textViewRadio = (TextView) findViewById(R.id.textViewRadio);
        textViewStart = (TextView) findViewById(R.id.textViewStart);
        textViewStop = (TextView) findViewById(R.id.textViewStop);
    }
}