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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import java.util.Vector;

public class LaunchBubbleSprite extends Sprite
{
  private int currentColor;
  private int currentDirection;
  private Drawable launcher;
  private BmpWrap[] bubbles;
  private BmpWrap[] colorblindBubbles;

  public LaunchBubbleSprite(int initialColor, int initialDirection,
                            Drawable launcher,
                            BmpWrap[] bubbles, BmpWrap[] colorblindBubbles)
  {
    super(new Rect(276, 362, 276 + 86, 362 + 76));

    currentColor = initialColor;
    currentDirection = initialDirection;
    this.launcher = launcher;
    this.bubbles = bubbles;
    this.colorblindBubbles = colorblindBubbles;
  }

  public void saveState(Bundle map, Vector saved_sprites) {
    if (getSavedId() != -1) {
      return;
    }
    super.saveState(map, saved_sprites);
    map.putInt(String.format("%d-currentColor", getSavedId()), currentColor);
    map.putInt(String.format("%d-currentDirection", getSavedId()),
               currentDirection);
  }

  public int getTypeId()
  {
    return Sprite.TYPE_LAUNCH_BUBBLE;
  }

  public void changeColor(int newColor)
  {
    currentColor = newColor;
  }

  public void changeDirection(int newDirection)
  {
    currentDirection = newDirection;
  }

  public final void paint(Canvas c, double scale, int dx, int dy)
  {
    if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL) {
      drawImage(bubbles[currentColor], 302, 390, c, scale, dx, dy);
    } else {
      drawImage(colorblindBubbles[currentColor], 302, 390, c, scale, dx, dy);
    }

    // Draw the scaled and rotated launcher.
    c.save();
    int xCenter = 318;
    int yCenter = 406;
    c.rotate((float)(0.025 * 180 * (currentDirection - 20)),
             (float)(xCenter * scale + dx), (float)(yCenter * scale + dy));
    launcher.setBounds((int)((xCenter - 50) * scale + dx),
                       (int)((yCenter - 50) * scale + dy),
                       (int)((xCenter + 50) * scale + dx),
                       (int)((yCenter + 50) * scale + dy));
    launcher.draw(c);
    c.restore();
  }
}
