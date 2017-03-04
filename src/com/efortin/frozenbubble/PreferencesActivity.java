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

import org.jfedor.frozenbubble.BubbleSprite;
import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.LevelManager;
import org.jfedor.frozenbubble.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class PreferencesActivity extends PreferenceActivity{

  private SeekBarPreference collisionSlider;
  private SeekBarPreference difficultySlider;

  private void cleanUp() {
    collisionSlider  = null;
    difficultySlider = null;
  }
  
  public static Preferences getDefaultPrefs(SharedPreferences sp) {
    Preferences prefs = new Preferences();
    prefs.bluetooth  =  sp.getInt("bluetooth", 0);
    prefs.collision  =  sp.getInt("collision_option", BubbleSprite.MIN_PIX);
    prefs.compressor =  sp.getBoolean("compressor_option", false);
    prefs.difficulty =  sp.getInt("difficulty_option", LevelManager.NORMAL);
    prefs.dontRushMe = !sp.getBoolean("rush_me_option", true);
    prefs.fullscreen =  sp.getBoolean("fullscreen_option", true);
    prefs.colorMode  =  sp.getBoolean("colorblind_option", FrozenBubble.GAME_COLORBLIND);
    prefs.musicOn    =  sp.getBoolean("play_music_option", true);
    prefs.soundOn    =  sp.getBoolean("sound_effects_option", true);
    prefs.targetMode =  Integer.valueOf(sp.getString("targeting_option",
        Integer.toString(FrozenBubble.POINT_TO_SHOOT)));

    return prefs;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);
     addPreferencesFromResource(R.layout.activity_preferences_screen);
     collisionSlider  = (SeekBarPreference) findPreference("collision_option" );
     difficultySlider = (SeekBarPreference) findPreference("difficulty_option");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    cleanUp();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent msg) {
    boolean result = false;

    if (keyCode == KeyEvent.KEYCODE_BACK) {
      FrozenBubble.setPrefs(getDefaultPrefs(PreferenceManager.
          getDefaultSharedPreferences(this)));
      finish();
    }
    else {
      result  = collisionSlider .onKey(keyCode, msg);
      result |= difficultySlider.onKey(keyCode, msg);
    }

    return result || super.onKeyDown(keyCode, msg);
  }

  public static void saveDefaultPreferences(Preferences prefs,
                                            SharedPreferences sp) {
    SharedPreferences.Editor editor = sp.edit();
    editor.putInt    ("bluetooth",            prefs.bluetooth );
    editor.putInt    ("collision_option",     prefs.collision );
    editor.putBoolean("compressor_option",    prefs.compressor);
    editor.putInt    ("difficulty_option",    prefs.difficulty);
    editor.putBoolean("rush_me_option",      !prefs.dontRushMe);
    editor.putBoolean("fullscreen_option",    prefs.fullscreen);
    editor.putBoolean("colorblind_option",    prefs.colorMode );
    editor.putBoolean("play_music_option",    prefs.musicOn   );
    editor.putBoolean("sound_effects_option", prefs.soundOn   );
    editor.putString ("targeting_option",     Integer.toString(prefs.targetMode));
    editor.commit();
  }
}
