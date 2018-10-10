package com.example.rgs.audiovideodemo;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.playBtn)
    ImageView playBtn;
    @BindView(R.id.elapsedTimeLabel)
    TextView elapsedTimeLabel;
    @BindView(R.id.remainingTimeLabel)
    TextView remainingTimeLabel;
    @BindView(R.id.seekTimeBar)
    SeekBar seekTimeBar = null;
    @BindView(R.id.volumeBar)
    SeekBar volumeBar = null;
    @BindView(R.id.songArtist)
    TextView songArtist;
    @BindView(R.id.songTitle)
    TextView songTitle;
    @BindView(R.id.albumTitle)
    TextView albumTitle;
    @BindView(R.id.navigation)
    BottomNavigationView navigation;

    // Locals
    TextView navText;
    MediaPlayer mp;
    AudioManager audioManager = null;
    int totalTime;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    navText.setText(R.string.nav_artists);
                    return true;
                case R.id.navigation_dashboard:
                    navText.setText(R.string.nav_songs);
                    return true;
                case R.id.navigation_notifications:
                    navText.setText(R.string.nav_filler);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        // Initialize Variables
        //navText = findViewById(R.id.songTitle);
        setVolumeBar();

        // Media Player
        mp = MediaPlayer.create(this, R.raw.aftermath);
        mp.setLooping(true);
        mp.seekTo(0);
        //mp.setVolume(0.5f,0.5f);
        totalTime = mp.getDuration();

        // Setup Navigation Bottom Bar
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Position Bar
        seekTimeBar.setMax(totalTime);
        seekTimeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    mp.seekTo(progress);
                    seekTimeBar.setProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // Volume Bar
/*        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volumeNum = progress / 100f;
                mp.setVolume(volumeNum, volumeNum);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });*/

        // Thread (Update seektime and time label)
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(mp != null){
                    try {
                        Message msg = new Message();
                        msg.what = mp.getCurrentPosition();
                        seekBarHandler.sendMessage(msg);
                        Thread.sleep(1000);

                    } catch (InterruptedException e){

                    }
                }
            }
        }).start();

    }
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();

    private class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Pause the playback
                mp.pause();
            }
        }

        MediaSessionCompat.Callback callback = new
                MediaSessionCompat.Callback() {
                    @Override
                    public void onPlay() {
                        registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
                    }

                    @Override
                    public void onStop() {
                        unregisterReceiver(myNoisyAudioStreamReceiver);
                    }
                };
    }
    private void setVolumeBar() {
        try {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            volumeBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            volumeBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

            volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            progress, 0);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.playBtn)
    public void setPlayBtn(View v){
        if(v == playBtn){
            playBtn.setActivated(!playBtn.isActivated());
            if(!mp.isPlaying()){
                mp.start();
                //playBtn.setActivated(playBtn.isActivated());
            }else{
                mp.pause();
                playBtn.setActivated(playBtn.isActivated());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
// Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        assert searchManager != null;
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @SuppressLint("HandlerLeak")
    private Handler seekBarHandler = new Handler(){
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            int currentPosition = msg.what;
            // Update seekbar
            seekTimeBar.setProgress(currentPosition);
            // Update times
            String elapsedTime = createTimeLabel(currentPosition);
            elapsedTimeLabel.setText(elapsedTime);

            String remainingTime = createTimeLabel(totalTime-currentPosition);
            remainingTimeLabel.setText("- " + remainingTime);
        }
    };



    private String createTimeLabel(int time) {
        String timeLabel;
        int min = time / 1000 / 60;
        int sec = time / 1000 % 60;

        timeLabel = min + ":";
        if(sec < 10){
            timeLabel += "0";
        }
        timeLabel += sec;

        return timeLabel;
    }

}
