/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright © 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright © 2003 Glenn Sanson.
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
// This file is derived from the LunarLander.java file which is part of
// the Lunar Lander game included with Android documentation.  The copyright
// notice for the Lunar Lander is reproduced below.
/*
 * Copyright (C) 2007 Google Inc.
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

import org.jfedor.frozenbubble.GameView.GameThread;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.efortin.frozenbubble.AccelerometerManager;
import com.peculiargames.andmodplug.MODResourcePlayer;
import com.peculiargames.andmodplug.PlayerThread;

public class FrozenBubble extends Activity
  implements GameView.GameListener, AccelerometerManager.AccelerometerListener
{
  public final static int SOUND_WON                = 0;
  public final static int SOUND_LOST               = 1;
  public final static int SOUND_LAUNCH             = 2;
  public final static int SOUND_DESTROY            = 3;
  public final static int SOUND_REBOUND            = 4;
  public final static int SOUND_STICK              = 5;
  public final static int SOUND_HURRY              = 6;
  public final static int SOUND_NEWROOT            = 7;
  public final static int SOUND_NOH                = 8;
  public final static int SOUND_WHIP               = 9;
  public final static int NUM_SOUNDS               = 10;

  public final static int GAME_NORMAL              = 0;
  public final static int GAME_COLORBLIND          = 1;

  public final static int MENU_COLORBLIND_MODE_ON  = 1;
  public final static int MENU_COLORBLIND_MODE_OFF = 2;
  public final static int MENU_FULLSCREEN_ON       = 3;
  public final static int MENU_FULLSCREEN_OFF      = 4;
  public final static int MENU_SOUND_OPTIONS       = 5;
  public final static int MENU_DONT_RUSH_ME        = 6;
  public final static int MENU_RUSH_ME             = 7;
  public final static int MENU_NEW_GAME            = 8;
  public final static int MENU_ABOUT               = 9;
  public final static int MENU_EDITOR              = 10;
  public final static int MENU_TARGET_MODE         = 11;

  public final static int AIM_TO_SHOOT             = 0;
  public final static int POINT_TO_SHOOT           = 1;
  public final static int ROTATE_TO_SHOOT          = 2;

  public final static String PREFS_NAME = "frozenbubble";

  private static boolean fullscreen = true;
  private static int     gameMode   = GAME_NORMAL;
  private static boolean musicOn    = true;
  private static boolean soundOn    = true;
  private static boolean dontRushMe = false;
  private static int     targetMode = POINT_TO_SHOOT;

  private GameThread mGameThread;
  private GameView   mGameView;

  private static final String EDITORACTION = "org.jfedor.frozenbubble.GAME";
  private boolean activityCustomStarted = false;
  /*************************************************************************/
  //
  //   MOD Player parameters.
  //
  //
  /*************************************************************************/
  //
  //   Save song number and position in song at onPause() time
  //   (to Preferences).
  //
  //
  public static final String PLAYER_PREFS_NAME = "ModPlayerPrefs";
  public static final String PREFS_SONGNUM     = "SongNum";
  public static final String PREFS_SONGPATTERN = "SongPattern";

  private SharedPreferences mConfig;
  public static final int   DEFAULT_SONG = 0;
  private MODResourcePlayer resplayer    = null;
  private int               mod_now;
  private int               mod_was;

  private final int[] MODlist = {
    R.raw.aftertherain,
    R.raw.ambientlight,
    R.raw.ambientpower,
    R.raw.androidrupture,
    R.raw.artificial,
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
    R.raw.technostyleiii,
    R.raw.worldofpeace
  };

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_COLORBLIND_MODE_ON,  0,
             R.string.menu_colorblind_mode_on);
    menu.add(0, MENU_COLORBLIND_MODE_OFF, 0,
             R.string.menu_colorblind_mode_off);
    menu.add(0, MENU_FULLSCREEN_ON,       0,
             R.string.menu_fullscreen_on);
    menu.add(0, MENU_FULLSCREEN_OFF,      0,
             R.string.menu_fullscreen_off);
    menu.add(0, MENU_SOUND_OPTIONS,       0,
             R.string.menu_sound_options);
    menu.add(0, MENU_TARGET_MODE,         0,
             R.string.menu_target_mode);
    menu.add(0, MENU_DONT_RUSH_ME,        0,
             R.string.menu_dont_rush_me);
    menu.add(0, MENU_RUSH_ME,             0,
             R.string.menu_rush_me);
    menu.add(0, MENU_ABOUT,               0,
             R.string.menu_about);
    menu.add(0, MENU_NEW_GAME,            0,
             R.string.menu_new_game);
    menu.add(0, MENU_EDITOR,              0,
             R.string.menu_editor);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(MENU_SOUND_OPTIONS      ).setVisible(true);
    menu.findItem(MENU_COLORBLIND_MODE_ON ).setVisible(
                  getMode() == GAME_NORMAL);
    menu.findItem(MENU_COLORBLIND_MODE_OFF).setVisible(
                  getMode() != GAME_NORMAL);
    menu.findItem(MENU_FULLSCREEN_ON      ).setVisible(!fullscreen);
    menu.findItem(MENU_FULLSCREEN_OFF     ).setVisible(fullscreen);
    menu.findItem(MENU_TARGET_MODE        ).setVisible(true);
    menu.findItem(MENU_DONT_RUSH_ME       ).setVisible(!getDontRushMe());
    menu.findItem(MENU_RUSH_ME            ).setVisible(getDontRushMe());
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId()) {
    case MENU_NEW_GAME:
      newGameDialog();
      return true;
    case MENU_COLORBLIND_MODE_ON:
      setMode(GAME_COLORBLIND);
      return true;
    case MENU_COLORBLIND_MODE_OFF:
      setMode(GAME_NORMAL);
      return true;
    case MENU_FULLSCREEN_ON:
      fullscreen = true;
      setFullscreen();
      return true;
    case MENU_FULLSCREEN_OFF:
      fullscreen = false;
      setFullscreen();
      return true;
    case MENU_SOUND_OPTIONS:
      soundOptionsDialog();
      return true;
    case MENU_ABOUT:
      mGameView.getThread().setState(GameView.GameThread.STATE_ABOUT);
      return true;
    case MENU_TARGET_MODE:
      targetOptionsDialog();
      return true;
    case MENU_DONT_RUSH_ME:
      setDontRushMe(true);
      return true;
    case MENU_RUSH_ME:
      setDontRushMe(false);
      return true;
    case MENU_EDITOR:
      startEditor();
      return true;
    }
    return false;
  }

  private void setFullscreen()
  {
    if (fullscreen)
    {
      getWindow().addFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
      getWindow().clearFlags(
        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }
    else
    {
      getWindow().clearFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
      getWindow().addFlags(
        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }
    mGameView.requestLayout();
  }

  private void newGameDialog()
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(FrozenBubble.this);
    //
    // Set the dialog title.
    //
    //
    builder.setTitle(R.string.menu_new_game)
    // Set the action buttons
    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        // User clicked OK.  Start a new game and music player.
        mGameThread.newGame();
        newPlayer( true );
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

  private void soundOptionsDialog()
  {
    boolean isCheckedItem[] = {getSoundOn(), getMusicOn()};

    AlertDialog.Builder builder = new AlertDialog.Builder(FrozenBubble.this);
    //
    // Set the dialog title.
    //
    //
    builder.setTitle(R.string.menu_sound_options)
    //
    // Specify the list array, the items to be selected by default
    // (null for none), and the listener through which to receive
    // callbacks when items are selected.
    //
    //
    .setMultiChoiceItems(R.array.sound_options_array, isCheckedItem,
                         new DialogInterface.OnMultiChoiceClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which, boolean isChecked)
      {
        switch ( which )
        {
          case 0:
            setSoundOn( isChecked );
            break;
          case 1:
            setMusicOn( isChecked );
            break;
        }
      }
    })
    // Set the action buttons
    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
        // User clicked OK.
        if ( resplayer != null )
        {
          if ( getMusicOn() == true )
          {
            resplayer.setVolume( 255 );
          }
          else
          {
            resplayer.setVolume( 0 );
          }
        }
      }
    });
    builder.create();
    builder.show();
  }

  private void targetOptionsDialog()
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(FrozenBubble.this);
    //
    // Set the dialog title.
    //
    //
    builder.setTitle(R.string.menu_target_mode)
    //
    // Specify the list array, the item to be selected by default,
    // and the listener through which to receive callbacks when the
    // item is selected.
    //
    //
    .setSingleChoiceItems(R.array.shoot_mode_array, targetMode,
                          new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface builder, int which)
      {
        switch ( which )
        {
          case 0:
            setTargetMode( AIM_TO_SHOOT );
            break;
          case 1:
            setTargetMode( POINT_TO_SHOOT );
            break;
          case 2:
            setTargetMode( ROTATE_TO_SHOOT );
            break;
        }
      }
    })
    // Set the action buttons
    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface builder, int id) {
        // User clicked OK.
      }
    });

    builder.create();
    builder.show();
  }

  public synchronized static void setMode(int newMode)
  {
    gameMode = newMode;
  }

  public synchronized static int getMode()
  {
    return gameMode;
  }

  public synchronized static boolean getMusicOn()
  {
    return musicOn;
  }

  public synchronized static void setMusicOn(boolean mo)
  {
    musicOn = mo;
  }

  public synchronized static boolean getSoundOn()
  {
    return soundOn;
  }

  public synchronized static void setSoundOn(boolean so)
  {
    soundOn = so;
  }

  public synchronized static boolean getAimThenShoot()
  {
    return ((targetMode == AIM_TO_SHOOT) || (targetMode == ROTATE_TO_SHOOT));
  }

  public synchronized void setTargetMode(int tm)
  {
    targetMode = tm;

    if ((targetMode == ROTATE_TO_SHOOT) &&
        AccelerometerManager.isSupported(getApplicationContext()))
    {
      AccelerometerManager.startListening(getApplicationContext(),this);
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    if ((targetMode != ROTATE_TO_SHOOT) && AccelerometerManager.isListening())
    {
      AccelerometerManager.stopListening();
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
  }

  public synchronized static boolean getDontRushMe()
  {
    return dontRushMe;
  }

  public synchronized static void setDontRushMe(boolean dont)
  {
    dontRushMe = dont;
  }

  /*
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    if (savedInstanceState != null)
    {
      //Log.i("frozen-bubble", "FrozenBubble.onCreate(...)");
    }
    else
    {
      //Log.i("frozen-bubble", "FrozenBubble.onCreate(null)");
    }
    super.onCreate(savedInstanceState);

    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    // Allow editor functionalities.
    Intent i = getIntent();
    if ( ( null == i ) || ( null == i.getExtras() ) ||
         !i.getExtras().containsKey("levels") )
    {
      // Default intent.
      activityCustomStarted = false;
      setContentView(R.layout.activity_frozen_bubble);
      mGameView = (GameView)findViewById(R.id.game);
    }
    else
    {
      // Get custom level last played.
      SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                  Context.MODE_PRIVATE);
      int startingLevel = sp.getInt("levelCustom", 0);
      int startingLevelIntent = i.getIntExtra("startingLevel", -2);
      startingLevel =
        (startingLevelIntent == -2) ? startingLevel : startingLevelIntent;
      activityCustomStarted = true;
      mGameView = new GameView(this,
                               i.getExtras().getByteArray("levels"),
                               startingLevel);
      setContentView(mGameView);
    }

    mGameView.setGameListener(this);
    mGameThread = mGameView.getThread();

    if (savedInstanceState != null) {
      mGameThread.restoreState(savedInstanceState);
    }
    mGameView.requestFocus();

    setFullscreen();
    setTargetMode(targetMode);
    newPlayer( true );
  }

  /**
   * Invoked when the Activity loses user focus.
   */
  @Override
  protected void onPause() {
    //Log.i("frozen-bubble", "FrozenBubble.onPause()");
    super.onPause();
    mGameView.getThread().pause();
    // Allow editor functionalities.
    SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sp.edit();
    // If I didn't run game from editor, save last played level.
    Intent i = getIntent();
    if ( ( null == i ) || !activityCustomStarted )
    {
      editor.putInt("level", mGameThread.getCurrentLevelIndex());
    }
    else
    {
      // Editor's intent is running.
      editor.putInt("levelCustom", mGameThread.getCurrentLevelIndex());
    }
    editor.commit();
    //
    //   Pause the MOD player and preserve song information.
    //
    //
    resplayer.PausePlay();
    savePlayerState( );
  }

  /**
   * Invoked when the Activity is finishing or being destroyed by the
   * system.
   */
  @Override
  protected void onDestroy() {
    //Log.i("frozen-bubble", "FrozenBubble.onDestroy()");
    super.onDestroy();
    cleanUp();
  }

  /**
   * Notification that something is about to happen, to give the Activity a
   * chance to save state.
   *
   * @param outState a Bundle into which this Activity should save its state
   */
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    //Log.i("frozen-bubble", "FrozenBubble.onSaveInstanceState()");
    // Just have the View's thread save its state into our Bundle.
    super.onSaveInstanceState(outState);
    mGameThread.saveState(outState);
    savePlayerState();
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onNewIntent(android.content.Intent)
   */
  @Override
  protected void onNewIntent(Intent intent) {
    if (null != intent && EDITORACTION.equals(intent.getAction())) {
      if (!activityCustomStarted) {
        activityCustomStarted = true;

        // Get custom level last played.
        SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                    Context.MODE_PRIVATE);
        int startingLevel = sp.getInt("levelCustom", 0);
        int startingLevelIntent = intent.getIntExtra("startingLevel", -2);
        startingLevel =
          (startingLevelIntent == -2) ? startingLevel : startingLevelIntent;

        mGameView = null;
        mGameView = new GameView( this,
                                  intent.getExtras().getByteArray("levels"),
                                  startingLevel );
        setContentView(mGameView);
        mGameThread = mGameView.getThread();
        mGameThread.newGame();
        mGameView.requestFocus();
        setFullscreen();
        newPlayer( true );
      }
    }
  }

  public void cleanUp()
  {
    //
    //   Since this activity is being destroyed, set the flag to ensure
    //   that the application displays the splash screen the next time
    //   it is launched.
    //
    //
    SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                                                Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sp.edit();
    editor.putBoolean("showSplashScreen", true);
    editor.commit();

    if (AccelerometerManager.isListening())
      AccelerometerManager.stopListening();

    if (mGameView != null)
      mGameView.cleanUp( );

    mGameView   = null;
    mGameThread = null;

    if (resplayer != null)
    {
      resplayer.StopAndClose();
      resplayer = null;
    }
  }

  public void onAccelerationChanged(float x, float y, float z)
  {
    if ( mGameThread != null )
      mGameThread.setPosition(20f+x*2f);
  }

  public void onGameEvent(int type)
  {
    switch ( type )
    {
      case GameView.EVENT_GAME_WON:
        mod_was = mod_now;
        mod_now++;
        break;

      case GameView.EVENT_GAME_LOST:
        break;

      case GameView.EVENT_GAME_PAUSED:
        resplayer.PausePlay();
        break;

      case GameView.EVENT_GAME_RESUME:
        //
        //   If the MOD resource player exists, then simply unpause
        //   the current MOD.  Otherwise, create it.
        //
        //
        if (resplayer == null)
        {
          //
          //   Get a new player thread with this mod file data.
          //
          //
          resplayer = new MODResourcePlayer(this);
          //
          //   Restore song number and current pattern so we can resume
          //   from there...
          //
          //
          mConfig = getSharedPreferences(PLAYER_PREFS_NAME, 0);
          mod_now = mConfig.getInt(PREFS_SONGNUM, DEFAULT_SONG);
          int pattern = mConfig.getInt(PREFS_SONGPATTERN, 0);
          resplayer.LoadMODResource(MODlist[mod_now]);
          resplayer.setCurrentPattern(pattern);
          //
          // Start up the music.
          //
          //
          resplayer.start();
          mod_was = mod_now;
        }
        else
        {
          if ( mod_now != mod_was )
            playCurrentMOD( );
          else
            resplayer.UnPausePlay();
        }
        break;

      case GameView.EVENT_LEVEL_START:
        playCurrentMOD( );
        break;

      default:
        break;
    }
  }

  private void newPlayer( boolean startPausedFlag )
  {
    //*****************************************
    // Start up the MOD player
    // *****************************************
    //
    //   Get the MOD playlist song index.  If the game is going to be
    //   played starting at the first game level, set the current MOD
    //   index to the first song in the playlist.  Otherwise, load the
    //   song index that was last saved in the MOD player preferences.
    //
    //
    if ( mGameView.getThread().getCurrentLevelIndex() == 0 )
    {
      mod_now = 0;
    }
    else
    {
      mConfig = getSharedPreferences(PLAYER_PREFS_NAME, 0);
      mod_now = mConfig.getInt(PREFS_SONGNUM, DEFAULT_SONG);
    }
    //
    //   If the MOD player instance is not NULL, destroy it and create
    //   a new one.
    //
    //
    if (resplayer != null)
    {
      resplayer.StopAndClose();
      resplayer = null;
    }
    // load the mod file
    resplayer = new MODResourcePlayer(this);
    resplayer.setLoopCount(PlayerThread.LOOP_SONG_FOREVER);
    resplayer.LoadMODResource(MODlist[mod_now]);
    if ( getMusicOn() == true )
    {
      resplayer.setVolume( 255 );
    }
    else
    {
      resplayer.setVolume( 0 );
    }
    // start up the music (well, start the thread, at least...)
    resplayer.startPaused(startPausedFlag);
    resplayer.start();
    mod_was = mod_now;
  }

  //
  //   Play the current song in our playlist from the beginning of the
  //   song.
  //
  //   The current MOD index may have been modified externally.
  //
  //
  private void playCurrentMOD()
  {
    if (resplayer != null)
    {
      resplayer.PausePlay();
      if (mod_now >= MODlist.length) mod_now = 0;
      // load the current MOD into the player
      resplayer.LoadMODResource(MODlist[mod_now]);
      resplayer.UnPausePlay();
      mod_was = mod_now;
    }
    else
      newPlayer( false );
  }

  private void savePlayerState()
  {
    //
    //   Save song number and current pattern so we can resume from
    //   there...
    //
    //
    SharedPreferences.Editor prefs =
      getSharedPreferences(PLAYER_PREFS_NAME, 0).edit();
    prefs.putInt(PREFS_SONGNUM, mod_now);
    prefs.putInt(PREFS_SONGPATTERN, resplayer.getCurrentPattern());
    prefs.commit();
  }

  // Starts editor / market with editor's download.
  private void startEditor()
  {
    Intent i = new Intent();
    // First try to run the plus version of Editor.
    i.setClassName("sk.halmi.fbeditplus", 
                   "sk.halmi.fbeditplus.EditorActivity");
    try {
      startActivity(i);
      finish();
    } catch (ActivityNotFoundException e) {
      // If not found, try to run the normal version.
      i.setClassName("sk.halmi.fbedit", 
                     "sk.halmi.fbedit.EditorActivity");
      try {
        startActivity(i);
        finish();
      } catch (ActivityNotFoundException ex) {
        // If user doesnt have Frozen Bubble Editor take him to market.
        try {
          Toast.makeText(getApplicationContext(), 
                         R.string.install_editor, Toast.LENGTH_SHORT).show();
          i = new Intent(Intent.ACTION_VIEW,
                         Uri.parse(
                         "market://search?q=frozen bubble level editor"));
          startActivity(i);
        } catch (Exception exc) {
          // Damn you don't have market?
          Toast.makeText(getApplicationContext(), 
                         R.string.market_missing, Toast.LENGTH_SHORT).show();
        }
      }
    }
  }
}