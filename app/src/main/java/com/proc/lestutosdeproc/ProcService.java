package com.proc.lestutosdeproc;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.xml.sax.ContentHandler;

import java.io.BufferedReader;
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

    private ContentHandler handler;
    private final String TAG = "Proc ProcService";

    @Override
    public boolean onStopJob(JobParameters params) {
        //Toast.makeText(this, "Stopping ProcService onStartJob", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Stopping ProcService onStartJob");
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void scheduleJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context, ProcService.class);
        JobInfo jobInbo = new JobInfo.Builder(0, serviceComponent)
                .setMinimumLatency(50000)      // Temps d'attente minimal avant déclenchement
                .setOverrideDeadline(60000)    // Temps d'attente maximal avant déclenchement
                .build();

        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(jobInbo);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onStartJob(JobParameters params) {
        // For debug...
        // Toast.makeText(this, "ProcService is working...", Toast.LENGTH_SHORT).show();

        try {
            // for xiaomi, ask for automatic boot
            if (Build.BRAND.equalsIgnoreCase("xiaomi")) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            // request no battery restriction
            PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
            String packageName = "com.proc.lestutosdeproc";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent i = new Intent();
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    i.setData(Uri.parse("package:" + packageName));
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            }


            // End of debug
            Log.d(TAG, "Starting ProcService onStartJob");

            // retrieving last video on peertube.lestutosdeprocessus.fr
            // https://lestutosdeprocessus.fr/get_last_video_on_peertube.php
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            String lastvideoonpeertube = null;
            HttpURLConnection connection = null;
            try {
                //Create connection
                connection = buildConnectionVariable(new URL("https://lestutosdeprocessus.fr/get_last_video_on_peertube.php"));

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "An error occured during connection : " + responseCode);
                    throw new Exception("Response code not OK : reponse code = " + responseCode);
                }
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
                    Log.i("ProcService", "ERROR while reading local file : " + e.toString());
                }

                if (lastvideoonpeertube != null) {
                    boolean isTheSame = text.toString().equals(lastvideoonpeertube);  // is working
                    if (isTheSame) {
                        // Last video is the same as local, nothing more to do.
                        Log.i(TAG, "Last video is the same on peertube and local");
                        ProcService.scheduleJob(getApplicationContext());
                        return true;
                    }
                }

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

                // Logic to turn on the screen
                if (!powerManager.isInteractive()) { // if screen is not already on, turn it on (get wake_lock for 10 seconds)
                    @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "MH24_SCREENLOCK");
                    wl.acquire(10000);
                    @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl_cpu = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MH24_SCREENLOCK");
                    wl_cpu.acquire(10000);
                }
                Toast.makeText(this, "NEW VIDEO AVAILABLE !", Toast.LENGTH_SHORT).show();

                // create notification
                createNotification(lastvideoonpeertube);
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

            // relaunch service
            ProcService.scheduleJob(getApplicationContext());

        } catch (Exception e) {
            Log.e(TAG, "Global error : " + e.toString());
            // relaunch service
            ProcService.scheduleJob(getApplicationContext());
        }

        return true;
    }

    private void createNotification(String lastvideoonpeertube) {
        NotificationManager mNotificationManagerDebug = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID_DEBUG = "Proc_channel_id";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importanceDebug = NotificationManager.IMPORTANCE_HIGH;
            String NOTIFICATION_CHANNEL_NAME = "Proc_channel_name";
            NotificationChannel notificationChannelDebug = new NotificationChannel(NOTIFICATION_CHANNEL_ID_DEBUG, NOTIFICATION_CHANNEL_NAME, importanceDebug);
            notificationChannelDebug.enableLights(true);
            notificationChannelDebug.setLightColor(Color.RED);
            notificationChannelDebug.enableVibration(true);
            notificationChannelDebug.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mNotificationManagerDebug.createNotificationChannel(notificationChannelDebug);
        }
        final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
        NotificationCompat.Builder mBuilderDebug = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_DEBUG)
                .setSmallIcon(R.drawable.ic_peertube)
                .setContentTitle("Peertube des Tutos de Processus")
                .setContentText("Une nouvelle vidéo est disponible : " + lastvideoonpeertube)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(DEFAULT_VIBRATE_PATTERN)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setLights(Color.WHITE, 2000, 3000)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

        NotificationManager notificationManagerDebug = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intentDebug = new Intent(this, MainActivity.class);
        @SuppressLint("WrongConstant") PendingIntent piDebug = PendingIntent.getActivity(this, 0, intentDebug, Intent.FLAG_ACTIVITY_NEW_TASK);
        mBuilderDebug.setContentIntent(piDebug);
        notificationManagerDebug.notify((int) (System.currentTimeMillis() / 1000), mBuilderDebug.build());
    }

    private HttpURLConnection buildConnectionVariable(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Language", "fr-FR");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }
}
