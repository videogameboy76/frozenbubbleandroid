/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright (c) 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright (c) 2003 Glenn Sanson.
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
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
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
 *    Copyright (c) Google Inc.
 *
 *          [[ http://glenn.sanson.free.fr/fb/ ]]
 *          [[ http://www.frozen-bubble.org/   ]]
 */

package org.jfedor.frozenbubble;

import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;

public class Compressor {
  private BmpWrap compressorHead;
  private BmpWrap compressor;
  int steps;

  public Compressor(BmpWrap compressorHead, BmpWrap compressor)
  {
    this.compressorHead = compressorHead;
    this.compressor = compressor;
    this.steps = 0;
  }

  public void saveState(Bundle map)
  {
    map.putInt("compressor-steps", steps);
  }

  public void restoreState(Bundle map)
  {
    steps = map.getInt("compressor-steps");
  }

  public void moveDown() {
    steps++;
  }

  public void paint(Canvas c, double scale, int dx, int dy)
  {
    for (int i = 0; i < steps; i++) {
      c.drawBitmap(compressor.bmp,
                   (float)(235 * scale + dx),
                   (float)((28 * i - 4) * scale + dy),
                   null);
      c.drawBitmap(compressor.bmp,
                   (float)(391 * scale + dx),
                   (float)((28 * i - 4) * scale + dy),
                   null);
    }
    c.drawBitmap(compressorHead.bmp,
                 (float)(160 * scale + dx),
                 (float)((-7 + 28 * steps) * scale + dy),
                 null);
  }
};
