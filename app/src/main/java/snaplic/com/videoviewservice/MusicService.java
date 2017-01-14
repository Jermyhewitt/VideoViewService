package snaplic.com.videoviewservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class MusicService extends Service {
    MediaPlayer player;
    int mAudioSession;
    String test;
    public static int bindCount;

    public static boolean isPlayStatus() {
        return playStatus;
    }

    public static void setPlayStatus(boolean playStatus) {
        MusicService.playStatus = playStatus;
    }

    private static boolean playStatus;


    public boolean isPlaying()
    {
        return playStatus;
    }

    private final IBinder mBinder = new MusicBinder();
    public MusicService() {
    }

    public MediaPlayer getMediaPlayer()
    {
        return player;
    }


    @Override
    public IBinder onBind(Intent intent) {
        String tester=test;
        return mBinder;
    }


    public class MusicBinder extends Binder {
        public MusicService getService() {
            // Return this instance of LocalService so clients can call public methods
            String testCopy=test;
            bindCount++;
            return MusicService.this;

        }
    }





    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "music service starting", Toast.LENGTH_SHORT).show();
        AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            test="This string should not change ever";
        bindCount=0;
            player = new MediaPlayer();
            if (mAudioSession != 0) {
               player.setAudioSessionId(mAudioSession);
            } else {
                mAudioSession = player.getAudioSessionId();
            }


        try
        {
            player.setDataSource(this, Uri.parse("URL of video"));
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.prepareAsync();

            Intent notificationIntent = new Intent(this, MusicService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification = new Notification.Builder(this)
                    .setContentTitle("getText(R.string.notification_title)")
                    .setContentText("getText(R.string.notification_message)")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setTicker("getText(R.string.ticker_text)")
                    .build();

            startForeground(10, notification);


        }
        catch (IOException ex) {
            String message=ex.getMessage();

        }
        catch (Exception e)
        {
            String Message=e.getMessage();
        }
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
               player.start();
                playStatus=true;
            }
        });
        player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Toast.makeText(MusicService.this, "music service starting", Toast.LENGTH_SHORT).show();
            }
        });

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       Toast.makeText(MusicService.this, "Service died", Toast.LENGTH_SHORT).show();
        player.stop();
        player.release();


        stopForeground(true);
        stopSelf();

    }
}
