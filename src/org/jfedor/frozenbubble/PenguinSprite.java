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

import java.util.Random;
import java.util.Vector;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;

public class PenguinSprite extends Sprite {
  public final static int PENGUIN_HEIGHT = 45;
  public final static int PENGUIN_WIDTH  = 57;

  public final static int STATE_TURN_LEFT  = 0;
  public final static int STATE_TURN_RIGHT = 1;
  public final static int STATE_FIRE       = 2;
  public final static int STATE_VOID       = 3;
  public final static int STATE_GAME_WON   = 4;
  public final static int STATE_GAME_LOST  = 5;

  public final static int[][] LOST_SEQUENCE =
    {{1,0}, {2,8}, {3,9}, {4,10}, {5,11}, {6,12}, {7,13}, {5,14}};
  public final static int[][] WON_SEQUENCE =
    {{1,0}, {2,7}, {3,6}, {4,15}, {5,16}, {6,17}, {7,18}, {4,19}};

  private int count;
  private int currentPenguin;
  private int finalState;
  private int nextPosition;

  private BmpWrap spritesImage;
  private Random rand;

  public PenguinSprite(Rect r, BmpWrap sprites, Random rand) {
    super(r);

    this.spritesImage = sprites;
    this.rand = rand;

    currentPenguin = 0;
    finalState     = STATE_VOID;
    nextPosition   = 0;
  }

  public PenguinSprite(Rect r, BmpWrap sprites, Random rand,
                       int currentPenguin, int count,
                       int finalState, int nextPosition) {
    super(r);

    this.spritesImage   = sprites;
    this.rand           = rand;
    this.currentPenguin = currentPenguin;
    this.count          = count;
    this.finalState     = finalState;
    this.nextPosition   = nextPosition;
  }

  @Override
  public void saveState(Bundle map, Vector<Sprite> saved_sprites, int id) {
    if (getSavedId() != -1) {
      return;
    }
    super.saveState(map, saved_sprites, id);
    map.putInt(String.format("%d-%d-currentPenguin", id, getSavedId()),
               currentPenguin);
    map.putInt(String.format("%d-%d-count", id, getSavedId()), count);
    map.putInt(String.format("%d-%d-finalState", id, getSavedId()),
               finalState);
    map.putInt(String.format("%d-%d-nextPosition", id, getSavedId()),
               nextPosition);
  }

  public static Rect getPenguinRect(int player) {
    if (player == 1)
      return new Rect(361, 436, 361 + PenguinSprite.PENGUIN_WIDTH - 2,
                      436 + PenguinSprite.PENGUIN_HEIGHT - 2);
    else
      return new Rect(221, 436, 221 + PenguinSprite.PENGUIN_WIDTH - 2,
                      436 + PenguinSprite.PENGUIN_HEIGHT - 2);
  }

  public int getTypeId() {
    return Sprite.TYPE_PENGUIN;
  }

  public void updateState(int state) {
    if (finalState != STATE_VOID) {
      count++;

      if (count % 6 == 0) {
        if (finalState == STATE_GAME_LOST) {
          currentPenguin = LOST_SEQUENCE[nextPosition][1];
          nextPosition   = LOST_SEQUENCE[nextPosition][0];
        }
        else if (finalState == STATE_GAME_WON) {
          currentPenguin = WON_SEQUENCE[nextPosition][1];
          nextPosition   = WON_SEQUENCE[nextPosition][0];
        }
      }
    }
    else {
      count++;

      switch(state) {
        case STATE_TURN_LEFT:
          count = 0;
          currentPenguin = 3;
          break;
        case STATE_TURN_RIGHT:
          count = 0;
          currentPenguin = 2;
          break;
        case STATE_FIRE:
          count = 0;
          currentPenguin = 1;
          break;
        case STATE_VOID:
          if (currentPenguin < 4 || currentPenguin > 7) {
            currentPenguin = 0;
          }
          break;
        case STATE_GAME_WON:
        case STATE_GAME_LOST:
          count = 0;
          finalState = state;
          currentPenguin = 0;
          return;
      }

      if (count>100) {
        currentPenguin = 7;
      }
      else if (count % 15 == 0 && count > 25) {
        currentPenguin = (rand.nextInt() % 3)+4;
        if (currentPenguin < 4) {
          currentPenguin = 0;
        }
      }
    }
  }

  public void paint(Canvas c, double scale, int dx, int dy) {
    Rect r = this.getSpriteArea();
    /*
     * Clip the specified penguin graphic from the image array.
     */
    drawImageClipped(spritesImage,
                     (r.left - 1) - ((currentPenguin % 4) * PENGUIN_WIDTH),
                     (r.top - 1) - ((currentPenguin / 4) * PENGUIN_HEIGHT),
                     r, c, scale, dx, dy);
  }
}
