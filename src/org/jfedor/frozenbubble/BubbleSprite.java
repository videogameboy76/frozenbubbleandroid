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
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;

public class BubbleSprite extends Sprite {
  public static final int MIN_PIX = 20;
  public static final int MAX_PIX = 29;
  public static double minDistance = MIN_PIX * MIN_PIX;

  private static final double FALL_SPEED       = 1.;
  private static final double MAX_BUBBLE_SPEED = 8.;
  private static final double GO_UP_SPEED      = 20.;

  private int           color;
  private int           fixedAnim;
  private BmpWrap       bubbleFace;
  private BmpWrap       bubbleBlindFace;
  private BmpWrap       frozenFace;
  private BmpWrap       bubbleBlink;
  private BmpWrap[]     bubbleFixed;
  private FrozenGame    frozen;
  private BubbleManager bubbleManager;
  private SoundManager  soundManager;
  private double        moveX, moveY;
  private double        realX, realY;
  private Point         lastOpenPosition;

  private boolean blink;
  private boolean checkFall;
  private boolean checkJump;
  private boolean fixed;
  private boolean released;

  /**
   * Class constructor used when restoring the game state from a bundle.
   */
  public BubbleSprite(Rect area, int color, double moveX, double moveY,
                      double realX, double realY, boolean fixed, boolean blink,
                      boolean released, boolean checkJump, boolean checkFall,
                      int fixedAnim, BmpWrap bubbleFace,
                      Point lastOpenPosition,
                      BmpWrap bubbleBlindFace, BmpWrap frozenFace,
                      BmpWrap[] bubbleFixed, BmpWrap bubbleBlink,
                      BubbleManager bubbleManager, SoundManager soundManager,
                      FrozenGame frozen) {
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
    this.lastOpenPosition = new Point(lastOpenPosition);
  }

  /**
   * Class constructor used when creating a launched bubble.
   */
  public BubbleSprite(Rect area, double direction, int color, BmpWrap bubbleFace,
                      BmpWrap bubbleBlindFace, BmpWrap frozenFace,
                      BmpWrap[] bubbleFixed, BmpWrap bubbleBlink,
                      BubbleManager bubbleManager, SoundManager soundManager,
                      FrozenGame frozen) {
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
    this.lastOpenPosition = currentPosition();

    fixed = false;
    fixedAnim = -1;
  }

  /**
   * Class constructor used when initializing a new level.
   */
  public BubbleSprite(Rect area, int color, BmpWrap bubbleFace,
                      BmpWrap bubbleBlindFace, BmpWrap frozenFace,
                      BmpWrap bubbleBlink, BubbleManager bubbleManager,
                      SoundManager soundManager, FrozenGame frozen) {
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
    this.lastOpenPosition = currentPosition();

    fixed = true;
    fixedAnim = -1;
    addToManager();
  }

  public void addToManager() {
    bubbleManager.addBubble(bubbleFace);
  }

  public void blink() {
    blink = true;
  }

  boolean checkCollision(Vector<BubbleSprite> neighbors) {
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

  boolean checkCollision(BubbleSprite sprite) {
    double value =
      (sprite.getSpriteArea().left - this.realX) *
      (sprite.getSpriteArea().left - this.realX) +
      (sprite.getSpriteArea().top - this.realY) *
      (sprite.getSpriteArea().top - this.realY);

    return value < minDistance;
  }

  public boolean checked() {
    return checkFall;
  }

  public void checkFall() {
    if (checkFall) {
      return;
    }

    checkFall = true;
    Vector<BubbleSprite> v = this.getNeighbors(this.lastOpenPosition);

    for (int i=0 ; i<v.size() ; i++) {
      BubbleSprite current = (BubbleSprite)v.elementAt(i);

      if (current != null) {
        current.checkFall();
      }
    }
  }

  void checkJump(Vector<Sprite> jump, BmpWrap compare) {
    if (checkJump) {
      return;
    }

    checkJump = true;

    if (this.bubbleFace == compare) {
      checkJump(jump, this.getNeighbors(this.lastOpenPosition));
    }
  }

  void checkJump(Vector<Sprite> jump, Vector<BubbleSprite> neighbors) {
    jump.addElement(this);

    for (int i = 0; i < neighbors.size(); i++) {
      BubbleSprite current = (BubbleSprite)neighbors.elementAt(i);

      if (current != null) {
        current.checkJump(jump, this.bubbleFace);
      }
    }
  }

  Point currentPosition() {
    int posY = (int)Math.floor((realY-28.-frozen.getMoveDown())/28.);
    int posX = (int)Math.floor((realX-174.)/32. + 0.5*(posY%2));

    if (posX > (LevelManager.NUM_COLS - 1)) {
      posX = LevelManager.NUM_COLS - 1;
    }

    if (posX < 0) {
      posX = 0;
    }

    if (posY < 0) {
      posY = 0;
    }

    return new Point(posX, posY);
  }

  public int getColor() {
    return this.color;
  }

  public boolean fixed() {
    return fixed;
  }

  public void frozenify() {
    changeSpriteArea(new Rect(getSpritePosition().x-1,
                              getSpritePosition().y-1, 34, 42));
    bubbleFace = frozenFace;
  }

  Vector<BubbleSprite> getNeighbors(Point p) {
    BubbleSprite[][] grid = frozen.getGrid();
    Vector<BubbleSprite> list = new Vector<BubbleSprite>();

    if ((p.y % 2) == 0) {
      if (p.x > 0) {
        list.addElement(grid[p.x-1][p.y]);
      }

      if (p.x < (LevelManager.NUM_COLS - 1)) {
        list.addElement(grid[p.x+1][p.y]);

        if (p.y > 0) {
          list.addElement(grid[p.x][p.y-1]);
          list.addElement(grid[p.x+1][p.y-1]);
        }

        if (p.y < (LevelManager.NUM_ROWS - 1)) {
          list.addElement(grid[p.x][p.y+1]);
          list.addElement(grid[p.x+1][p.y+1]);
        }
      }
      else {
        if (p.y > 0) {
          list.addElement(grid[p.x][p.y-1]);
        }

        if (p.y < (LevelManager.NUM_ROWS - 1)) {
          list.addElement(grid[p.x][p.y+1]);
        }
      }
    }
    else {
      if (p.x < (LevelManager.NUM_COLS - 1)) {
        list.addElement(grid[p.x+1][p.y]);
      }

      if (p.x > 0) {
        list.addElement(grid[p.x-1][p.y]);

        if (p.y > 0) {
          list.addElement(grid[p.x][p.y-1]);
          list.addElement(grid[p.x-1][p.y-1]);
        }

        if (p.y < (LevelManager.NUM_ROWS - 1)) {
          list.addElement(grid[p.x][p.y+1]);
          list.addElement(grid[p.x-1][p.y+1]);
        }
      }
      else {
        if (p.y > 0) {
          list.addElement(grid[p.x][p.y-1]);
        }

        if (p.y < (LevelManager.NUM_ROWS - 1)) {
          list.addElement(grid[p.x][p.y+1]);
        }
      }
    }

    return list;
  }

  public int getTypeId() {
    return Sprite.TYPE_BUBBLE;
  }

  public void fall() {
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

  public void goUp() {
    realX += moveX;

    if (realX>=414.) {
      moveX = -moveX;
      realX += (414. - realX);
    }
    else if (realX<=190.) {
      moveX = -moveX;
      realX += (190. - realX);
    }

    moveY = -GO_UP_SPEED;
    realY += moveY;
    Point currentPosition = currentPosition();
    /*
     * Only check for collisions if the current position of the attack
     * bubble corresponds to a fixed grid location.  Otherwise just move
     * the bubble.
     */
    if ((currentPosition.x >= 0) &&
        (currentPosition.x < LevelManager.NUM_COLS) &&
        (currentPosition.y >= 0) &&
        (currentPosition.y < LevelManager.NUM_ROWS)) {
      BubbleSprite[][] grid = frozen.getGrid();

      if (grid[currentPosition.x][currentPosition.y] == null)
        lastOpenPosition = currentPosition;

      Vector<BubbleSprite> neighbors = getNeighbors(lastOpenPosition);

      if (checkCollision(neighbors) || realY < 44.+frozen.getMoveDown()) {
        realX = 190.+lastOpenPosition.x*32-(lastOpenPosition.y%2)*16;
        realY = 44.+lastOpenPosition.y*28+frozen.getMoveDown();
        fixed = true;
        super.absoluteMove(new Point((int)realX, (int)realY));

        if (!this.register(grid, lastOpenPosition)) {
          frozen.removeSprite(this);
          frozen.malusBar.addBubbles(1);
        }
        else {
          addToManager();
          moveX = 0.;
          moveY = 0.;
          fixedAnim = 0;
        }
        frozen.deleteGoingUpBubble(this);
        return;
      }
    }

    super.absoluteMove(new Point((int)realX, (int)realY));
  }

  public void jump() {
    if (fixed) {
      moveX = -6. + frozen.getRandom().nextDouble() * 12.;
      moveY = -5. - frozen.getRandom().nextDouble() * 10.;
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

  public void move() {
    realX += moveX;

    if (realX>=414.) {
      moveX = -moveX;
      realX += (414. - realX);
      soundManager.playSound(FrozenBubble.SOUND_REBOUND);
    }
    else if (realX<=190.) {
      moveX = -moveX;
      realX += (190. - realX);
      soundManager.playSound(FrozenBubble.SOUND_REBOUND);
    }

    realY += moveY;
    Point currentPosition = currentPosition();
    BubbleSprite[][] grid = frozen.getGrid();

    if (grid[currentPosition.x][currentPosition.y] == null)
      lastOpenPosition = currentPosition;

    Vector<BubbleSprite> neighbors = getNeighbors(lastOpenPosition);

    if (checkCollision(neighbors) || realY < 44.+frozen.getMoveDown()) {
      realX = 190.+lastOpenPosition.x*32-(lastOpenPosition.y%2)*16;
      realY = 44.+lastOpenPosition.y*28+frozen.getMoveDown();
      fixed = true;

      Vector<Sprite> checkJump = new Vector<Sprite>();
      this.checkJump(checkJump, neighbors);

      if (checkJump.size() >= 3) {
        released = true;
        frozen.addAttackBubbles(checkJump.size() - 3);

        for (int i=0 ; i<checkJump.size() ; i++) {
          BubbleSprite current = (BubbleSprite)checkJump.elementAt(i);
          Point currentPoint = current.currentPosition();
          frozen.addJumpingBubble(current);

          if (i>0) {
            current.removeFromManager();
          }
          grid[currentPoint.x][currentPoint.y] = null;
        }

        for (int i = 0; i < LevelManager.NUM_COLS; i++) {
          if (grid[i][0] != null) {
            grid[i][0].checkFall();
          }
        }

        for (int i = 0; i < LevelManager.NUM_COLS; i++) {
          for (int j = 0; j < (LevelManager.NUM_ROWS - 1); j++) {
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
      }
      else if (!this.register(grid, lastOpenPosition)) {
        /*
         * If the moving bubble failed to register because the grid
         * location it would fill is already occupied, simply remove
         * the sprite, but otherwise act like it became affixed.
         */
        frozen.removeSprite(this);
        soundManager.playSound(FrozenBubble.SOUND_STICK);
        return;
      }
      else {
        addToManager();
        moveX = 0.;
        moveY = 0.;
        fixedAnim = 0;
        soundManager.playSound(FrozenBubble.SOUND_STICK);
      }
    }

    super.absoluteMove(new Point((int)realX, (int)realY));
  }

  public void moveDown() {
    if (fixed) {
      realY += 28.;
    }

    super.absoluteMove(new Point((int)realX, (int)realY));
  }

  public final void paint(Canvas c, double scale, int dx, int dy) {
    checkJump = false;
    checkFall = false;
    Point p = getSpritePosition();

    if (blink && bubbleFace != frozenFace) {
      blink = false;
      drawImage(bubbleBlink, p.x, p.y, c, scale, dx, dy);
    }
    else {
      if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL ||
          bubbleFace == frozenFace) {
        drawImage(bubbleFace, p.x, p.y, c, scale, dx, dy);
      }
      else {
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

  /**
   * Adds a bubble to the fixed grid.
   * @param grid - the array of fixed bubbles.
   * @param position - the position in the grid to check for occupancy.
   * @return true if the bubble becomes registered in the grid (false if
   * another bubble already occupies the same position).
   */
  public boolean register(BubbleSprite[][] grid, Point position) {
    boolean register = grid[position.x][position.y] == null;

    if (register)
      grid[position.x][position.y] = this;

    return register;
  }

  public boolean released() {
    return released;
  }

  public void removeFromManager() {
    bubbleManager.removeBubble(bubbleFace);
  }

  public void saveState(Bundle map, Vector<Sprite> savedSprites, int id) {
    if (getSavedId() != -1) {
      return;
    }
    super.saveState(map, savedSprites, id);
    map.putInt(String.format("%d-%d-color", id, getSavedId()), color);
    map.putDouble(String.format("%d-%d-moveX", id, getSavedId()), moveX);
    map.putDouble(String.format("%d-%d-moveY", id, getSavedId()), moveY);
    map.putDouble(String.format("%d-%d-realX", id, getSavedId()), realX);
    map.putDouble(String.format("%d-%d-realY", id, getSavedId()), realY);
    map.putBoolean(String.format("%d-%d-fixed", id, getSavedId()), fixed);
    map.putBoolean(String.format("%d-%d-blink", id, getSavedId()), blink);
    map.putBoolean(String.format("%d-%d-released", id, getSavedId()),
                   released);
    map.putBoolean(String.format("%d-%d-checkJump", id, getSavedId()),
                   checkJump);
    map.putBoolean(String.format("%d-%d-checkFall", id, getSavedId()),
                   checkFall);
    map.putInt(String.format("%d-%d-fixedAnim", id, getSavedId()), fixedAnim);
    map.putBoolean(String.format("%d-%d-frozen", id, getSavedId()),
                   bubbleFace == frozenFace ? true : false);
    map.putInt(String.format("%d-%d-lastOpenPosition.x", id, getSavedId()),
               lastOpenPosition.x);
    map.putInt(String.format("%d-%d-lastOpenPosition.y", id, getSavedId()),
               lastOpenPosition.y);
  }

  public static void setCollisionThreshold(int collision) {
    minDistance = collision * collision;
  }
}
