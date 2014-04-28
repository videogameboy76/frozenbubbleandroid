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

package org.jfedor.frozenbubble;

import java.util.Vector;

import android.graphics.Canvas;
import android.os.Bundle;

public abstract class GameScreen {

  public static enum eventEnum {
    GAME_WON,
    GAME_LOST,
    GAME_PAUSED,
    GAME_RESUME,
    LEVEL_START;
  }

  public static enum gameEnum {
    PLAYING,
    LOST,
    WON,
    NEXT_LOST,
    NEXT_WON;
  }

  public static enum stateEnum {
    RUNNING,
    PAUSED,
    ABOUT;
  }

  private Vector<Sprite> sprites;

  public final void saveSprites(Bundle map, Vector<Sprite> savedSprites,
                                int id) {
    for (int i = 0; i < sprites.size(); i++) {
      ((Sprite)sprites.elementAt(i)).saveState(map, savedSprites, id);
      map.putInt(String.format("%d-game-%d", id, i),
                 ((Sprite)sprites.elementAt(i)).getSavedId());
    }
    map.putInt(String.format("%d-numGameSprites", id), sprites.size());
  }

  public final void restoreSprites(Bundle map, Vector<Sprite> savedSprites,
                                   int id) {
    sprites = new Vector<Sprite>();
    int numSprites = map.getInt(String.format("%d-numGameSprites", id));
    for (int i = 0; i < numSprites; i++) {
      int spriteIdx = map.getInt(String.format("%d-game-%d", id, i));
      sprites.addElement(savedSprites.elementAt(spriteIdx));
    }
  }

  public GameScreen() {
    sprites = new Vector<Sprite>();
  }

  public final void addSprite(Sprite sprite) {
    sprites.removeElement(sprite);
    sprites.addElement(sprite);
  }

  public final void removeAllBubbleSprites() {
    int i = 0;
    while ((sprites.size() > 0) && (i < sprites.size())) {
      if(((Sprite)sprites.elementAt(i)).getTypeId() == Sprite.TYPE_BUBBLE) {
        removeSprite((Sprite)sprites.elementAt(i));
      }
      else
        i++;
    }
  }

  public final void removeSprite(Sprite sprite) {
    sprites.removeElement(sprite);
  }

  public final void spriteToBack(Sprite sprite) {
    sprites.removeElement(sprite);
    sprites.insertElementAt(sprite,0);
  }

  public final void spriteToFront(Sprite sprite) {
    sprites.removeElement(sprite);
    sprites.addElement(sprite);
  }

  public void paint(Canvas c, double scale, int dx, int dy) {
    for (int i = 0; i < sprites.size(); i++) {
      ((Sprite)sprites.elementAt(i)).paint(c, scale, dx, dy);
    }
  }

  public abstract gameEnum play(boolean key_left, boolean key_right,
                                boolean key_fire, boolean key_swap,
                                double trackball_dx,
                                boolean touch_fire,
                                double touch_x, double touch_y,
                                boolean ats_touch_fire, double ats_touch_dx);
}
