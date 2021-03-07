package com.example.lestutosdeproc;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.proc.lestutosdeproc.R;

import org.xml.sax.ContentHandler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ProcService extends JobService {

    private final String TAG = "Example ProcService";
    private ContentHandler handler;

    @Override
    public boolean onStopJob(JobParameters params) {
        //Toast.makeText(this, "Stopping ProcService onStartJob", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Stopping ProcService onStartJob");
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onStartJob(JobParameters params) {
        //Toast.makeText(this, "Starting ProcService onStartJob", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Starting ProcService onStartJob");

        // retrieving last video on peertube.lestutosdeprocessus.fr
        // https://lestutosdeprocessus.fr/get_last_video_on_peertube.php
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        String lastvideoonpeertube = null;
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL("https://lestutosdeprocessus.fr/get_last_video_on_peertube.php");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Language", "fr-FR");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.flush();
            wr.close();
            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close();
            lastvideoonpeertube = response.toString();
        } catch (Exception e) {
            Log.d("ERROR ProcService", "ERROR : " + e.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        Log.d("ProcService", "Last video on peertube = " + lastvideoonpeertube);


        // test if local file for last video already exists
        String strfile = getApplicationContext().getFilesDir().getAbsolutePath() + "/proc_peertube_last_video.dat";
        File file = new File(strfile);

        boolean exists = file.exists();

        if (exists) {
            // if file exists we check if last video on peertube is the same as local
            StringBuilder text = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line);
                }
                br.close();
            } catch (IOException e) {
                Log.i(TAG, "ERROR while reading local file : " + e.toString());
            }


            boolean isTheSame = false;
            if (lastvideoonpeertube != null) {
                isTheSame = text.toString().equals(lastvideoonpeertube);  // is working
            }
            if (isTheSame) {
                // Last video is the same as local, nothing more to do.
                Log.i(TAG, "Last video is the same on peertube and local");
            } else {
                // Last video and local are not the same, THERE IS A NEW VIDEO AVAILABLE
                // LET'S NOTIFY OUR USER !!!
                Log.i(TAG, "Old video stored lacally : " + text.toString());
                Log.i(TAG, "THERE IS A NEW VIDEO AVAILABLE : " + lastvideoonpeertube);
                // update local file
                try {
                    FileWriter out = new FileWriter(file);
                    out.write(lastvideoonpeertube);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // create notification
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                String NOTIFICATION_CHANNEL_ID = "Proc_channel_id";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    int importance = NotificationManager.IMPORTANCE_LOW;
                    String NOTIFICATION_CHANNEL_NAME = "Proc_channel_name";
                    NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance);
                    notificationChannel.enableLights(true);
                    notificationChannel.setLightColor(Color.RED);
                    notificationChannel.enableVibration(true);
                    notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                    mNotificationManager.createNotificationChannel(notificationChannel);
                }
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_peertube)
                        .setContentTitle("Peertube des Tutos de Processus")
                        .setContentText("Une nouvelle vidéo est disponible : " + lastvideoonpeertube)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Intent intent = new Intent(this, MainActivity.class);
                @SuppressLint("WrongConstant") PendingIntent pi = PendingIntent.getActivity(this, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
                mBuilder.setContentIntent(pi);
                notificationManager.notify((int) (System.currentTimeMillis() / 1000), mBuilder.build());
            }
        } else {
            // if file doesn't exists, let's create it
            try {
                file.createNewFile();
                FileWriter out = new FileWriter(file);
                out.write(lastvideoonpeertube);
                out.close();
                Log.d(TAG, "File proc_peertube_last_video.dat didn't exists so we create a new one with last video : " + lastvideoonpeertube);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IOException while creating local file : " + e.toString());
            } catch (Exception e) {
                Log.e(TAG, "Exception while creating local file : " + e.toString());
            }
        }


        return true;
    }


}
