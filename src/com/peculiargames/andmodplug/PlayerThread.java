/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright © 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright © 2003 Glenn Sanson.
 * MOD player source - Copyright © 2011 Patrick Casey.
 * Additional source - Copyright © 2013 Eric Fortin.
 *
 * This code is distributed under the GNU General Public License
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to:
 * Free Software Foundation, Inc.
 * 675 Mass Ave
 * Cambridge, MA 02139, USA
 *
 *
 * Artwork:
 *    Alexis Younes <73lab at free.fr>
 *      (everything but the bubbles)
 *    Amaury Amblard-Ladurantie <amaury at linuxfr.org>
 *      (the bubbles)
 *
 * Soundtrack:
 *    Matthias Le Bidan <matthias.le_bidan at caramail.com>
 *      (the three musics and all the sound effects)
 *
 * Design & Programming:
 *    Guillaume Cottenceau <guillaume.cottenceau at free.fr>
 *      (design and manage the project, whole Perl sourcecode)
 *
 * Java version:
 *    Glenn Sanson <glenn.sanson at free.fr>
 *      (whole Java sourcecode, including JIGA classes
 *             http://glenn.sanson.free.fr/jiga/)
 *
 * Android port:
 *    Pawel Aleksander Fedorynski <pfedor@fuw.edu.pl>
 *    Eric Fortin <videogameboy76 at yahoo.com>
 *    Copyright © Google Inc.
 *
 *          [[ http://glenn.sanson.free.fr/fb/ ]]
 *          [[ http://www.frozen-bubble.org/   ]]
 */

//
// This class is for a separate thread to control the modplug player,
// getting commands from the Activity.
//
// TODO:  Needs more error checking - I ignore the minbuffer size when
//        getting the audio track, LoadMODData() may fail, etc.
//
//    Typical call order:
//
//    PlayerThreadOLD() - to get player instance (in top most activity
//                        that will use music)
//    LoadMODData()     - to call libmodplug's Load() function with the
//                        MOD data
//    or
//    PlayerThreadOLD(moddatabuffer) - to get player instance and load
//                                     data in one call
//    then
//    start()
//
//    Then when changing songs (i.e. new game level or transition to
//    another sub-activity, etc.) 
//
//    PausePlay()
//    UnLoadMod()
//    LoadMODData(newmodfiledata)
//    UnPausePlay()
//    repeat...
//
// *NOTE*
// This class assumes there's only one player thread for a whole
// application (all activities) thus the static lock objects (mPVlock,
// mRDlock) below, and lots of other probably bad coding practice
// below... :-(  
//
// For a multi-Activity application, you can try the TakeOwnership()
// and GiveUpOwnership() calls, e.g. TakeOwnership(this) in an
// Activity's OnCreate(), and then GiveUpOwnership(this) in onPause().
//
//

package com.peculiargames.andmodplug;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Player class for MOD/XM song files (extends Java Thread). Has
 * methods to load song file data, play the song, get information about
 * the song, pause, unpause, etc.
 * 
 * 
 * <p>   <b>Typical call order:</b>
 * <br>  <code>// get player instance (in topmost activity, etc.)
 * <br>  pt = PlayerThread();
 * <br>  pt.LoadMODData();  // load MOD/XM data into player</code>
 * <br><b>or:</b><br>
 *       <code>// get player & load data in one call
 * <br>
 *       pt = PlayerThread(moddatabuffer);</code>
 * <br><b>then:</b><br>
 *       <code>pt.start();  // start thread (playing song)</code>
 * <br>
 * <b>then when changing songs (new game level or transition to another
 *    sub-activity, etc.):</b> 
 * <br>  <code>pt.PausePlay();</code>
 * <br>  <code>pt.UnLoadMod();</code>
 *
 * <br>  <code>pt.LoadMODData(newmodfiledata);</code>
 * <br>  <code>pt.UnPausePlay();</code>
 * <br> <code>// repeat...</code>
 *
 * @author    P.A. Casey (crow) Peculiar-Games.com
 *
 * @version   1.0
 *
 */
public class PlayerThread extends Thread {

  public  final static String VERS      = "1.0";
  private final static String LOGPREFIX = "PLAYERTHREAD";

  // flags for pattern changes
  /**
   * constant for <code>setPatternLoopRange()</code> calls - change to
   * new pattern range immediately
   */
  public final static int PATTERN_CHANGE_IMMEDIATE = 1;
  /**
   * constant for <code>setPatternLoopRange()</code> calls - change to
   * new pattern range after currently playing pattern finishes
   */
  public final static int PATTERN_CHANGE_AFTER_CURRENT = 2;
  /**
   * constant for <code>setPatternLoopRange()</code> calls - change to
   * new pattern range after current range of patterns finishes playing
   */
  public final static int PATTERN_CHANGE_AFTER_GROUP = 3;
  // values for song loop counts
  /**
   * constant for <code>setLoopCount()</code> calls - loop song forever
   */
  public final static int LOOP_SONG_FOREVER = -1;

  //
  // Limit volume volume steps to 8 steps (just an arbitrary decision).
  // There's also a setVolume() method that accepts a float.
  //
  //
  public final static float[] sVolume_floats = {0.0f, 0.125f, 0.25f, 0.375f,
                                                0.5f, 0.625f, 0.75f, 1.0f};

  //
  // Object for lock on PlayerValid check (mostly necessary for passing
  // a single PlayerThread instance among Activities in an Android
  // multi-activity application).
  //
  //
  public static Object sPVlock;

  //
  // Object for lock on ReadData call (to prevent UI thread messing
  // with player thread's GetSoundData() calls).
  //
  //
  public static Object sRDlock;

  //
  // Mark the player as invalid for when an Activity shuts it down, but
  // Android allows a reference to the player to persist.  A better
  // solution is probably to just null out the reference to the
  // PlayerThread object in whichever Activity shuts it down.
  //
  //
  public  boolean mPlayerValid = false;
  private boolean mWaitFlag    = false;
  private boolean mFlushedData = false;
  private boolean mPlaying     = true;
  private boolean mRunning     = true;

  //
  // Android will report the minimum buffer size needed to keep playing
  // audio at our requested rate smoothly.
  //
  //
  private int mMinbuffer;
  private static int mModsize;  // holds the size in bytes of the mod file
  private final static int BUFFERSIZE = 20000;  // the sample buffer size

  // buffer for audio data
  private static short[] mBuffer;  
  private AudioTrack mMytrack;
  private boolean mLoad_ok;

  // for storing info about the MOD file currently loaded
  private String  mModname;
  private int     mNumChannels;
  private int     mRate;
  private int     posWas;
  private boolean songFinished;

  // could probably get rid of this, unneeded when using libmodplug as
  // single-entrant library?!?
  private static byte[] mdunused;

  // start the player in a paused state?
  private boolean mStart_paused;

  private static final int NUM_RATES = 5;
  private final int[] try_rates = {44100, 32000, 22000, 16000, 8000};

  //
  // Ownership code -- for when several activities try to share a
  //                   single mod player instance...
  //
  // This probably needs to be synchronized...
  //
  //
  private Object mOwner;

  public boolean TakeOwnership(Object newowner) {
    if (mOwner == null || mOwner == newowner) {
      mOwner = newowner;
      return true;
    }
    else
      return false;
  }

  public boolean GiveUpOwnership(Object currowner) {
    if (mOwner == null || mOwner == currowner) {
      mOwner = null;
      return true;
    }
    else
      return false;
  }

  public Object GetOwner() {
    return mOwner;
  }

  //*******************************************************************
  // Listener interface for various events
  //*******************************************************************
  // event types
  public static final int EVENT_PLAYER_STARTED = 1;
  public static final int EVENT_PATTERN_CHANGE = 2;
  public static final int EVENT_SONG_COMPLETED = 3;

  // track if player has started (after loading a new mod)
  private static boolean sPlayerStarted;

  // listener user set
  public interface PlayerListener {
    public abstract void onPlayerEvent(int type);
  }

  PlayerListener mPlayerListener;

  public void setPlayerListener(PlayerListener pl) {
    mPlayerListener = pl;
  }

  //*******************************************************************
  // Constructors
  //*******************************************************************
  //
  //  Here's (one of) the constructor(s) -- grabs an audio track and
  //  loads a mod file
  //
  //  MOD file data has already been read in (using a FileStream) by
  //  the caller -- that functionality could probably be included here,
  //  but for now we'll do it this way.
  //
  //  You could use this in the top parent activity (like a game menu)
  //  to create a PlayerThread and load the mod data in one call.
  //
  //
  /**
   * Allocates a MOD/XM/etc. song PlayerThread  
   *
   * The modData argument is a byte[] array with the MOD file preloaded
   * into it. The desiredrate argument is a specifier that attempts to
   * set the rate audio data will play at - will be overridden if the
   * OS doesn't allow that rate. 
   *
   *
   * @param  modData  a byte[] array containing the MOD file data.
   *
   * @param  desired rate rate of playback (e.g. 44100Hz, or 0 for
   *         default rate) for system audio data playback.
   *
   */
  public PlayerThread(byte[] modData, int desiredrate)
  {
    //
    // Just call the regular constructor and then load in the supplied
    // MOD file data.
    //
    //
    this(desiredrate);

    // load the mod file (data) into libmodplug
    mLoad_ok = ModPlug_JLoad(modData, modData.length);

    if (mLoad_ok)
    {
      // get info (name and number of tracks) for the loaded MOD file
      mModname     = ModPlug_JGetName();
      mNumChannels = ModPlug_JNumChannels();
      posWas       = 0;
      songFinished = false;
    }
  }

  //
  //  This method just gets an audio track. The mod file will be loaded
  //  later with a call to LoadMODData().
  //
  //
  /**
   * Allocates a MOD/XM/etc. song PlayerThread  
   *
   * The desiredrate argument is a specifier that attempts to set the
   * rate audio data will play at - will be overridden if the OS
   * doesn't allow that rate.
   * <p>
   *  General call order when using this constructor is:
   *  <br>
   *     pthr = new PlayerThread(0);
   *  <br>
   *      pthr.LoadMODData(modData);
   *  <br>
   *      pthr.start();
   *  

  @param  desiredrate rate of playback (e.g. 44100Hz, or 0 for default
   *      rate) for system audio data playback.
   *  
   */
  public PlayerThread(int desiredrate)
  {
    // no Activity owns this player yet
    mOwner         = null;
    mStart_paused  = false;
    sPlayerStarted = false;

    // try to get the audio track
    if (!GetAndroidAudioTrack(desiredrate))
      return;

    //
    // Set up our audio sample buffer(libmodplug processes the mod file
    // and fills this with sample data).
    //
    // For proper error checking, this should check that BUFFERSIZE is
    // greater than the minbuffer size the audio system reports in the
    // contructors...
    //
    //
    mBuffer      = new short[BUFFERSIZE];
    mPlayerValid = true;
  }

  //
  // Try to get an Android stereo audio track used by the various
  // constructors.
  //
  //
  private boolean GetAndroidAudioTrack(int desiredrate)
  {
    int rateindex = 0;

    //
    // Get a stereo audio track from Android.
    //
    // PACKETSIZE is the amount of data we request from libmodplug,
    // minbuffer is the size Android tells us is necessary to play
    // smoothly for the rate, configuration we want and is a separate
    // buffer the OS handles.
    //
    // Init the track and player for the desired rate, or if none
    // specified, highest possible.
    //
    //
    if (desiredrate == 0)
    {
      boolean success = false;
      while (!success && (rateindex < NUM_RATES))
      {
        try
        {
          mMinbuffer = AudioTrack.getMinBufferSize(try_rates[rateindex],
            AudioFormat.CHANNEL_CONFIGURATION_STEREO,
            AudioFormat.ENCODING_PCM_16BIT);
          mMytrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                    try_rates[rateindex],
                                    AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                                    AudioFormat.ENCODING_PCM_16BIT,
                                    mMinbuffer, AudioTrack.MODE_STREAM);
          // init the Modplug player for this sample rate
          ModPlug_Init(try_rates[rateindex]);
          success = true;
        }
        catch (IllegalArgumentException e)
        {
          rateindex++;
        }
      }
    }
    else
    {
      mMinbuffer = AudioTrack.getMinBufferSize(desiredrate,
        AudioFormat.CHANNEL_CONFIGURATION_STEREO,
        AudioFormat.ENCODING_PCM_16BIT);
      mMytrack = new AudioTrack(AudioManager.STREAM_MUSIC, desiredrate,
                                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                mMinbuffer, AudioTrack.MODE_STREAM);
      // init the Modplug player for this sample rate
      ModPlug_Init(desiredrate);
    }

    if (desiredrate == 0)
      mRate = try_rates[rateindex];
    else
      mRate = desiredrate;

    if (mMytrack == null)
    {
      mPlayerValid = false;
      // couldn't get an audio track so return false to caller
      return false;
    }
    else
    {
      switch(mMytrack.getState())
      {
        case AudioTrack.STATE_INITIALIZED:
           break;
        default:
          mMytrack = new AudioTrack(AudioManager.STREAM_MUSIC, mRate,
                                    AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                                    AudioFormat.ENCODING_PCM_16BIT,
                                    mMinbuffer*2, AudioTrack.MODE_STREAM);
          switch(mMytrack.getState())
          {
            case AudioTrack.STATE_INITIALIZED:
              break;
            default:
              break;
          }
          break;
      }
    }
    // got the audio track!
    return true;
  }

  //
  // Load new mod file data.
  //
  // Call PausePlay( ) if a song is currently playing prior to invoking
  // this method.
  //
  //
  /**
   * Loads MOD/XM,etc. song data for playback.
   *
   * The modData argument is a byte[] array with the MOD containing the song file data. 
   * <p>
   * Example of loading the data:
   * <br>
   * modfileInStream =    getResources().openRawResource(R.raw.coolxmsong);
   * <br>
   * try {
   * <br>
   *   modsize = modfileInStream.read(modData,0, modfileInStream.available());
   * <br>
   * } catch (IOException e) {
   * <br>
   *   e.printStackTrace();
   * <br>
   * }
   *
   * @param  modData  a byte[] array containing the MOD file data.
   *
   */
  public void LoadMODData(byte[] modData) {

    UnLoadMod();
    mdunused = modData;
    mLoad_ok = ModPlug_JLoad(modData, modData.length);

    if (mLoad_ok) {
      mModname     = ModPlug_JGetName();
      mNumChannels = ModPlug_JNumChannels();
      posWas       = 0;
      songFinished = false;
    }

    //
    // Re-init this flag so that an event will be passed to the
    // PlayerListener after the first write() to the AudioTrack - when
    // I assume music will actually start playing...
    //
    //
    synchronized(this) {
      sPlayerStarted = false;
    }
  }

  //
  // This PlayerValid stuff is for multi-activity use, or also
  // Android's Pause/Resume.
  //
  // A better way to deal with it is probably to always stop and join()
  // the PlayerThread in onPause() and allocate a new PlayerThread in
  // onResume()  (or onCreate()??).
  //
  // Check if the player thread is still valid.
  //
  //
  public boolean PlayerValid() {
    // return whether this player is valid
    synchronized(sPVlock) {
      return mPlayerValid;
    }
  }

  //
  // Mark this playerthread as invalid (typically when we're closing
  // down the main Activity).
  //
  //
  public void InvalidatePlayer() {
    synchronized(sPVlock) {
      mPlayerValid = false;
    }
  }

  //
  // The thread's run() call, where the actual sounds get played.
  //
  //
  /**
   * Start playing the MOD/XM song (hopefully it's been previously
   * loaded using <code>LoadMODData()</code> or
   * <code>LoadMODResource()</code> ;)
   *
   */
  public void run()
  {
    boolean pattern_change = false;

    if (mStart_paused)
      mPlaying = false;
    else
      mPlaying = true;

    // main play loop
    mMytrack.play();

    while (mRunning)
    {
      while (mPlaying)
      {
        // pre-load another packet
        synchronized(sRDlock)
        {
          ModPlug_JGetSoundData(mBuffer, BUFFERSIZE);

          if (ModPlug_CheckPatternChange())
            pattern_change = true;
        }

        //
        // Pass a packet of sound sample data to the audio track
        // (blocks until audio track can accept the new data).
        //
        //
        mMytrack.write(mBuffer, 0, BUFFERSIZE);

        //
        // Send player events.
        //
        //
        synchronized(this)
        {
          if (!sPlayerStarted)
          {
            sPlayerStarted = true;
            if (mPlayerListener != null)
            {
              mPlayerListener.onPlayerEvent(EVENT_PLAYER_STARTED);
            }
          }
        }

        synchronized(this)
        {
          if (pattern_change)
          {
            pattern_change = false;

            if (mPlayerListener != null)
              mPlayerListener.onPlayerEvent(EVENT_PATTERN_CHANGE);
          }
        }

        synchronized(this)
        {
          int posNow = getCurrentPos();

          if ((posNow >= posWas) && (posNow < getMaxPos()))
            songFinished = false;

          if (!songFinished && ((posNow < posWas) || (posNow >= getMaxPos())))
          {
            if (mPlayerListener != null)
              mPlayerListener.onPlayerEvent(EVENT_SONG_COMPLETED);

            songFinished = true;
          }

          posWas = posNow;
        }
      }

      // ******************* WAIT CODE ***********************
      synchronized (this)
      {
        if (mWaitFlag)
        {
          try
          {
            wait();

            if (mFlushedData)
            {
              sleep(20);
            }
          }
          catch (Exception e)
          {
            e.getCause().printStackTrace();
          }
        }
      }
      // clear flushed flag
      mFlushedData = false;
    }
    //**********************
    // experimental
    //**********************
    mMytrack.release();
  }

  //
  // MOD file info getters
  /**
   * Get the name of the song.
   *
   * @return the name of the song (from the MOD/XM file header)
   */
  public String getModName() {
    return mModname;
  }

  /**
   * Get the number of channels used in the song (MOD/XM songs
   * typically use from 4 to 32 channels in a pattern, mixed together
   * for awesomeness).
   *                              
   * @return the number of channels the song uses
   */
  public int getNumChannels() {
    return mNumChannels;
  }

  /**
   * Set the file size of the MOD/XM song.
   */
  public static void setModSize(int modsize) {
    mModsize = modsize;
  }

  /**
   * Get the file size of the MOD/XM song.
   *
   * @return the size of the song file
   */
  public int getModSize() {
    return mModsize;
  }

  public int getRate() {
    return mRate;
  }

  /**
   * Pauses playback of the current song.
   */
  public void PausePlay() {
    mPlaying = false;
    //
    // This check is usually not needed before stop()ing the audio
    // track, but seem to get an uninitialized audio track here
    // occasionally, generating an IllegalStateException.
    //
    //
    if (mMytrack.getState() == AudioTrack.STATE_INITIALIZED)
      mMytrack.stop();

    mWaitFlag = true;

    synchronized(this) {
      this.notify();
    }
  }

  /**
   * Resumes playback of the current song.
   */
  public void UnPausePlay() {
    mMytrack.play();
    mPlaying  = true;
    mWaitFlag = false;

    synchronized(this) {
      this.notify();
    }
  }

  public void Flush() {
    if (!mPlaying) {
      mMytrack.flush();
      mFlushedData = true;
    }
  }

  /**
   * Sets playback volume for the MOD/XM player.
   *
   * @param  vol an integer from 0 (sound off) to 255 (full volume)
   *
   */
  public void setVolume(int vol) {
    vol = vol>>5;
    if (vol>7) vol = 7;
    if (vol<0) vol = 0;
    mMytrack.setStereoVolume(sVolume_floats[vol], sVolume_floats[vol]);
  }

  /**
   * Sets playback volume for the MOD/XM player.
   *
   * @param  vol a floating point number from 0.0f (sound off) to 1.0f
   *         (full volume)
   *
   */
  public void setVolume(float vol) {
    if (vol>1.0f) vol = 1.0f;
    if (vol<0) vol = 0;
    mMytrack.setStereoVolume(vol, vol);
  }

  //
  // Startup options.
  //
  //
  public void startPaused(boolean flag) {
    //
    // Set before calling the thread's start() method.  This will cause
    // it to start in paused mode.
    //
    //
    mStart_paused = flag;
  }

  //
  // Closing down code.
  //
  //
  /**
   * Stop the player thread (completely stops it, as opposed to
   * pausing).
   *
   *  <p>Typically the player should then be <code>join()</code>ed to
   *  completely remove the thread from the application's Android
   *  process, and also call <code>CloseLIBMODPLUG()</code> to close
   *  the native player library and de-allocate all resources it used.
   */
  public void StopThread() {
    // stops the thread playing  (see run() above)
    mPlaying = false;
    mRunning = false;
    //
    // This check is usually not needed before stop()ing the audio
    // track, but seem to get an uninitialized audio track here
    // occasionally, generating an IllegalStateException.
    //
    //
    if (mMytrack.getState() == AudioTrack.STATE_INITIALIZED)
      mMytrack.stop();

    mPlayerValid = false;
    mWaitFlag    = false;

    synchronized(this) {
      this.notify();
    }
  }

  /**
   * Close the native internal tracker library (libmodplug) and de-
   * allocate any resources.
   */
  public static void CloseLIBMODPLUG() {
    //ModPlug_JUnload(mdunused, MAXMODSIZE);
    ModPlug_JUnload(mdunused, 0);
    ModPlug_CloseDown();
  }

  //
  // Hack function to modify tempo on the fly.
  //
  //
  /**
   * EXPERIMENTAL method for modifying the song's tempo (+ or -) by
   * <code>mt</code>.
   * @param  mt modifier for the song's "native" tempo (positive values
   *   to increase tempo, negative values to decrease tempo)
   */
  public void modifyTempo(int mt) {
    ModPlug_ChangeTempo(mt);
  }
  /**
   * EXPERIMENTAL method for setting the song's tempo to
   * <code>temp</code>.
   * @param  temp the tempo for the song (overrides song's "native"
   *   tempo)
   */
  public void setTempo(int temp) {
    ModPlug_SetTempo(temp);
  }

  /**
   * EXPERIMENTAL: Get the default tempo from the song's header.
   *                              
   * @return the tempo
   */
  public int getSongDefaultTempo() {
    return ModPlug_GetNativeTempo();
  }
  /**
   * EXPERIMENTAL: Get the current "position" in song
   *                              
   * @return the position
   */
  public int getCurrentPos() {
    return ModPlug_GetCurrentPos();
  }
  /**
   * EXPERIMENTAL: Get the maximum "position" in song
   *                              
   * @return the maximum position
   */
  public int getMaxPos() {
    return ModPlug_GetMaxPos();
  }
  /**
   * EXPERIMENTAL: Get the current order
   *                              
   * @return the order
   */
  public int getCurrentOrder() {
    return ModPlug_GetCurrentOrder();
  }
  /**
   * EXPERIMENTAL: Get the current pattern
   *                              
   * @return the pattern
   */
  public int getCurrentPattern() {
    return ModPlug_GetCurrentPattern();
  }
  /**
   * EXPERIMENTAL: set the current pattern (pattern is changed but
   * plays from current row in pattern).
   *                              
   * @param pattern the new pattern to start playing immediately
   */
  public void setCurrentPattern(int pattern) {
    ModPlug_SetCurrentPattern(pattern);
  }
  /**
   * EXPERIMENTAL: set the next pattern to play after current pattern
   * finishes.
   *                              
   * @param pattern the new pattern to start playing after the current
   *   pattern finishes playing
   */
  public void setNextPattern(int pattern) {
    ModPlug_SetNextPattern(pattern);
  }
  /**
   * EXPERIMENTAL: Get the current row in the pattern
   *                              
   * @return the row
   */
  public int getCurrentRow() {
    return ModPlug_GetCurrentRow();
  }
  /**
   * EXPERIMENTAL: Set log printing flag
   *                              
   * @param flag true to start printing debug information to log
   *   output, false to stop
   */
  public void setLogOutput(boolean flag) {
    ModPlug_LogOutput(flag);
  }
   /**
   * EXPERIMENTAL method to change patterns in a song (playing in
   * PATTERN LOOP mode). Waits for the currently playing pattern to
   * finish.
   * 
   * @param  newpattern the new song pattern to start playing
   *   (repeating) in PATTERN LOOP mode
   */
  public void changePattern(int newpattern) {
    ModPlug_ChangePattern(newpattern);
  }
   /**
   * EXPERIMENTAL method to change song to PATTERN LOOP mode, repeating
   * <code>pattern</code>
   * @param  pattern the song pattern to start playing(repeating) in
   *   PATTERN LOOP mode
   */
  public void repeatPattern(int pattern) {
    ModPlug_RepeatPattern(pattern);
  }
  /**
   * EXPERIMENTAL method to loop song in a group of patterns.
   * @param  from  start of pattern range to play in loop
   * @param  to end of pattern range to play in loop
   * @param  when is a constant flag (PATTERN_CHANGE_IMMEDIATE,
   *   PATTERN_CHANGE_AFTER_CURRENT, PATTERN_CHANGE_AFTER_GROUP) to
   *   signal when the new pattern range should take effect
   */
  public void setPatternLoopRange(int from, int to, int when) {
    ModPlug_SetPatternLoopRange(from, to, when);
  }
  /**
   * EXPERIMENTAL method to loop song the specified number of times.
   * @param  number of times to loop (-1 = forever)
   */
  public void setLoopCount(int loopcount) {
    ModPlug_SetLoopCount(loopcount);
  }
  /**
   * EXPERIMENTAL method to set song to PATTERN LOOP mode, repeating
   * any pattern playing or subsequently set via
   * <code>changePattern()</code>
   * @param  flag true to set PATTERN LOOP mode, false to turn off
   *   PATTERN LOOP mode
   */
  public void setPatternLoopMode(boolean flag) {
    ModPlug_SetPatternLoopMode(flag);
  }
  //
  // Unload the current mod from libmodplug, but make sure to wait
  // until any GetSoundData() call in the player thread has finished.
  //
  //
  /**
   * Unload MOD/XM data previously loaded into the native player
   * library.
   */
  public void UnLoadMod()
  {
    //
    // Since this can/will be called from the UI thread, need to synch
    // and not have a call into libmodplug unloading the file, while a
    // call to GetModData() is also executing in the player thread (see
    // run() above).
    //
    //
    synchronized(sRDlock) {
      //ModPlug_JUnload(mdunused, MAXMODSIZE);
      ModPlug_JUnload(mdunused, 0);
    }
  }

  //
  // Native methods in our JNI libmodplug stub code.
  //
  //
  public static native boolean ModPlug_Init(int rate);
  public native boolean ModPlug_JLoad(byte[] buffer, int size);
  public native String ModPlug_JGetName();
  public native int ModPlug_JNumChannels();
  public native int ModPlug_JGetSoundData(short[] sndbuffer, int datasize);
  public static native boolean ModPlug_JUnload(byte[] buffer, int size);
  public static native boolean ModPlug_CloseDown();

  // HACKS ;-)
  public static native int ModPlug_GetNativeTempo();
  public static native void ModPlug_ChangeTempo(int tempotweak); 
  public static native void ModPlug_SetTempo(int tempo); 
  public static native void ModPlug_ChangePattern(int newpattern);
  public static native void ModPlug_RepeatPattern(int pattern);
  public static native boolean ModPlug_CheckPatternChange();
  public static native void ModPlug_SetPatternLoopMode(boolean flag);

  public native void ModPlug_SetCurrentPattern(int pattern);
  public native void ModPlug_SetNextPattern(int pattern);

  // More info
  public native int ModPlug_GetCurrentPos();
  public native int ModPlug_GetMaxPos();
  public native int ModPlug_GetCurrentOrder();
  public native int ModPlug_GetCurrentPattern();
  public native int ModPlug_GetCurrentRow();

  // FOURBYFOUR
  public static native void ModPlug_SetPatternLoopRange(int from,
                                                        int to,
                                                        int when);
  public static native void ModPlug_SetLoopCount(int loopcount);

  // Log output
  public static native void ModPlug_LogOutput(boolean flag);

  static
  {
    try {
      System.loadLibrary("modplug-"+VERS);
      //System.loadLibrary("modplug");
    }
    catch (UnsatisfiedLinkError ule) {
      Log.e(LOGPREFIX, "WARNING: Could not load libmodplug-"+VERS+".so");
      Log.e(LOGPREFIX, "------ older or differently named libmodplug???");
    }

    //
    // Get lock objects for synchronizing access to playervalid flag and
    // GetSoundData() call.
    //
    //
    sPVlock = new Object();
    sRDlock = new Object();
  }
}
