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

import java.util.Vector;
import java.util.Random;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;

public class BubbleSprite extends Sprite
{
  private static double FALL_SPEED = 1.;
  private static double MAX_BUBBLE_SPEED = 8.;
  private static double MINIMUM_DISTANCE = 841.;

  private int color;
  private BmpWrap bubbleFace;
  private BmpWrap bubbleBlindFace;
  private BmpWrap frozenFace;
  private BmpWrap bubbleBlink;
  private BmpWrap[] bubbleFixed;
  private FrozenGame frozen;
  private BubbleManager bubbleManager;
  private double moveX, moveY;
  private double realX, realY;

  private boolean fixed;
  private boolean blink;
  private boolean released;

  private boolean checkJump;
  private boolean checkFall;

  private int fixedAnim;

  private SoundManager soundManager;

  public void saveState(Bundle map, Vector savedSprites) {
    if (getSavedId() != -1) {
      return;
    }
    super.saveState(map, savedSprites);
    map.putInt(String.format("%d-color", getSavedId()), color);
    map.putDouble(String.format("%d-moveX", getSavedId()), moveX);
    map.putDouble(String.format("%d-moveY", getSavedId()), moveY);
    map.putDouble(String.format("%d-realX", getSavedId()), realX);
    map.putDouble(String.format("%d-realY", getSavedId()), realY);
    map.putBoolean(String.format("%d-fixed", getSavedId()), fixed);
    map.putBoolean(String.format("%d-blink", getSavedId()), blink);
    map.putBoolean(String.format("%d-released", getSavedId()), released);
    map.putBoolean(String.format("%d-checkJump", getSavedId()), checkJump);
    map.putBoolean(String.format("%d-checkFall", getSavedId()), checkFall);
    map.putInt(String.format("%d-fixedAnim", getSavedId()), fixedAnim);
    map.putBoolean(String.format("%d-frozen", getSavedId()),
                   bubbleFace == frozenFace ? true : false);
  }

  public int getTypeId()
  {
    return Sprite.TYPE_BUBBLE;
  }

  public BubbleSprite(Rect area, int color, double moveX, double moveY,
                      double realX, double realY, boolean fixed, boolean blink,
                      boolean released, boolean checkJump, boolean checkFall,
                      int fixedAnim, BmpWrap bubbleFace,
                      BmpWrap bubbleBlindFace, BmpWrap frozenFace,
                      BmpWrap[] bubbleFixed, BmpWrap bubbleBlink,
                      BubbleManager bubbleManager, SoundManager soundManager,
                      FrozenGame frozen)
  {
    super(area);
    this.color = color;
    this.moveX = moveX;
    this.moveY = moveY;
    this.realX = realX;
    this.realY = realY;
    this.fixed = fixed;
    this.blink = blink;
    this.released = released;
    this.checkJump = checkJump;
    this.checkFall = checkFall;
    this.fixedAnim = fixedAnim;
    this.bubbleFace = bubbleFace;
    this.bubbleBlindFace = bubbleBlindFace;
    this.frozenFace = frozenFace;
    this.bubbleFixed = bubbleFixed;
    this.bubbleBlink = bubbleBlink;
    this.bubbleManager = bubbleManager;
    this.soundManager = soundManager;
    this.frozen = frozen;
  }

  public BubbleSprite(Rect area, int direction, int color, BmpWrap bubbleFace,
                      BmpWrap bubbleBlindFace, BmpWrap frozenFace,
                      BmpWrap[] bubbleFixed, BmpWrap bubbleBlink,
                      BubbleManager bubbleManager, SoundManager soundManager,
                      FrozenGame frozen)
  {
    super(area);

    this.color = color;
    this.bubbleFace = bubbleFace;
    this.bubbleBlindFace = bubbleBlindFace;
    this.frozenFace = frozenFace;
    this.bubbleFixed = bubbleFixed;
    this.bubbleBlink = bubbleBlink;
    this.bubbleManager = bubbleManager;
    this.soundManager = soundManager;
    this.frozen = frozen;

    this.moveX = MAX_BUBBLE_SPEED * -Math.cos(direction * Math.PI / 40.);
    this.moveY = MAX_BUBBLE_SPEED * -Math.sin(direction * Math.PI / 40.);
    this.realX = area.left;
    this.realY = area.top;

    fixed = false;
    fixedAnim = -1;
  }

  public BubbleSprite(Rect area, int color, BmpWrap bubbleFace,
                      BmpWrap bubbleBlindFace, BmpWrap frozenFace,
                      BmpWrap bubbleBlink, BubbleManager bubbleManager,
                      SoundManager soundManager, FrozenGame frozen)
  {
    super(area);

    this.color = color;
    this.bubbleFace = bubbleFace;
    this.bubbleBlindFace = bubbleBlindFace;
    this.frozenFace = frozenFace;
    this.bubbleBlink = bubbleBlink;
    this.bubbleManager = bubbleManager;
    this.soundManager = soundManager;
    this.frozen = frozen;

    this.realX = area.left;
    this.realY = area.top;

    fixed = true;
    fixedAnim = -1;
    bubbleManager.addBubble(bubbleFace);
  }

  Point currentPosition()
  {
    int posY = (int)Math.floor((realY-28.-frozen.getMoveDown())/28.);
    int posX = (int)Math.floor((realX-174.)/32. + 0.5*(posY%2));

    if (posX>7) {
      posX = 7;
    }

    if (posX<0) {
      posX = 0;
    }

    if (posY<0) {
      posY = 0;
    }

    return new Point(posX, posY);
  }

  public void removeFromManager()
  {
    bubbleManager.removeBubble(bubbleFace);
  }

  public boolean fixed()
  {
    return fixed;
  }

  public boolean checked()
  {
    return checkFall;
  }

  public boolean released()
  {
    return released;
  }

  public void moveDown()
  {
    if (fixed) {
      realY += 28.;
    }

    super.absoluteMove(new Point((int)realX, (int)realY));
  }

  public void move()
  {
    realX += moveX;

    if (realX>=414.) {
      moveX = -moveX;
      realX += (414. - realX);
      soundManager.playSound(FrozenBubble.SOUND_REBOUND);
    } else if (realX<=190.) {
      moveX = -moveX;
      realX += (190. - realX);
      soundManager.playSound(FrozenBubble.SOUND_REBOUND);
    }

    realY += moveY;

    Point currentPosition = currentPosition();
    Vector neighbors = getNeighbors(currentPosition);

    if (checkCollision(neighbors) || realY < 44.+frozen.getMoveDown()) {
      realX = 190.+currentPosition.x*32-(currentPosition.y%2)*16;
      realY = 44.+currentPosition.y*28+frozen.getMoveDown();

      fixed = true;

      Vector checkJump = new Vector();
      this.checkJump(checkJump, neighbors);

      BubbleSprite[][] grid = frozen.getGrid();

      if (checkJump.size() >= 3) {
        released = true;

        for (int i=0 ; i<checkJump.size() ; i++) {
          BubbleSprite current = (BubbleSprite)checkJump.elementAt(i);
          Point currentPoint = current.currentPosition();

          frozen.addJumpingBubble(current);
          if (i>0) {
            current.removeFromManager();
          }
          grid[currentPoint.x][currentPoint.y] = null;
        }

        for (int i=0 ; i<8 ; i++) {
          if (grid[i][0] != null) {
            grid[i][0].checkFall();
          }
        }

        for (int i=0 ; i<8 ; i++) {
          for (int j=0 ; j<12 ; j++) {
            if (grid[i][j] != null) {
              if (!grid[i][j].checked()) {
                frozen.addFallingBubble(grid[i][j]);
                grid[i][j].removeFromManager();
                grid[i][j] = null;
              }
            }
          }
        }

        soundManager.playSound(FrozenBubble.SOUND_DESTROY);
      } else {
        bubbleManager.addBubble(bubbleFace);
        grid[currentPosition.x][currentPosition.y] = this;
        moveX = 0.;
        moveY = 0.;
        fixedAnim = 0;
        soundManager.playSound(FrozenBubble.SOUND_STICK);
      }
    }

    super.absoluteMove(new Point((int)realX, (int)realY));
  }

  Vector getNeighbors(Point p)
  {
    BubbleSprite[][] grid = frozen.getGrid();

    Vector list = new Vector();

    if ((p.y % 2) == 0) {
      if (p.x > 0) {
        list.addElement(grid[p.x-1][p.y]);
      }

      if (p.x < 7) {
        list.addElement(grid[p.x+1][p.y]);

        if (p.y > 0) {
          list.addElement(grid[p.x][p.y-1]);
          list.addElement(grid[p.x+1][p.y-1]);
        }

        if (p.y < 12) {
          list.addElement(grid[p.x][p.y+1]);
          list.addElement(grid[p.x+1][p.y+1]);
        }
      } else {
        if (p.y > 0) {
          list.addElement(grid[p.x][p.y-1]);
        }

        if (p.y < 12) {
          list.addElement(grid[p.x][p.y+1]);
        }
      }
    } else {
      if (p.x < 7) {
        list.addElement(grid[p.x+1][p.y]);
      }

      if (p.x > 0) {
        list.addElement(grid[p.x-1][p.y]);

        if (p.y > 0) {
          list.addElement(grid[p.x][p.y-1]);
          list.addElement(grid[p.x-1][p.y-1]);
        }

        if (p.y < 12) {
          list.addElement(grid[p.x][p.y+1]);
          list.addElement(grid[p.x-1][p.y+1]);
        }
      } else {
        if (p.y > 0) {
          list.addElement(grid[p.x][p.y-1]);
        }

        if (p.y < 12) {
          list.addElement(grid[p.x][p.y+1]);
        }
      }
    }

    return list;
  }

  void checkJump(Vector jump, BmpWrap compare)
  {
    if (checkJump) {
      return;
    }
    checkJump = true;

    if (this.bubbleFace == compare) {
      checkJump(jump, this.getNeighbors(this.currentPosition()));
    }
  }

  void checkJump(Vector jump, Vector neighbors)
  {
    jump.addElement(this);

    for (int i=0 ; i<neighbors.size() ; i++) {
      BubbleSprite current = (BubbleSprite)neighbors.elementAt(i);

      if (current != null) {
        current.checkJump(jump, this.bubbleFace);
      }
    }
  }

  public void checkFall()
  {
    if (checkFall) {
      return;
    }
    checkFall = true;

    Vector v = this.getNeighbors(this.currentPosition());

    for (int i=0 ; i<v.size() ; i++) {
      BubbleSprite current = (BubbleSprite)v.elementAt(i);

      if (current != null) {
        current.checkFall();
      }
    }
  }

  boolean checkCollision(Vector neighbors)
  {
    for (int i=0 ; i<neighbors.size() ; i++) {
      BubbleSprite current = (BubbleSprite)neighbors.elementAt(i);

      if (current != null) {
        if (checkCollision(current)) {
          return true;
        }
      }
    }

    return false;
  }

  boolean checkCollision(BubbleSprite sprite)
  {
    double value =
        (sprite.getSpriteArea().left - this.realX) *
        (sprite.getSpriteArea().left - this.realX) +
        (sprite.getSpriteArea().top - this.realY) *
        (sprite.getSpriteArea().top - this.realY);

    return (value < MINIMUM_DISTANCE);
  }

  public void jump()
  {
    if (fixed) {
      moveX = -6. + frozen.getRandom().nextDouble() * 12.;
      moveY = -5. - frozen.getRandom().nextDouble() * 10. ;

      fixed = false;
    }

    moveY += FALL_SPEED;
    realY += moveY;
    realX += moveX;

    super.absoluteMove(new Point((int)realX, (int)realY));

    if (realY >= 680.) {
      frozen.deleteJumpingBubble(this);
    }
  }

  public void fall()
  {
    if (fixed) {
      moveY = frozen.getRandom().nextDouble()* 5.;
    }

    fixed = false;

    moveY += FALL_SPEED;
    realY += moveY;

    super.absoluteMove(new Point((int)realX, (int)realY));

    if (realY >= 680.) {
      frozen.deleteFallingBubble(this);
    }
  }

  public void blink()
  {
    blink = true;
  }

  public void frozenify()
  {
    changeSpriteArea(new Rect(getSpritePosition().x-1, getSpritePosition().y-1,
                              34, 42));
    bubbleFace = frozenFace;
  }

  public final void paint(Canvas c, double scale, int dx, int dy)
  {
    checkJump = false;
    checkFall = false;

    Point p = getSpritePosition();

    if (blink && bubbleFace != frozenFace) {
      blink = false;
      drawImage(bubbleBlink, p.x, p.y, c, scale, dx, dy);
    } else {
      if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL ||
          bubbleFace == frozenFace) {
        drawImage(bubbleFace, p.x, p.y, c, scale, dx, dy);
      } else {
        drawImage(bubbleBlindFace, p.x, p.y, c, scale, dx, dy);
      }
    }

    if (fixedAnim != -1) {
      drawImage(bubbleFixed[fixedAnim], p.x, p.y, c, scale, dx, dy);
      fixedAnim++;
      if (fixedAnim == 6) {
        fixedAnim = -1;
      }
    }
  }
}
