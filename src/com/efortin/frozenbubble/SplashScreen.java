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

package com.efortin.frozenbubble;

import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;

public class SplashScreen extends Activity
{

  //
  //   Maximum time until we go to the next activity.
  //
  //
  protected int _splashTime = 3000;
  private Thread splashThread;

  /*
   * Called when the activity is first created.
   */
  @Override
  public void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );
    setContentView( R.layout.activity_splash_screen );

    if ( ! displaySplashScreen() )
    {
      startFrozenBubble();
      return;
    }

    //
    //   Thread for displaying the SplashScreen.
    //
    //
    splashThread = new Thread()
    {
      @Override
      public void run()
      {
        try
        {
          synchronized( this )
          {
            //
            //   TODO: The splash screen waits before launching the
            //         game activity.  Change this so that the game
            //         activity is started immediately, and notifies
            //         the splash screen activity when it is done
            //         loading saved state data and preferences, so the
            //         splash screen functions as a distraction from
            //         game loading latency.  There is no advantage in
            //         doing this right now, because there is no lag.
            //
            //
            wait( _splashTime );  //wait 3 seconds
          }
        } catch( InterruptedException e ) {}
        finally
        {
          startFrozenBubble();
        }
      }
    };

    splashThread.start();
  }

  /**
   * Invoked when the screen is touched.
   */
  @Override
  public boolean onTouchEvent( MotionEvent event )
  {
    if ( event.getAction() == MotionEvent.ACTION_DOWN )
    {
      synchronized( splashThread )
      {
        splashThread.notifyAll();
      }
    }
    return true;
  }

  private boolean displaySplashScreen()
  {
    SharedPreferences sp = getSharedPreferences( FrozenBubble.PREFS_NAME,
                                                 Context.MODE_PRIVATE );
    boolean showSplashScreen = sp.getBoolean( "showSplashScreen", true );

    if ( showSplashScreen )
    {
      SharedPreferences.Editor editor = sp.edit();
      editor.putBoolean( "showSplashScreen", false );
      editor.commit();
    }

    return showSplashScreen;
  }

  private void startFrozenBubble()
  {
    //
    //   Create an intent to launch the activity to play the game.
    //
    //
    Intent intent = new Intent( this, FrozenBubble.class );
    startActivity( intent );
    //
    //   Terminate the splash screen activity.
    //
    //
    finish();
  }
}
