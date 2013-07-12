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

package com.efortin.frozenbubble;

import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class SplashScreen extends Activity {
  /*
   * Provide unique IDs for the views associated with the relative
   * layout.  These are used to define relative view layout positions
   * with respect to other views in the layout.
   * 
   * These IDs are generated automatically if using an XML layout, but
   * this object implements a RelativeLayout that is constructed purely
   * programmatically.
   */
  private final static int SCREEN_ID = 100;
  private final static int BTN1_ID   = 101;
  private final static int BTN2_ID   = 102;
  private final static int BTN3_ID   = 103;

  private Boolean homeShown = false;
  private Boolean musicOn = true;
  private ImageView myImageView = null;
  private RelativeLayout myLayout = null;
  private ModPlayer myModPlayer = null;
  private Thread splashThread = null;

  /**
   * Given that we are using a relative layout for the home screen in
   * order to display the background image and various buttons, this
   * function adds the buttons to the layout to provide game options to
   * the player.
   * <p>
   * The buttons are defined in relation to one another so that when
   * using keys to navigate the buttons, the appropriate button will be
   * highlighted.
   */
  private void addHomeButtons() {
    // Construct the 2 player game button.
    Button start2pGameButton = new Button(this);
    start2pGameButton.setOnClickListener(new Button.OnClickListener(){

      public void onClick(View v){
        // Process the button tap and start a 2 player game.
        startFrozenBubble(2);
      }
    });
    start2pGameButton.setText("Player vs. CPU");
    start2pGameButton.setTextSize(TypedValue.COMPLEX_UNIT_PT, 8);
    start2pGameButton.setWidth((int) (start2pGameButton.getTextSize() * 10));
    start2pGameButton.setHorizontalFadingEdgeEnabled(true);
    start2pGameButton.setFadingEdgeLength(5);
    start2pGameButton.setShadowLayer(5, 5, 5, R.color.black);
    start2pGameButton.setId(BTN2_ID);
    LayoutParams myParams1 = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                              LayoutParams.WRAP_CONTENT);
    myParams1.addRule(RelativeLayout.CENTER_HORIZONTAL);
    myParams1.addRule(RelativeLayout.CENTER_VERTICAL);
    myParams1.topMargin = 15;
    myParams1.bottomMargin = 15;
    // Add view to layout.
    myLayout.addView(start2pGameButton, myParams1);
    // Construct the 1 player game button.
    Button start1pGameButton = new Button(this);
    start1pGameButton.setOnClickListener(new Button.OnClickListener(){

      public void onClick(View v){
        // Process the button tap and start/resume a 1 player game.
        startFrozenBubble(1);
      }
    });
    start1pGameButton.setText("Puzzle");
    start1pGameButton.setTextSize(TypedValue.COMPLEX_UNIT_PT, 8);
    start1pGameButton.setWidth((int) (start1pGameButton.getTextSize() * 10));
    start1pGameButton.setHorizontalFadingEdgeEnabled(true);
    start1pGameButton.setFadingEdgeLength(5);
    start1pGameButton.setShadowLayer(5, 5, 5, R.color.black);
    start1pGameButton.setId(BTN1_ID);
    LayoutParams myParams2 = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                              LayoutParams.WRAP_CONTENT);
    myParams2.addRule(RelativeLayout.CENTER_HORIZONTAL);
    myParams2.addRule(RelativeLayout.ABOVE, start2pGameButton.getId());
    myParams2.topMargin = 15;
    myParams2.bottomMargin = 15;
    // Add view to layout.
    myLayout.addView(start1pGameButton, myParams2);
    // Construct the options button.
    Button optionsButton = new Button(this);
    optionsButton.setOnClickListener(new Button.OnClickListener(){

      public void onClick(View v){
        // Process the button tap and start the preferences activity.
        startPreferencesScreen();
      }
    });
    optionsButton.setText("Options");
    optionsButton.setTextSize(TypedValue.COMPLEX_UNIT_PT, 8);
    optionsButton.setWidth((int) (optionsButton.getTextSize() * 10));
    optionsButton.setHorizontalFadingEdgeEnabled(true);
    optionsButton.setFadingEdgeLength(5);
    optionsButton.setShadowLayer(5, 5, 5, R.color.black);
    optionsButton.setId(BTN3_ID);
    LayoutParams myParams3 = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                              LayoutParams.WRAP_CONTENT);
    myParams3.addRule(RelativeLayout.CENTER_HORIZONTAL);
    myParams3.addRule(RelativeLayout.BELOW, start2pGameButton.getId());
    myParams3.topMargin = 15;
    myParams3.bottomMargin = 15;
    // Add view to layout.
    myLayout.addView(optionsButton, myParams3);
  }

  private void cleanUp() {
    if (myModPlayer != null) {
      myModPlayer.destroyMusicPlayer();
      myModPlayer = null;
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent msg) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      cleanUp();
      //
      // Terminate the splash screen activity.
      //
      //
      finish();
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * @see android.app.Activity#onCreate(android.os.Bundle)
   * 
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    restoreGamePrefs();
    // Configure the window presentation and layout.
    setWindowLayout();
    myLayout = new RelativeLayout(this);
    myLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                              LayoutParams.FILL_PARENT));
    myImageView = new ImageView(this);

    if (FrozenBubble.isRunning)
      startFrozenBubble(0);
    else {
      setBackgroundImage(R.drawable.splash);
      setContentView(myLayout);
      //
      // Thread for managing the splash screen.
      //
      //
      splashThread = new Thread() {
        @Override
        public void run() {
          try {
            synchronized(this) {
              //
              // TODO: The splash screen waits before launching the
              //       game activity.  Change this so that the game
              //       activity is started immediately, and notifies
              //       the splash screen activity when it is done
              //       loading saved state data and preferences, so the
              //       splash screen functions as a distraction from
              //       game loading latency.  There is no advantage in
              //       doing this right now, because there is no lag.
              //
              //
              wait(3000);  // wait 3 seconds
            }
          } catch (InterruptedException e) {
          } finally {
            runOnUiThread(new Runnable() {
              public void run() {
                startHomeScreen();
              }
            });
          }
        }
      };
      splashThread.start();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (myModPlayer != null) {
      myModPlayer.pausePlay();
    }
  }

  @Override
  public void onResume() {
    super.onPause();
    if (myModPlayer != null) {
      restoreGamePrefs();
      if (musicOn)
        myModPlayer.unPausePlay();
    }
  }

  /*
   * (non-Javadoc)
   * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
   * 
   * Invoked when the screen is touched.
   */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (splashThread != null) {
        synchronized(splashThread) {
          splashThread.notifyAll();
        }
      }
    }
    return true;
  }

  private void restoreGamePrefs() {
    SharedPreferences mConfig = getSharedPreferences(FrozenBubble.PREFS_NAME,
                                                     Context.MODE_PRIVATE);
    musicOn = mConfig.getBoolean("musicOn", true );
  }

  private void setBackgroundImage(int resId) {
    if (myImageView.getParent() != null)
      myLayout.removeView(myImageView);

    myImageView.setBackgroundColor(getResources().getColor(R.color.black));
    myImageView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                                 LayoutParams.FILL_PARENT));
    myImageView.setImageResource(resId);
    myImageView.setId(SCREEN_ID);
    myLayout.addView(myImageView);
  }

  /**
   * Set the window layout according to the game preferences.
   *
   * <p>Requesting that the title bar be removed <b>must</b> be
   * performed before setting the view content by applying the XML
   * layout, or it will generate an exception.
   */
  private void setWindowLayout() {
    final int flagFs   = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    final int flagNoFs = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
    // Remove the title bar.
    requestWindowFeature(Window.FEATURE_NO_TITLE);
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

  private void startFrozenBubble(int numPlayers) {
    //
    // Since the default game activity creates its own player,
    // destroy the current player.
    //
    //
    cleanUp();
    //
    // Create an intent to launch the activity to play the game.
    //
    //
    Intent intent = new Intent(this, FrozenBubble.class);
    if (numPlayers > 1)
      intent.putExtra("numPlayers", (int)numPlayers);
    startActivity(intent);
    //
    // Terminate the splash screen activity.
    //
    //
    finish();
  }

  private void startHomeScreen() {
    if (!homeShown) {
      homeShown = true;
      setBackgroundImage(R.drawable.home_screen);
      addHomeButtons();
      setContentView(myLayout);
      myModPlayer = new ModPlayer(this, R.raw.introzik, musicOn, false);
    }
  }

  private void startPreferencesScreen() {
    Intent intent = new Intent(this, PreferencesActivity.class);
    startActivity(intent);
  }
}
