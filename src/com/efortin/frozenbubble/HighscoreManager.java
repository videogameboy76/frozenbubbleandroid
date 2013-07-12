/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright (c) 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright (c) 2003 Glenn Sanson.
 * High score manager source - Copyright (c) 2010 Michel Racic.
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
 *    Eric Fortin  <videogameboy76 at yahoo.com>
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

import java.util.List;

import android.content.Context;
import android.os.Bundle;

/**
 * @author Michel Racic (http://www.2030.tk)
 * <br>  A class to manage the highscore table for each level.
 */
public class HighscoreManager {

  public static final String PUZZLE_DATABASE_NAME      = "frozenbubble";
  public static final String MULTIPLAYER_DATABASE_NAME = "multiplayer";

  private boolean isPaused = true;
  private int currentLevel = 0;
  private long startTime   = 0;
  private long pausedTime  = 0;
  private long lastScoreId = -1;
  private final HighscoreDB db;
  private final Context     ctx;
  String name = null;

  public HighscoreManager(Context context, String databaseName) {
    ctx = context;
    db = new HighscoreDB(ctx, databaseName);
  }

  /**
   * @param nbBubbles
   *        - The number of bubbles launched by the player.
   */
  public void endLevel(int nbBubbles) {
    long endTime  = System.currentTimeMillis();
    long duration = (endTime - startTime) + pausedTime;

    if ( duration < 0 )
      duration = 0;
    /*
    if (name == null) {
      SharedPreferences sp = ctx.getSharedPreferences(FrozenBubble.PREFS_NAME,
                                                      Context.MODE_PRIVATE);
      name = sp.getString("highscorename", "anon");
    }
    //
    // Prompt the player to enter their name to enter into the high
    // scores table.
    //
    //
    AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
    alert.setTitle("Highscore name"); alert.setMessage("Set your name:");
    final EditText input = new EditText(ctx); input.setText(name);
    alert.setView(input);
    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        SharedPreferences sp =
          ctx.getSharedPreferences(FrozenBubble.PREFS_NAME,
                                   Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("highscorename", input.getText() .toString());
        editor.commit();
      }
    });
    alert.show();
    SharedPreferences sp = ctx.getSharedPreferences(FrozenBubble.PREFS_NAME,
                                                    Context.MODE_PRIVATE);
    name = sp.getString("highscorename", "anon");
    */
    lastScoreId = db.insert(new HighscoreDO(currentLevel, "anon",
                            nbBubbles, duration));
    //Log.i("FrozenBubble-highscore", "endLevel() " + (duration / 1000F) +
    //  " seconds and " + nbBubbles + " shots used for level " + currentLevel);
  }

  public void lostLevel() {
    lastScoreId = -1;
  }

  public void startLevel(int level) {
    startTime    = System.currentTimeMillis();
    currentLevel = level;
    pausedTime   = 0;
    isPaused     = false;
    //Log.i("FrozenBubble-highscore", "startLevel(" + level + ")");
  }

  /**
   * Accumulate the play time between pause/resume cycles.
   * <p>
   * <code>pausedTime</code> is an accumulation of the play time
   * between pause/resume cycles.
   */
  public void pauseLevel() {
    long currentTime = System.currentTimeMillis();
    if (!isPaused) {
      isPaused = true;
      pausedTime += currentTime - startTime;
    }
    startTime = currentTime;
    //Log.i("FrozenBubble-highscore", "pauseLevel() " + (pausedTime / 1000F) +
    //  " seconds used");
  }

  public void resumeLevel() {
    startTime = System.currentTimeMillis();
    isPaused = false;
    //Log.i("FrozenBubble-highscore", "resumeLevel() " + (pausedTime / 1000F) +
    //  " seconds used");
  }

  public void saveState(Bundle map) {
    pauseLevel();
    map.putInt("HighscoreManager-currentLevel", currentLevel);
    map.putLong("HighscoreManager-pausedTime", pausedTime);
    //Log.i("FrozenBubble-highscore", "saveState() " + (pausedTime / 1000F) +
    //  " seconds used in level " + currentLevel);
  }

  public void restoreState(Bundle map) {
    currentLevel = map.getInt("LevelManager-currentLevel");
    pausedTime = map.getLong("HighscoreManager-pausedTime");
    resumeLevel();
    //Log.i("FrozenBubble-highscore", "restoreState() " + (pausedTime / 1000F) +
    //  " seconds used in level " + currentLevel);
  }

  public List<HighscoreDO> getHighscore(int level, int limit) {
    return db.selectByLevel(level, limit);
  }

  public int getLevel() {
    return currentLevel;
  }

  public long getLastScoreId() {
    return lastScoreId;
  }
}
