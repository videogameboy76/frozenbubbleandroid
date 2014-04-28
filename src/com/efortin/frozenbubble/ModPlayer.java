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

import android.content.Context;

import com.peculiargames.andmodplug.MODResourcePlayer;
import com.peculiargames.andmodplug.PlayerThread;

public class ModPlayer {
  private MODResourcePlayer resplayer = null;

  public ModPlayer(Context context,
                   int songId,
                   boolean musicOn,
                   boolean startPaused) {
    newMusicPlayer(context, songId, musicOn, startPaused);
  }

  /**
   * Stop the music player, close the thread, and free the instance.
   */
  public void destroyMusicPlayer() {
    synchronized(this) {
      if (resplayer != null) {
        resplayer.StopAndClose();
        resplayer = null;
      }
    }
  }

  /**
   * Load a new song.
   * @param songId - The song resource ID.
   * @param startPlaying - If <code>true</code>, the song starts playing
   * immediately.  Otherwise it is paused and must be unpaused to start
   * playing.
   */
  public void loadNewSong(int songId, boolean startPlaying) {
    if (resplayer != null) {
      // Pause the current song.
      resplayer.PausePlay();
      // Load the current MOD into the player.
      resplayer.LoadMODResource(songId);
      if (startPlaying)
        resplayer.UnPausePlay();
    }
  }

  /**
   * Create a new music player.
   * @param context - The application context.
   * @param songId - The song resource ID.
   * @param startPaused - If <code>false</code>, the song starts playing
   * immediately.  Otherwise it is paused and must be unpaused to start
   * playing.
   */
  private void newMusicPlayer(Context context,
                              int songId,
                              boolean musicOn,
                              boolean startPaused) {
    // Create a new music player.
    resplayer = new MODResourcePlayer(context);
    // Load the mod file.
    resplayer.LoadMODResource(songId);
    // Loop the song forever.
    resplayer.setLoopCount(PlayerThread.LOOP_SONG_FOREVER);
    // Turn the music on or off.
    setMusicOn(musicOn);
    // Start the music thread.
    resplayer.startPaused(startPaused);
    resplayer.start();
  }

  public void pausePlay() {
    if (resplayer != null)
      resplayer.PausePlay();
  }

  public void setMusicOn(boolean musicOn) {
    if (musicOn) {
      resplayer.setVolume(255);
    }
    else {
      resplayer.setVolume(0);
    }
  }

  public void unPausePlay() {
    if (resplayer != null)
      resplayer.UnPausePlay();
  }
}
