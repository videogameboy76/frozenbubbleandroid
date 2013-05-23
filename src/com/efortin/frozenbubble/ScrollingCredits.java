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
 *    Copyright © Google Inc.
 *
 *          [[ http://glenn.sanson.free.fr/fb/ ]]
 *          [[ http://www.frozen-bubble.org/   ]]
 */

package com.efortin.frozenbubble;

import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.peculiargames.andmodplug.MODResourcePlayer;
import com.peculiargames.andmodplug.PlayerThread;

public class ScrollingCredits extends Activity implements Runnable {
  private boolean victoryScreenShown = false;
  private ScrollingTextView credits;
  private MODResourcePlayer resplayer = null;
  private static final int DEFAULT_SONG = 0;
  private final int[] MODlist = { R.raw.worldofpeace };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Configure the window presentation and layout.
    setWindowLayout(R.layout.activity_scrolling_credits);
    // Get the instance of the ScrollingTextView object.
    credits = (ScrollingTextView)findViewById(R.id.scrolling_credits);
    // Configure the credits text presentation.
    credits.setScrollRepeatLimit(0);
    credits.setSpeed(50.0f);
    credits.setScrollDirection(ScrollingTextView.SCROLL_UP);
    credits.setTextSize(18.0f);
    // Start the credits music.
    playMusic();
    // Post this runnable instance to the scrolling text view.
    credits.postDelayed(this, 100);
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseCredits();
  }

  @Override
  public void onResume() {
    super.onResume();
    resumeCredits();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    cleanUp();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent msg) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      end();
      return true;
    }
    return checkCreditsDone();
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    return checkCreditsDone();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return checkCreditsDone();
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);

    if (hasFocus)
      resumeCredits();
    else
      pauseCredits();
  }

  /**
   * Stop the music player, close the thread, and free the instance.
   */
  private void destroyMusicPlayer() {
    if (resplayer != null) {
      resplayer.StopAndClose();
      resplayer = null;
    }
  }

  /**
   * Create a new music player if necessary, load the song, and start
   * playing it.
   */
  private void playMusic() {
    boolean threadStarted = resplayer != null;
    // If the MOD player instance is NULL, create a new music player.
    if (resplayer == null)
      resplayer = new MODResourcePlayer(this);
    else
      resplayer.PausePlay();
    // Load the module music file.
    resplayer.LoadMODResource(MODlist[DEFAULT_SONG]);
    // Loop the song forever.
    resplayer.setLoopCount(PlayerThread.LOOP_SONG_FOREVER);
    // Set the volume per the game preferences.
    if (FrozenBubble.getMusicOn() == true) {
      resplayer.setVolume(255);
    }
    else {
      resplayer.setVolume(0);
    }
    // Start up the music.
    if (!threadStarted)
    {
      resplayer.startPaused(false);
      resplayer.start();
    }
    else
      resplayer.UnPausePlay();
  }

  private void displayImage(int id) {
    // Construct a new LinearLayout programmatically. 
    LinearLayout linearLayout = new LinearLayout(this);
    linearLayout.setOrientation(LinearLayout.VERTICAL);
    linearLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                                  LayoutParams.FILL_PARENT));
    // ImageView setup for the image.
    ImageView imageView = new ImageView(this);
    // Set image resource.
    imageView.setImageResource(R.drawable.victory);
    // Set image position and scaling.
    imageView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                               LayoutParams.FILL_PARENT));
    // Add view to layout.
    linearLayout.addView(imageView);
    // Set the content view to this layout and display the image.
    setContentView(linearLayout);
  }

  /**
   * Set the window layout according to the settings in the specified
   * layout XML file.  Then apply the full screen option according to
   * the player preference setting.
   * 
   * <p>Note that the title bar is not desired for the scrolling
   * credits, and requesting that the title bar be removed <b>must</b>
   * be applied before setting the view content by applying the XML
   * layout or it will generate an exception.
   * 
   * @param  layoutResID
   *         - The resource ID of the XML layout to use for the window
   *         layout settings.
   */
  private void setWindowLayout(int layoutResID) {
    final int flagFs   = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    final int flagNoFs = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;

    // Remove the title bar.
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    // Load and apply the specified XML layout.
    setContentView(layoutResID);
    // Set full screen mode based on the game preferences.
    SharedPreferences mConfig =
      getSharedPreferences(FrozenBubble.PREFS_NAME, Context.MODE_PRIVATE);
    boolean fullscreen = mConfig.getBoolean("fullscreen", true);

    if (fullscreen) {
      getWindow().addFlags(flagFs);
      getWindow().clearFlags(flagNoFs);
    }
    else {
      getWindow().clearFlags(flagFs);
      getWindow().addFlags(flagNoFs);
    }
  }

  public boolean checkCreditsDone() {
    if (!credits.isScrolling()) {
      end();
      return true;
    }
    return false;
  }

  public void cleanUp() {
    destroyMusicPlayer();
  }

  public void end() {
    credits.abort();
    //
    // Since the default game activity creates its own player,
    // destroy the current player.
    //
    //
    destroyMusicPlayer();
    //
    // Create an intent to launch the game activity.  Since it was
    // running in the background while this activity was running, it
    // may have been stopped by the system.
    //
    //
    Intent intent = new Intent(this, FrozenBubble.class);
    startActivity(intent);
    finish();
  }

  public void pauseCredits() {
    if (resplayer != null)
      resplayer.PausePlay();
    credits.setPaused(true);
  }

  public void resumeCredits() {
    if (resplayer != null)
      resplayer.UnPausePlay();
    credits.setPaused(false);
  }

  @Override
  public void run() {
    // Check if we need to display the end of game victory image.
    if (!credits.isScrolling() && !victoryScreenShown) {
      victoryScreenShown = true;
      // Make the credits text transparent.
      credits.setTextColor(Color.TRANSPARENT);
      // Display the end of game victory image.
      displayImage(R.drawable.victory);
    }
    credits.postDelayed(this, 100);
  }
}
