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

public class Preferences {
  public static final int PREFS_BYTES = 22;

  boolean adsOn      = true;
  int     collision  = BubbleSprite.MIN_PIX;
  boolean colorMode  = false;
  boolean compressor = false;
  int     difficulty = LevelManager.MODERATE;
  boolean dontRushMe = true;
  boolean fullscreen = true;
  int     gameMode   = FrozenBubble.GAME_NORMAL;
  boolean musicOn    = true;
  boolean soundOn    = true;
  int     targetMode = FrozenBubble.POINT_TO_SHOOT;

  /**
   * <code>Preferences</code> class constructor.  Variables are
   * initialized to defaults.
   */
  public Preferences() {
    adsOn      = true;
    collision  = BubbleSprite.MIN_PIX;
    colorMode  = false;
    compressor = false;
    difficulty = LevelManager.MODERATE;
    dontRushMe = true;
    fullscreen = true;
    gameMode   = FrozenBubble.GAME_NORMAL;
    musicOn    = true;
    soundOn    = true;
    targetMode = FrozenBubble.POINT_TO_SHOOT;
  }

  /**
   * <code>Preferences</code> class constructor.
   * @param prefs - object reference used to initialize this object.
   * Pass <code>null</code> to create a default instance.
   */
  public Preferences(Preferences prefs) {
    copy(prefs);
  }

  /**
   * Copy the values of the supplied object to this object.
   * @param prefs - the object to copy to this object.
   */
  public void copy(Preferences prefs) {
    if (prefs != null) {
      this.adsOn = prefs.adsOn;
      this.collision = prefs.collision;
      this.colorMode = prefs.colorMode;
      this.compressor = prefs.compressor;
      this.difficulty = prefs.difficulty;
      this.dontRushMe = prefs.dontRushMe;
      this.fullscreen = prefs.fullscreen;
      this.gameMode = prefs.gameMode;
      this.musicOn = prefs.musicOn;
      this.soundOn = prefs.soundOn;
      this.targetMode = prefs.targetMode;
    }
  }
};
