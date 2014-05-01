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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class PreferencesActivity extends PreferenceActivity{

  private Preferences mPrefs;
  private SeekBarPreference collisionSlider;
  private SeekBarPreference difficultySlider;

  private void cleanUp() {
    mPrefs = null;
    collisionSlider = null;
    difficultySlider = null;
  }

  public static void getFrozenBubblePrefs(Preferences prefs, SharedPreferences sp) {
    prefs.adsOn      = sp.getBoolean("adsOn",      true                       );
    prefs.collision  = sp.getInt    ("collision",  BubbleSprite.MIN_PIX       );
    prefs.compressor = sp.getBoolean("compressor", false                      );
    prefs.difficulty = sp.getInt    ("difficulty", LevelManager.MODERATE      );
    prefs.dontRushMe = sp.getBoolean("dontRushMe", false                      );
    prefs.fullscreen = sp.getBoolean("fullscreen", true                       );
    prefs.gameMode   = sp.getInt    ("gameMode",   FrozenBubble.GAME_NORMAL   );
    prefs.musicOn    = sp.getBoolean("musicOn",    true                       );
    prefs.soundOn    = sp.getBoolean("soundOn",    true                       );
    prefs.targetMode = sp.getInt    ("targetMode", FrozenBubble.POINT_TO_SHOOT);

    if (prefs.gameMode == FrozenBubble.GAME_NORMAL)
      prefs.colorMode = false;
    else
      prefs.colorMode = true;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);

     mPrefs = new Preferences();
     setDefaultPreferences();
     addPreferencesFromResource(R.layout.activity_preferences_screen);
     collisionSlider = (SeekBarPreference) findPreference("collision_option");
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
      savePreferences();
      finish();
    }
    else {
      result = collisionSlider.onKey(keyCode, msg);
      result |= difficultySlider.onKey(keyCode, msg);
    }

    return result || super.onKeyDown(keyCode, msg);
  }

  private void savePreferences() {
    SharedPreferences dsp = 
        PreferenceManager.getDefaultSharedPreferences(this);
    mPrefs.adsOn      = dsp.getBoolean("ads_option", true);
    mPrefs.collision  = dsp.getInt("collision_option", BubbleSprite.MIN_PIX);
    mPrefs.compressor = dsp.getBoolean("compressor_option", false);
    mPrefs.difficulty = dsp.getInt("difficulty_option", LevelManager.MODERATE);
    mPrefs.dontRushMe = !dsp.getBoolean("rush_me_option", true);
    mPrefs.fullscreen = dsp.getBoolean("fullscreen_option", true);
    mPrefs.colorMode  = dsp.getBoolean("colorblind_option", false);
    mPrefs.musicOn    = dsp.getBoolean("play_music_option", true);
    mPrefs.soundOn    = dsp.getBoolean("sound_effects_option", true);
    mPrefs.targetMode = Integer.valueOf(dsp.getString("targeting_option",
        Integer.toString(FrozenBubble.POINT_TO_SHOOT)));

    if (!mPrefs.colorMode)
      mPrefs.gameMode = FrozenBubble.GAME_NORMAL;
    else
      mPrefs.gameMode = FrozenBubble.GAME_COLORBLIND;

    setFrozenBubblePrefs(mPrefs);

    SharedPreferences sp = getSharedPreferences(FrozenBubble.PREFS_NAME,
        Context.MODE_PRIVATE);

    setFrozenBubblePrefs(mPrefs, sp);
  }

  private void setDefaultPreferences() {
    SharedPreferences sp = getSharedPreferences(FrozenBubble.PREFS_NAME,
                                                Context.MODE_PRIVATE);
    getFrozenBubblePrefs(mPrefs, sp);

    SharedPreferences spEditor =
        PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = spEditor.edit();
    editor.putBoolean("ads_option", mPrefs.adsOn);
    editor.putInt("collision_option", mPrefs.collision);
    editor.putBoolean("compressor_option", mPrefs.compressor);
    editor.putInt("difficulty_option", mPrefs.difficulty);
    editor.putBoolean("rush_me_option", !mPrefs.dontRushMe);
    editor.putBoolean("fullscreen_option", mPrefs.fullscreen);
    editor.putBoolean("colorblind_option", mPrefs.colorMode);
    editor.putBoolean("play_music_option", mPrefs.musicOn);
    editor.putBoolean("sound_effects_option", mPrefs.soundOn);
    editor.putString("targeting_option", Integer.toString(mPrefs.targetMode));
    editor.commit();
  }

  /**
   * Update the game preferences to the desired values.
   * @param prefs - the desired game preferences.
   */
  public static void setFrozenBubblePrefs(Preferences prefs) {
    FrozenBubble.setAdsOn(prefs.adsOn);
    FrozenBubble.setCollision(prefs.collision);
    FrozenBubble.setCompressor(prefs.compressor);
    FrozenBubble.setDifficulty(prefs.difficulty);
    FrozenBubble.setDontRushMe(prefs.dontRushMe);
    FrozenBubble.setFullscreen(prefs.fullscreen);
    FrozenBubble.setMode(prefs.gameMode);
    FrozenBubble.setMusicOn(prefs.musicOn);
    FrozenBubble.setSoundOn(prefs.soundOn);
    FrozenBubble.setTargetMode(prefs.targetMode);
  }

  /**
   * Save the desired game preference values to nonvolatile memory.
   * @param prefs - the desired game preferences.
   * @param sp - the <code>SharedPreferences</code> object reference to
   * create a preference editor for the purpose of saving the
   * preferences to nonvolatile memory.
   */
  public static void setFrozenBubblePrefs(Preferences prefs, SharedPreferences sp) {
    SharedPreferences.Editor editor = sp.edit();
    editor.putBoolean("adsOn", prefs.adsOn);
    editor.putInt("collision", prefs.collision);
    editor.putBoolean("compressor", prefs.compressor);
    editor.putInt("difficulty", prefs.difficulty);
    editor.putBoolean("dontRushMe", prefs.dontRushMe);
    editor.putBoolean("fullscreen", prefs.fullscreen);
    editor.putInt("gameMode", prefs.gameMode);
    editor.putBoolean("musicOn", prefs.musicOn);
    editor.putBoolean("soundOn", prefs.soundOn);
    editor.putInt("targetMode", prefs.targetMode);
    editor.commit();
  }
}
