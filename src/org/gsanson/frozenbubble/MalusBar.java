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

package org.gsanson.frozenbubble;

import org.jfedor.frozenbubble.BmpWrap;
import org.jfedor.frozenbubble.Sprite;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;

public class MalusBar extends Sprite {

  /** X-pos for tomatoes */
  int minX;
  /** Max Y-pos for bar */
  int maxY;
  /** Number of waiting bubbles */
  int nbMalus;
  /** Time to release bubbles */
  public int releaseTime;

  /** Banana Image */
  private BmpWrap banana;
  /** Tomato Image */
  private BmpWrap tomato;

  /**
   * Manages a malus bar (bananas & tomatoes).
   * 
   * @param coordX
   *        - X-coord of game facade.
   * @param coordY
   *        - Y-coord of game facade.
   * 
   * @param leftSide
   *        - if on left side (false => right side).
   * 
   * @param tomato
   *        - image resource for a tomato.
   * 
   * @param banana
   *        - image resource for a banana.
   */
  public MalusBar(int coordX, int coordY, BmpWrap banana, BmpWrap tomato) {
    super(new Rect(coordX, coordY, coordX + 33, coordY + 354));
    minX = coordX;
    maxY = coordY + 354;
    releaseTime = 0;

    this.banana = banana;
    this.tomato = tomato;
  }

  @Override
  public final void paint(Canvas c, double scale, int dx, int dy) {
    int count = nbMalus;
    int pos = maxY;
    while (count >= 7) {
      pos -= 13;
      drawImage(tomato, minX, pos, c, scale, dx, dy);
      count -= 7;
    }
    while (count > 0) {
      pos -= 11;
      drawImage(banana, minX + 3, pos, c, scale, dx, dy);
      count--;
    }
  }

  public void addBubbles(int toAdd) {
    if (toAdd > 0)
      releaseTime = 0;
    nbMalus += toAdd;
  }

  public int getBubbles() {
    return nbMalus;
  }

  public int getTypeId() {
    return Sprite.TYPE_IMAGE;
  }

  public int removeLine() {
    int nb = Math.min(7, nbMalus);
    nbMalus -= nb;
    return nb;
  }

  public void restoreState(Bundle map, int id) {
    nbMalus     = map.getInt(String.format("%d-nbMalus", id));
    releaseTime = map.getInt(String.format("%d-releaseTime", id));
  }

  public void saveState(Bundle map, int id) {
    map.putInt(String.format("%d-nbMalus", id), nbMalus);
    map.putInt(String.format("%d-releaseTime", id), releaseTime);
  }
}
