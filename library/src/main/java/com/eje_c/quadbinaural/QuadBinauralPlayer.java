package com.eje_c.quadbinaural;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;

public class QuadBinauralPlayer {

    private final Context context;
    private final File pd;
    private int pdHandle;
    private MediaPlayer mediaPlayer;

    public QuadBinauralPlayer(Context context) {
        this.context = context;
        pd = new File(context.getFilesDir(), "quad_binaural.pd");
    }

    /**
     * Initialize with default parameter values.
     *
     * @throws IOException
     */
    public void init() throws IOException {
        init(1, true);
    }

    /**
     * You must call this method before any methods.
     *
     * @throws IOException
     */
    public void init(int ticksPerBuffer, boolean restart) throws IOException {

        // Delete old pd file
        if (pd.exists()) {
            pd.delete();
        }

        // Copy pd file
        IoUtils.extractResource(context.getResources().openRawResource(R.raw.quad_binaural), pd.getName(), pd.getParentFile());

        final int sampleRate = AudioParameters.suggestSampleRate();
        final int outChannels = AudioParameters.suggestOutputChannels();
        PdAudio.initAudio(sampleRate, 0, outChannels, ticksPerBuffer, restart);

        mediaPlayer = new MediaPlayer();
    }

    /**
     * Convenient method for opening audio and video file together.
     *
     * @param video Video file that has no audio.
     * @param audio Audio file that has 8-ch binaural tracks.
     *              1-2 for front
     *              3-4 for right
     *              5-6 for back
     *              7-8 for left
     * @throws IOException
     */
    public void open(File video, File audio) throws IOException {

        try {
            setAudio(audio);
            setVideo(video);
        } catch (IOException e) {
            reset();
            throw e;
        }
    }

    /**
     * Set video file.
     *
     * @param video
     * @throws IOException
     */
    public void setVideo(File video) throws IOException {
        mediaPlayer.setDataSource(video.getAbsolutePath());
        mediaPlayer.prepare();
    }

    /**
     * Set video uri. Video can be played from HTTP(S) but audio cannot.
     *
     * @param uri
     * @throws IOException
     */
    public void setVideo(Uri uri) throws IOException {
        mediaPlayer.setDataSource(context, uri);
        mediaPlayer.prepare();
    }

    /**
     * Set video from resource.
     *
     * @param resId
     * @throws IOException
     */
    public void setVideo(int resId) throws IOException {
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId);
        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        afd.close();
        mediaPlayer.prepare();
    }

    /**
     * Set audio file.
     *
     * @param audio
     * @throws IOException
     */
    public void setAudio(File audio) throws IOException {
        pdHandle = PdBase.openPatch(pd);
        PdAudio.startAudio(context);
        PdBase.sendMessage("message", "open", audio.getAbsolutePath());
    }

    /**
     * Reset video and audio file resource. You can reuse player later.
     */
    public void reset() {
        PdBase.closePatch(pdHandle);
        PdAudio.stopAudio();

        mediaPlayer.reset();
    }

    /**
     * You must call this method when player is no longer used.
     */
    public void release() {
        PdAudio.release();
        PdBase.release();

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * Call this to spatialize audio.
     *
     * @param lookAtVector 3-dimensional normalized vector that represents head orientation.
     *                     [x, y, z]
     *                     +x is right
     *                     +y is up (currently not used)
     *                     +z is forward
     */
    public void setLookDirection(float[] lookAtVector) {
        float dist = (float) Math.sqrt(lookAtVector[0] * lookAtVector[0] + lookAtVector[2] * lookAtVector[2]);
        if (dist > 0.001f) {
            float x = lookAtVector[0] / dist;
            float z = lookAtVector[2] / dist;
            PdBase.sendFloat("x", x);
            PdBase.sendFloat("z", z);
        }
    }

    /**
     * Start playing.
     */
    public void start() {
        mediaPlayer.start();

        PdBase.sendFloat("control", 1);
    }

    /**
     * Stop playing.
     */
    public void stop() {
        mediaPlayer.pause();
        mediaPlayer.seekTo(0);

        PdBase.sendFloat("control", 0);
    }

    /**
     * Get a {@code MediaPlayer}.
     *
     * @return MediaPlayer
     */
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    /**
     * Get whether playing or not.
     *
     * @return While playing, return true. Otherwise false.
     */
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
}
