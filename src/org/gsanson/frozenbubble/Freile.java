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

import org.jfedor.frozenbubble.BubbleSprite;

import android.view.KeyEvent;

public class Freile implements Opponent, Runnable {

  /** Rotation of the launcher */
  public static final double LAUNCHER_ROTATION = 0.05;
  /** Minimum angle for launcher */
  public static final double MIN_LAUNCHER = -Math.PI / 2. + 0.12;
  /** Maximum angle for launcher */
  public static final double MAX_LAUNCHER = Math.PI / 2. - 0.12;
  /** Ball speed */
  public static final double MOVE_SPEED = 3.;

  private static final int BONUS_POTENTIAL_DETACHED   = 2;
  private static final int BONUS_POTENTIAL_SAME_COLOR = 3;
  private static final int BONUS_SAME_COLOR           = 4;
  private static final int BONUS_DETACHED             = 6;

  /** Default option value */
  private static final int[][] BACKGROUND_GRID = 
  {{0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
   {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
   {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
   {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
   {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
   {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
   {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
   {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};

  //********************************************************************
  // Listener interface for various opponent events
  //********************************************************************

  /*
   * Event types enumeration.
   */
  public static enum eventEnum {
    DONE_COMPUTING;
  }

  /**
   * Opponent event listener user set.
   * @author Glenn Sanson
   *
   */
  public interface OpponentListener {
    public abstract void onOpponentEvent(eventEnum event);
  }

  OpponentListener mOpponentListener;

  public void setOpponentListener (OpponentListener ol) {
    mOpponentListener = ol;
  }

  /* Reference to the managed game grid */
  private BubbleSprite[][] grid;
  /* Current color */
  private int color;
  /* Next color */
  private int nextColor;
  /* Current compressor level */
  private int compressor;
  /* Swap launch bubble with next bubble? */
  private boolean colorSwap;
  /* Calculating new position */
  private boolean computing;
  /* Thread running flag */
  private boolean running;
  /* Best direction */
  private double bestDirection;  
  /* Best ball destination */
  private int[] bestDestination;

  public Freile(BubbleSprite[][] grid) {
    this.grid = grid;
    mOpponentListener = null;
    running = true;

    new Thread(this).start();
  }

  /**
   * Checks if work is still in progress.
   * @return true if the calculation is not yet finished
   */
  public boolean isComputing() {
    return computing;
  }

  public double getExactDirection(double currentDirection) {
    /*
     * currentDirection is not used here.
     */
    return bestDirection;
  }

  public int getAction(double currentDirection) {
    int direction = 0;

    /*
     * If the flag is set to swap the current launch bubble with the
     * next one, then return the appropriate action.
     * 
     * If the angle error is less than a minimum acceptable threshold,
     * cease aiming the launcher and fire the bubble.
     * 
     * Otherwise, rotate the launcher to the appropriate firing angle.
     */
    if (colorSwap) {
      direction = KeyEvent.KEYCODE_DPAD_DOWN;
      colorSwap = false;
    }
    else if (Math.abs(currentDirection - bestDirection) < 0.04) {
      direction = KeyEvent.KEYCODE_DPAD_UP;
    } else {
      if (currentDirection < bestDirection) {
        direction = KeyEvent.KEYCODE_DPAD_RIGHT;
      } else {
        direction = KeyEvent.KEYCODE_DPAD_LEFT;
      }
    }

    return direction;
  }

  public void compute(int currentColor, int nextColor, int compressor) {
    this.color = currentColor;
    this.nextColor = nextColor;
    this.compressor = compressor;
    computing = true;

    synchronized (this) {
      notify();
    }
  }

  public int[] getBallDestination() {
    return bestDestination;
  }

  public void run() {
    while (running) {
      if (computing) {
        computing = false;
        if (mOpponentListener != null)
          mOpponentListener.onOpponentEvent(eventEnum.DONE_COMPUTING);
      }

      while (running && !computing) {
        try {
          synchronized(this) {
            wait(1000);
          }
        } catch (InterruptedException e) {
          // TODO - auto-generated exception handler stub.
          //e.printStackTrace();
        }
      }

      if (running) {
        /*
         * Compute grid options.
         */
        int[][] gridOptions = new int[8][13];
  
        /*
         * Check for best option.
         */
        int bestOption = -1;
        bestDirection = 0.;
        colorSwap = false;
  
        int newOption;
        int[] position = null;
        for (double direction = 0.;
             direction < MAX_LAUNCHER;
             direction += LAUNCHER_ROTATION) {
          position = getCollision(direction);
          newOption = computeOption(position[0], position[1],
                                    color, gridOptions);
          if (newOption > bestOption) {
            bestOption = newOption;
            bestDirection = direction;
            bestDestination = position;
          }        
        }
        for (double direction = -LAUNCHER_ROTATION;
             direction > MIN_LAUNCHER;
             direction -= LAUNCHER_ROTATION) {
          position = getCollision(direction);
          newOption = computeOption(position[0], position[1],
                                    color, gridOptions);
          if (newOption > bestOption) {
            bestOption = newOption;
            bestDirection = direction;
            bestDestination = position;
          }
        }
        if (color != nextColor) {
          for (double direction = 0.;
               direction < MAX_LAUNCHER;
               direction += LAUNCHER_ROTATION) {
            position = getCollision(direction);
            newOption = computeOption(position[0], position[1],
                                      nextColor, gridOptions);
            if (newOption > bestOption) {
              bestOption = newOption;
              bestDirection = direction;
              bestDestination = position;
              colorSwap = true;
            }       
          }
          for (double direction = -LAUNCHER_ROTATION;
               direction > MIN_LAUNCHER;
               direction -= LAUNCHER_ROTATION) {
            position = getCollision(direction);
            newOption = computeOption(position[0], position[1],
                                      nextColor, gridOptions);
            if (newOption > bestOption) {
              bestOption = newOption;
              bestDirection = direction;
              bestDestination = position;
              colorSwap = true;
            }
          }
        }
      }
    }
  }

  private int computeOption(int posX, int posY, int color, int[][] gridOptions) {

    if (gridOptions[posX][posY] == 0) {
      int option = BACKGROUND_GRID[posX][posY];

      int[][] states = CollisionHelper.checkState(posX, posY, color, grid);
      for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 12; j++) {
          if (i != posX || j != posY) {
            switch (states[i][j]) {
              case CollisionHelper.STATE_REMOVE:
                option += BONUS_SAME_COLOR;
              break;
              case CollisionHelper.STATE_POTENTIAL_REMOVE:
                option += BONUS_POTENTIAL_SAME_COLOR;
              break;
              case CollisionHelper.STATE_DETACHED:
                option += BONUS_DETACHED;
              break;
              case CollisionHelper.STATE_POTENTIAL_DETACHED:
                option += BONUS_POTENTIAL_DETACHED;
              break;
            }
          }
        }
      }

      gridOptions[posX][posY] = option;
    } 

    return gridOptions[posX][posY];
  }

  private int[] getCollision(double direction) {

    int[] position = null;

    double speedX = MOVE_SPEED * Math.cos(direction - Math.PI / 2.);
    double speedY = MOVE_SPEED * Math.sin(direction - Math.PI / 2.);

    double posX = 112.;
    double posY = 350. - compressor * 28.;

    while (position == null) {
      posX += speedX;
      posY += speedY;

      if (posX < 0.) {
        posX = - posX;
        speedX = -speedX;
      } else if (posX > 224.) {
        posX = 448. - posX;
        speedX = -speedX;
      }

      /*
       * Check top collision.
       */
      if (posY < 0.) {
        int valX = (int) posX;

        position = new int[2];
        position[0] = valX >> 5;
        if ((valX & 16) > 0) {
          position[0]++;
        }
        position[1] = 0;
      } else {
        /*
         * Check other collision.
         */
        position = CollisionHelper.collide((int) posX, (int) posY, grid);
      }
    }

    return position;
  }

  /**
   * Stop the thread <code>run()</code> execution.
   * <p>Interrupt the thread when it is suspended via
   * <code>wait()</code>.
   */
  public void stopThread() {
    running = false;
    mOpponentListener = null;

    synchronized(this) {
      this.notify();
    }
  }
}
