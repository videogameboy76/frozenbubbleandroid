/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright (c) 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright (c) 2003 Glenn Sanson.
 * Additional source - Copyright (c) 2013 Eric Fortin.
 *
 * This code is distributed under the GNU General Public License
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 or 3, as published by the Free Software Foundation.
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
 *    Copyright (c) Google Inc.
 *
 *          [[ http://glenn.sanson.free.fr/fb/ ]]
 *          [[ http://www.frozen-bubble.org/   ]]
 */

/*
 *  This file is derived from the LunarLander.java file which is part of
 *  the Lunar Lander game included with Android documentation.  The
 *  copyright notice for the Lunar Lander game is reproduced below.
 */

/*
 * Copyright (c) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfedor.frozenbubble;

import java.util.Random;

import org.jfedor.frozenbubble.GameScreen.eventEnum;
import org.jfedor.frozenbubble.GameScreen.stateEnum;
import org.jfedor.frozenbubble.GameView.GameThread;
import org.jfedor.frozenbubble.MultiplayerGameView.MultiplayerGameThread;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.efortin.frozenbubble.AccelerometerManager;
import com.efortin.frozenbubble.HomeScreen;
import com.efortin.frozenbubble.ModPlayer;
import com.efortin.frozenbubble.ScrollingCredits;
import com.efortin.frozenbubble.VirtualInput;

public class FrozenBubble extends Activity
  implements GameView.GameListener,
             MultiplayerGameView.GameListener,
             AccelerometerManager.AccelerometerListener {
  /*
   * The following screen orientation definitions were added to
   * ActivityInfo in API level 9.
   */
  public final static int SCREEN_ORIENTATION_SENSOR_LANDSCAPE  = 6;
  public final static int SCREEN_ORIENTATION_SENSOR_PORTRAIT   = 7;
  public final static int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8;
  public final static int SCREEN_ORIENTATION_REVERSE_PORTRAIT  = 9;

  public final static int SOUND_WON     = 0;
  public final static int SOUND_LOST    = 1;
  public final static int SOUND_LAUNCH  = 2;
  public final static int SOUND_DESTROY = 3;
  public final static int SOUND_REBOUND = 4;
  public final static int SOUND_STICK   = 5;
  public final static int SOUND_HURRY   = 6;
  public final static int SOUND_NEWROOT = 7;
  public final static int SOUND_NOH     = 8;
  public final static int SOUND_WHIP    = 9;
  public final static int NUM_SOUNDS    = 10;

  public final static int GAME_NORMAL     = 0;
  public final static int GAME_COLORBLIND = 1;

  public final static int MENU_COLORBLIND_ON  = 1;
  public final static int MENU_COLORBLIND_OFF = 2;
  public final static int MENU_FULLSCREEN_ON  = 3;
  public final static int MENU_FULLSCREEN_OFF = 4;
  public final static int MENU_SOUND_OPTIONS  = 5;
  public final static int MENU_DONT_RUSH_ME   = 6;
  public final static int MENU_RUSH_ME        = 7;
  public final static int MENU_NEW_GAME       = 8;
  public final static int MENU_ABOUT          = 9;
  public final static int MENU_EDITOR         = 10;
  public final static int MENU_TARGET_MODE    = 11;

  public final static int AIM_TO_SHOOT    = 0;
  public final static int POINT_TO_SHOOT  = 1;
  public final static int ROTATE_TO_SHOOT = 2;

  public final static int LOCALE_LOCAL    = 0;
  public final static int LOCALE_LAN      = 1;
  public final static int LOCALE_INTERNET = 2;

  public static int gameLocale = LOCALE_LOCAL;
  public static int myPlayerId = VirtualInput.PLAYER1; 
  public static int numPlayers = 0;

  private static boolean adsOn      = true;
  private static int     collision  = BubbleSprite.MIN_PIX;
  private static boolean compressor = false;
  private static int     difficulty = LevelManager.MODERATE;
  private static boolean dontRushMe = false;
  private static boolean fullscreen = true;
  private static int     gameMode   = GAME_NORMAL;
  private static boolean musicOn    = true;
  private static boolean soundOn    = true;
  private static int     targetMode = POINT_TO_SHOOT;

  public final static String PREFS_NAME   = "frozenbubble";
  public final static String TAG          = "FrozenBubble.java";
  public final static String EDITORACTION = "org.jfedor.frozenbubble.GAME";

  private boolean activityCustomStarted = false;
  private boolean allowUnpause;
  private int     currentOrientation;

  private GameThread mGameThread = null;
  private MultiplayerGameThread mMultiplayerGameThread = null;
  private GameView mGameView = null;
  private MultiplayerGameView mMultiplayerGameView = null;
  private OrientationEventListener myOrientationEventListener = null;
  private ModPlayer myModPlayer = null;

  private final int[] MODlist = {
    R.raw.ambientpower,
    R.raw.ambientlight,
    R.raw.androidrupture,
    R.raw.artificial,
    R.raw.aftertherain,
    R.raw.bluestars,
    R.raw.chungababe,
    R.raw.crystalhammer,
    R.raw.dreamscope,
    R.raw.freefall,
    R.raw.gaeasawakening,
    R.raw.homesick,
    R.raw.ifcrystals,
    R.raw.popcorn,
    R.raw.stardustmemories,
    R.raw.sunshineofthemorningsun,
    R.raw.technostyleiii
  };

  /*
   * Following are ancestor superclass overrides.
   */

  /*
   * (non-Javadoc)
   * @see android.app.Activity#onCreate(android.os.Bundle)
   * This method is called when the activity is started.  The activity
   * may have been reconfigured or the system may have killed the
   * process, after which it regained focus to invoke this method.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    //if (savedInstanceState != null)
    //{
    //  Log.i(TAG, "FrozenBubble.onCreate(...)");
    //}
    //else
    //{
    //  Log.i(TAG, "FrozenBubble.onCreate(null)");
    //}
    super.onCreate(savedInstanceState);
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    /*
     * Allow editor functionalities.
     */
    Intent intent = getIntent();
    try {
      if ((null == intent) || (null == intent.getExtras()) ||
          !intent.getExtras().containsKey("levels"))
        startDefaultGame(intent, savedInstanceState);
      else
        startCustomGame(intent);
    } catch (NullPointerException npe) {
      startDefaultGame(intent, savedInstanceState);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_COLORBLIND_ON,  0, R.string.menu_colorblind_on);
    menu.add(0, MENU_COLORBLIND_OFF, 0, R.string.menu_colorblind_off);
    menu.add(0, MENU_FULLSCREEN_ON,  0, R.string.menu_fullscreen_on);
    menu.add(0, MENU_FULLSCREEN_OFF, 0, R.string.menu_fullscreen_off);
    menu.add(0, MENU_SOUND_OPTIONS,  0, R.string.menu_sound_options);
    menu.add(0, MENU_TARGET_MODE,    0, R.string.menu_target_mode);
    menu.add(0, MENU_DONT_RUSH_ME,   0, R.string.menu_dont_rush_me);
    menu.add(0, MENU_RUSH_ME,        0, R.string.menu_rush_me);
    menu.add(0, MENU_ABOUT,          0, R.string.menu_about);
    menu.add(0, MENU_NEW_GAME,       0, R.string.menu_new_game);
    menu.add(0, MENU_EDITOR,         0, R.string.menu_editor);
    return true;
  }

  /**
   * Invoked when the Activity is finishing or being destroyed by the
   * system.
   */
  @Override
  protected void onDestroy() {
    //Log.i(TAG, "FrozenBubble.onDestroy()");
    super.onDestroy();
    numPlayers = 0;
    gameLocale = LOCALE_LOCAL;
    cleanUp();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      /*
       * The current game was exited, so reset the static game state
       * variables.
       */
      numPlayers = 0;
      gameLocale = LOCALE_LOCAL;
      /*
       * Preserve game information and perform activity cleanup.
       */
      pause();
      if (mGameView != null)
        mGameView.getThread().setRunning(false);
      if (mMultiplayerGameView != null)
        mMultiplayerGameView.getThread().setRunning(false);
      cleanUp();
      /*
       * Create an intent to launch the home screen.
       */
      Intent intent = new Intent(this, HomeScreen.class);
      intent.putExtra("startHomeScreen", true);
      startActivity(intent);
      finish();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onNewIntent(android.content.Intent)
   */
  @Override
  protected void onNewIntent(Intent intent) {
    if (null != intent) {
      if (EDITORACTION.equals(intent.getAction())) {
        cleanUpGameView();
        startCustomGame(intent);
      }
      else if ((numPlayers != 0) && intent.hasExtra("numPlayers")) {
        int newNumPlayers = intent.getIntExtra("numPlayers", 1);

        if (newNumPlayers != numPlayers) {
          cleanUpGameView();
          startDefaultGame(intent, null);
        }
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sp.edit();

    switch (item.getItemId()) {
      case MENU_NEW_GAME:
        newGameDialog();
        return true;
      case MENU_COLORBLIND_ON:
        setMode(GAME_COLORBLIND);
        editor.putInt("gameMode", gameMode);
        editor.commit();
        return true;
      case MENU_COLORBLIND_OFF:
        setMode(GAME_NORMAL);
        editor.putInt("gameMode", gameMode);
        editor.commit();
        return true;
      case MENU_FULLSCREEN_ON:
        fullscreen = true;
        editor.putBoolean("fullscreen", fullscreen);
        editor.commit();
        setFullscreen();
        return true;
      case MENU_FULLSCREEN_OFF:
        fullscreen = false;
        editor.putBoolean("fullscreen", fullscreen);
        editor.commit();
        setFullscreen();
        return true;
      case MENU_SOUND_OPTIONS:
        soundOptionsDialog();
        return true;
      case MENU_ABOUT:
        if (mGameView != null)
          mGameView.getThread().setState(stateEnum.ABOUT);

        if (mMultiplayerGameView != null)
          mMultiplayerGameView.getThread().setState(stateEnum.ABOUT);
        return true;
      case MENU_TARGET_MODE:
        targetOptionsDialog();
        return true;
      case MENU_DONT_RUSH_ME:
        setDontRushMe(true);
        editor.putBoolean("dontRushMe", dontRushMe);
        editor.commit();
        return true;
      case MENU_RUSH_ME:
        setDontRushMe(false);
        editor.putBoolean("dontRushMe", dontRushMe);
        editor.commit();
        return true;
      case MENU_EDITOR:
        startEditor();
        return true;
    }

    return false;
  }

  @Override
  public void onOptionsMenuClosed(Menu menu) {
    super.onOptionsMenuClosed(menu);
    allowUnpause = true;
  }

  /**
   * Invoked when the Activity loses user focus.
   */
  @Override
  protected void onPause() {
    //Log.i(TAG, "FrozenBubble.onPause()");
    super.onPause();
    pause();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    allowUnpause = false;
    menu.findItem(MENU_COLORBLIND_ON ).setVisible(getMode() == GAME_NORMAL);
    menu.findItem(MENU_COLORBLIND_OFF).setVisible(getMode() != GAME_NORMAL);
    menu.findItem(MENU_FULLSCREEN_ON ).setVisible(!fullscreen);
    menu.findItem(MENU_FULLSCREEN_OFF).setVisible(fullscreen);
    menu.findItem(MENU_SOUND_OPTIONS ).setVisible(true);
    menu.findItem(MENU_TARGET_MODE   ).setVisible(true);
    menu.findItem(MENU_DONT_RUSH_ME  ).setVisible(!getDontRushMe());
    menu.findItem(MENU_RUSH_ME       ).setVisible(getDontRushMe());
    return true;
  }

  /**
   * Notification that something is about to happen, to give the
   * Activity a chance to save state.
   * @param outState - A Bundle into which this Activity should save its
   * state.
   */
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    //Log.i(TAG, "FrozenBubble.onSaveInstanceState()");
    /*
     * Just have the View's thread save its state into our Bundle.
     */
    super.onSaveInstanceState(outState);
    saveState();

    if (mGameThread != null)
      mGameThread.saveState(outState);

    if (mMultiplayerGameThread != null)
      mMultiplayerGameThread.saveState(outState);
  }

  @Override
  public void onWindowFocusChanged (boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    allowUnpause = hasFocus;
  }

  /*
   * The following methods are used to retrieve and set the volatile
   * memory game options variables.  They are all static, so are
   * available to all activities within this application while the
   * application is running.
   */

  public synchronized static boolean getAdsOn() {
    return adsOn;
  }

  public synchronized static void setAdsOn(boolean newAdsOn) {
    adsOn = newAdsOn;
  }
  public synchronized static boolean getAimThenShoot() {
    return (targetMode == AIM_TO_SHOOT) || (targetMode == ROTATE_TO_SHOOT);
  }

  public synchronized static int getCollision() {
    return collision;
  }

  public synchronized static void setCollision(int newCollision) {
    collision = newCollision;
  }

  public synchronized static boolean getCompressor() {
    return compressor;
  }

  public synchronized static void setCompressor(boolean newCompressor) {
    compressor = newCompressor;
  }

  public synchronized static int getDifficulty() {
    return difficulty;
  }

  public synchronized static void setDifficulty(int newDifficulty) {
    difficulty = newDifficulty;
  }

  public synchronized static boolean getDontRushMe() {
    return dontRushMe;
  }

  public synchronized static void setDontRushMe(boolean dont) {
    dontRushMe = dont;
  }

  public synchronized static boolean getFullscreen() {
    return fullscreen;
  }

  public synchronized static void setFullscreen(boolean newFullscreen) {
    fullscreen = newFullscreen;
  }

  public synchronized static int getMode() {
    return gameMode;
  }

  public synchronized static void setMode(int newMode) {
    gameMode = newMode;
  }

  public synchronized static boolean getMusicOn() {
    return musicOn;
  }

  public synchronized static void setMusicOn(boolean mo) {
    musicOn = mo;
  }

  public synchronized static boolean getSoundOn() {
    return soundOn;
  }

  public synchronized static void setSoundOn(boolean so) {
    soundOn = so;
  }

  public synchronized static int getTargetMode() {
    return targetMode;
  }

  public synchronized static void setTargetMode(int tm) {
    targetMode = tm;
  }

  /*
   * Following are general utility functions.
   */

  public void cleanUp() {
    if (AccelerometerManager.isListening())
      AccelerometerManager.stopListening();

    if (myOrientationEventListener != null) {
      myOrientationEventListener.disable();
      myOrientationEventListener = null;
    }

    cleanUpGameView();

    if (myModPlayer != null)
      myModPlayer.destroyMusicPlayer();
    myModPlayer = null;
  }

  private void cleanUpGameView() {
    if (mGameView != null)
      mGameView.cleanUp();
    mGameView   = null;
    mGameThread = null;

    if (mMultiplayerGameView != null)
      mMultiplayerGameView.cleanUp();
    mMultiplayerGameView   = null;
    mMultiplayerGameThread = null;
  }

  private int getScreenOrientation() {
    /*
     * The method getOrientation() was deprecated in API level 8.
     *
     * For API level 8 or greater, use getRotation().
     */
    int rotation = getWindowManager().getDefaultDisplay().getOrientation();
    DisplayMetrics dm = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(dm);
    int width  = dm.widthPixels;
    int height = dm.heightPixels;
    int orientation;
    /*
     * The orientation determination is based on the natural orienation
     * mode of the device, which can be either portrait, landscape, or
     * square.
     *
     * After the natural orientation is determined, convert the device
     * rotation into a fully qualified orientation.
     */
    if ((((rotation == Surface.ROTATION_0  ) ||
          (rotation == Surface.ROTATION_180)) && (height > width)) ||
        (((rotation == Surface.ROTATION_90 ) ||
          (rotation == Surface.ROTATION_270)) && (width  > height))) {
      /*
       * Natural orientation is portrait.
       */
      switch(rotation) {
        case Surface.ROTATION_0:
          orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          break;
        case Surface.ROTATION_90:
          orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          break;
        case Surface.ROTATION_180:
          orientation = SCREEN_ORIENTATION_REVERSE_PORTRAIT;
          break;
        case Surface.ROTATION_270:
          orientation = SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
          break;
        default:
          orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          break;              
      }
    }
    else {
      /*
       * Natural orientation is landscape or square.
       */
      switch(rotation) {
        case Surface.ROTATION_0:
          orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          break;
        case Surface.ROTATION_90:
          orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
          break;
        case Surface.ROTATION_180:
          orientation = SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
          break;
        case Surface.ROTATION_270:
          orientation = SCREEN_ORIENTATION_REVERSE_PORTRAIT;
          break;
        default:
          orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
          break;              
      }
    }

    return orientation;
  }

  /**
   * Restore the game options from the saved preferences, and register
   * the device orientation listener to detect orientation changes.
   */
  private void initGameOptions() {
    restoreGamePrefs();

    currentOrientation = getScreenOrientation();
    myOrientationEventListener =
      new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
        @Override
        public void onOrientationChanged(int arg0) {
          currentOrientation = getScreenOrientation();
        }
      };
    if (myOrientationEventListener.canDetectOrientation())
      myOrientationEventListener.enable();
  }

  /**
   * Start a new game and music player.
   */
  public void newGame() {
    if (mGameThread != null)
      mGameThread.newGame();

    if (mMultiplayerGameThread != null) {
      mMultiplayerGameThread.newGame();
      mMultiplayerGameThread.startOpponent();
    }

    playMusic(false);
  }

  private void newGameDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(FrozenBubble.this);
    /*
     * Set the dialog title.
     */
    builder.setTitle(R.string.menu_new_game)
    /*
     * Set the action buttons.
     */
    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        // User clicked OK.  Start a new game.
        newGame();
      }
    })
    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        // User clicked Cancel.  Do nothing.
      }
    });
    builder.create();
    builder.show();
  }

  public void onAccelerationChanged(float x, float y, float z) {
    if (mGameThread != null) {
      if (currentOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT)
        x = -x;

      mGameThread.setPosition(20.0f+x*2.0f);
    }

    if (mMultiplayerGameThread != null) {
      if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        x = -y;
      else if (currentOrientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
        x = y;
      else if (currentOrientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT)
        x = -x;

      mMultiplayerGameThread.setPosition(20.0f+x*2.0f);
    }
  }

  public void onGameEvent(eventEnum event) {
    switch (event) {
      case GAME_WON:
        break;

      case GAME_LOST:
        break;

      case GAME_PAUSED:
        saveState();

        if (myModPlayer != null)
          myModPlayer.pausePlay();
        break;

      case GAME_RESUME:
        if (myModPlayer == null)
          playMusic(true);
        else if (allowUnpause)
          myModPlayer.unPausePlay();
        break;

      case LEVEL_START:
        if ((mGameView != null) &&
            (mGameView.getThread().getCurrentLevelIndex() == 0)) {
          /*
           * Destroy the current music player, which will free audio
           * stream resources and allow the system to use them.
           */
          if (myModPlayer != null)
            myModPlayer.destroyMusicPlayer();
          myModPlayer = null;
          /*
           * Clear the game screen and suspend input processing for
           * three seconds.
           *
           * Afterwards,  the "About" screen will be displayed as a
           * backup just in case anything goes awry with displaying
           * the end-of-game credits.  It will be displayed after the
           * user touches the screen when the credits are finished.
           */
          mGameView.clearGameScreen(true, 3000);
          /*
           * Create an intent to launch the activity to display the
           * credits screen.
           */
          Intent intent = new Intent(this, ScrollingCredits.class);
          startActivity(intent);
        }
        else
          playMusic(true);
        break;

      default:
        break;
    }
  }

  /**
   * Pause the game.
   */
  private void pause() {
    if (mGameView != null)
      mGameView.getThread().pause();

    if (mMultiplayerGameView != null)
      mMultiplayerGameView.getThread().pause();

    /*
     * Pause the MOD player.
     */
    if (myModPlayer != null)
      myModPlayer.pausePlay();
  }

  /**
   * This function determines whether a music player instance needs to
   * be created or if one already exists.  Then, based on the current
   * level, the song to play is calculated and loaded.  If desired, the
   * song will start playing immediately, or it can remain paused.
   * @param startPlaying - If <code>true</code>, the song starts playing
   * immediately.  Otherwise it is paused and must be unpaused to start
   * playing.
   */
  private void playMusic(boolean startPlaying)
  {
    int modNow;
    /*
     * Ascertain which song to play.
     */
    if (mGameView != null)
      modNow = mGameView.getThread().getCurrentLevelIndex() % MODlist.length;
    else
    {
      Random rand = new Random();
      modNow = rand.nextInt(MODlist.length - 1);
    }
    /*
     * Determine whether to create a music player or load the song.
     */
    if (myModPlayer == null)
      myModPlayer = new ModPlayer(this, MODlist[modNow],
                                  getMusicOn(), !startPlaying);
    else
      myModPlayer.loadNewSong(MODlist[modNow], startPlaying);
    allowUnpause = true;
  }

  private void restoreGamePrefs() {
    SharedPreferences mConfig = getSharedPreferences(PREFS_NAME,
                                                     Context.MODE_PRIVATE);
    adsOn      = mConfig.getBoolean("adsOn",      true                 );
    collision  = mConfig.getInt    ("collision",  BubbleSprite.MIN_PIX );
    compressor = mConfig.getBoolean("compressor", false                );
    difficulty = mConfig.getInt    ("difficulty", LevelManager.MODERATE);
    dontRushMe = mConfig.getBoolean("dontRushMe", false                );
    fullscreen = mConfig.getBoolean("fullscreen", true                 );
    gameMode   = mConfig.getInt    ("gameMode",   GAME_NORMAL          );
    musicOn    = mConfig.getBoolean("musicOn",    true                 );
    soundOn    = mConfig.getBoolean("soundOn",    true                 );
    targetMode = mConfig.getInt    ("targetMode", POINT_TO_SHOOT       );

    BubbleSprite.setCollisionThreshold(collision);
    setTargetMode(targetMode);
    setTargetModeOrientation();
  }

  /**
   * Save critically important game information.
   */
  public void saveState() {
    if (mGameView != null) {
      /*
       * Allow level editor functionalities.
       */
      SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                  Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = sp.edit();
      /*
       * If the game wasn't launched from the level editor, save the
       * last level played.
       */
      Intent i = getIntent();
      if ((null == i) || !activityCustomStarted) {
        editor.putInt("level", mGameThread.getCurrentLevelIndex());
      }
      else {
        /*
         * The level editor's intent is running.
         */
        editor.putInt("levelCustom", mGameThread.getCurrentLevelIndex());
      }
      editor.commit();
    }
  }

  private void setFullscreen() {
    final int flagFs   = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    final int flagNoFs = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;

    if (fullscreen) {
      getWindow().addFlags(flagFs);
      getWindow().clearFlags(flagNoFs);
    }
    else {
      getWindow().clearFlags(flagFs);
      getWindow().addFlags(flagNoFs);
    }

    if (mGameView != null)
      mGameView.requestLayout();

    if (mMultiplayerGameView != null)
      mMultiplayerGameView.requestLayout();
  }

  private void setTargetModeOrientation() {
    if ((targetMode == ROTATE_TO_SHOOT) &&
        AccelerometerManager.isSupported(getApplicationContext())) {
      AccelerometerManager.startListening(getApplicationContext(),this);
      /*
       * In API level 9, SCREEN_ORIENTATION_SENSOR_PORTRAIT and
       * SCREEN_ORIENTATION_SENSOR_LANDSCAPE were added to ActivityInfo.
       * This application is developed in API level 4, but using these
       * values will be supported correctly in devices with a native API
       * level that implements this functionality.
       *
       * These modes allow the device to display the screen in either
       * normal or reverse portrait mode based on the device orientation
       * reported by the accelerometer hardware.
       *
       * For multiplayer games using rotate to shoot, set the
       * orientation to sensor landscape, and for single player games,
       * set the orientation to sensor portrait.
       */
      if (numPlayers > 1) {
        setRequestedOrientation(SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
      }
      else {
        setRequestedOrientation(SCREEN_ORIENTATION_SENSOR_PORTRAIT);
      }
    }

    if ((targetMode != ROTATE_TO_SHOOT) &&
        AccelerometerManager.isListening()) {
      AccelerometerManager.stopListening();
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
  }

  private void soundOptionsDialog() {
    boolean isCheckedItem[] = {getSoundOn(), getMusicOn()};

    AlertDialog.Builder builder = new AlertDialog.Builder(FrozenBubble.this);
    /*
     * Set the dialog title.
     */
    builder.setTitle(R.string.menu_sound_options)
    /*
     * Specify the list array, the items to be selected by default (null
     * for none), and the listener through which to receive callbacks
     * when items are selected.
     */
    .setMultiChoiceItems(R.array.sound_options_array, isCheckedItem,
                         new DialogInterface.OnMultiChoiceClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                    Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        switch (which) {
          case 0:
            setSoundOn(isChecked);
            editor.putBoolean("soundOn", soundOn);
            editor.commit();
            break;
          case 1:
            setMusicOn(isChecked);
            if (myModPlayer != null) {
              myModPlayer.setMusicOn(isChecked);
            }
            editor.putBoolean("musicOn", musicOn);
            editor.commit();
            break;
        }
      }
    })
    /*
     * Set the action buttons.
     */
    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        // User clicked OK.
      }
    });
    builder.create();
    builder.show();
  }

  /**
   * Method to start a game using levels from the level editor.
   * <p>If the level isn't specified from the editor, then the player
   * selected the option to continue playing from the last level
   * played, so use the last level played instead.
   * @param intent - The intent from the level editor used to start this
   * activity, which contains the custom level data.
   */
  private void startCustomGame(Intent intent) {
    activityCustomStarted = true;
    numPlayers = 1;
    initGameOptions();
    /*
     * Get custom level last played.
     */
    SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                Context.MODE_PRIVATE);
    int startingLevel       = sp    .getInt     ("levelCustom",    0);
    int startingLevelIntent = intent.getIntExtra("startingLevel", -2);
    startingLevel =
      (startingLevelIntent == -2) ? startingLevel : startingLevelIntent;
    mGameView = new GameView(this,
                             intent.getExtras().getByteArray("levels"),
                             startingLevel);
    setContentView(mGameView);
    mGameView.setGameListener(this);
    mGameThread = mGameView.getThread();
    mGameView.requestFocus();
    setFullscreen();
    playMusic(false);
  }

  /**
   * Method to start a game using default levels, if single player game
   * mode was selected.
   * <p>This method is also used to start a multiplayer game.
   * @param intent - The intent used to start this activity.
   * @param savedInstanceState - the bundle of saved state information.
   */
  private void startDefaultGame(Intent intent, Bundle savedInstanceState) {
    /*
     * Initialize the flag to denote this game uses default levels.
     */
    activityCustomStarted = false;
    /*
     * Check if this is a single player or multiplayer game.
     */
    numPlayers = 1;
    gameLocale = LOCALE_LOCAL;
    if (intent != null) {
      if (intent.hasExtra("myPlayerId"))
        myPlayerId = intent.getIntExtra("myPlayerId", VirtualInput.PLAYER1);
      if (intent.hasExtra("numPlayers"))
        numPlayers = intent.getIntExtra("numPlayers", 1);
      if (intent.hasExtra("gameLocale"))
        gameLocale = intent.getIntExtra("gameLocale", LOCALE_LOCAL);
    }
    initGameOptions();
    /*
     * If there is more than one player, launch a multiplayer game.
     * Otherwise start a single player game.
     */
    if (numPlayers > 1) {
      mMultiplayerGameView = new MultiplayerGameView(this,
                                                     myPlayerId,
                                                     gameLocale);
      setContentView(mMultiplayerGameView);
      mMultiplayerGameView.setGameListener(this);
      mMultiplayerGameThread = mMultiplayerGameView.getThread();
      /*
       * Only restore the bundle for a multiplayer game if it was local.
       */
      if ((savedInstanceState != null) && (gameLocale == LOCALE_LOCAL)) {
        int savedPlayers = savedInstanceState.getInt("numPlayers");
        if (savedPlayers == 2) {
          mMultiplayerGameThread.restoreState(savedInstanceState);
        }
      }
      mMultiplayerGameThread.startOpponent();
      mMultiplayerGameView.requestFocus();
    }
    else {
      setContentView(R.layout.activity_frozen_bubble);
      mGameView = (GameView)findViewById(R.id.game);
      mGameView.setGameListener(this);
      mGameThread = mGameView.getThread();
      if (savedInstanceState != null) {
        int savedPlayers = savedInstanceState.getInt("numPlayers");
        if (savedPlayers == 1)
          mGameThread.restoreState(savedInstanceState);
      }
      mGameView.requestFocus();
    }
    setFullscreen();
    playMusic(false);
  }

  /**
   * Starts editor / market with editor's download.
   */
  private void startEditor() {
    Intent i = new Intent();
    /*
     * First try to run the plus version of the level editor.
     */
    i.setClassName("sk.halmi.fbeditplus", 
                   "sk.halmi.fbeditplus.EditorActivity");
    try {
      startActivity(i);
      finish();
    } catch (ActivityNotFoundException e) {
      /*
       * If not found, try to run the normal version.
       */
      i.setClassName("sk.halmi.fbedit", 
                     "sk.halmi.fbedit.EditorActivity");
      try {
        startActivity(i);
        finish();
      } catch (ActivityNotFoundException ex) {
        /*
         * If the user doesn't have the Frozen Bubble Level Editor, take
         * him to the application market.
         */
        try {
          Toast.makeText(getApplicationContext(), 
                         R.string.install_editor, Toast.LENGTH_SHORT).show();
          i = new Intent(Intent.ACTION_VIEW,
                         Uri.parse(
                         "market://search?q=frozen bubble level editor"));
          startActivity(i);
        } catch (Exception exc) {
          /*
           * Damn, you don't have market?
           */
          Toast.makeText(getApplicationContext(), 
                         R.string.market_missing, Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  private void targetOptionsDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(FrozenBubble.this);
    /*
     * Set the dialog title.
     */
    builder.setTitle(R.string.menu_target_mode)
    /*
     * Specify the list array, the item to be selected by default, and
     * the listener through which to receive callbacks when the item is
     * selected.
     */
    .setSingleChoiceItems(R.array.shoot_mode_array, targetMode,
                          new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface builder, int which) {
        switch (which) {
          case 0:
            setTargetMode(AIM_TO_SHOOT);
            break;
          case 1:
            setTargetMode(POINT_TO_SHOOT);
            break;
          case 2:
            setTargetMode(ROTATE_TO_SHOOT);
            break;
        }
        setTargetModeOrientation();
      }
    })
    /*
     * Set the action buttons.
     */
    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface builder, int id) {
        // User clicked OK.
        SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                    Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("targetMode", targetMode);
        editor.commit();
      }
    });

    builder.create();
    builder.show();
  }
}
