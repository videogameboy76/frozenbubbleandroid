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

public class CollisionHelper {

  private static final int STATE_INTERMEDIATE_CHECK = -2;
  private static final int STATE_CHECK_NEXT = -1;

  public static final int STATE_UNDEFINED = 0;
  public static final int STATE_POTENTIAL_REMOVE = 1;
  public static final int STATE_REMOVE = 2;
  public static final int STATE_ATTACHED = 3;
  public static final int STATE_POTENTIAL_DETACHED = 4;
  public static final int STATE_DETACHED = 5;

  private CollisionHelper() {}

  /**
   * Checks whether a moving ball collides with fixed balls
   * @param x X-coord of the moving ball, relative to the game area
   * @param y Y-coord of the moving ball, relative to the game area (right under the compressor)
   * @param grid The grid of fixed balls
   * @return the position in the grid of the moving ball or null if no collision occurs
   */
  public static int[] collide(int x, int y, BubbleSprite[][] grid) {

    int minDist = (int)BubbleSprite.minDistance;
    int[] minCoords = null;
    int[][] toCheck = toCheck(x, y);

    // Check for collision
    boolean collision = false;
    int i = 0;
    while (!collision && i < 4) {
      collision = collision(x, y, toCheck[i][0], toCheck[i][1], grid);
      i++;
    }

    // Check for position
    if (collision) {
      minCoords = new int[2];

      for (i = 0; i < 4; i++) {
        minDist = distance(x, y, toCheck[i][0], toCheck[i][1], minDist, minCoords);
      }
    }

    return minCoords;
  }

  /**
   * Calculates the distance between real position and a specific point in the grid
   * @param x real X-coord
   * @param y real Y-coord
   * @param targetX X target point (grid)
   * @param targetY Y target point (grid)
   * @param minDist current minimum distance
   * @param outCoords the coordinates associated with the minimum distance found
   * @return The real distance or the current minDist if the point is out of the grid or empty
   */
  private static int distance(int x, int y, int targetX, int targetY, int minDist, int[] outCoords) {
    int distance = minDist;

    if (targetX >= 0 && targetX < 8 && targetY >= 0 && targetY < 13) {
      int dx = (targetX << 5) - ((targetY % 2) << 4) - x;
      int dy = targetY * 28 - y;

      distance = dx * dx + dy * dy;
      if (distance < minDist) {
        outCoords[0] = targetX;
        outCoords[1] = targetY;
      } else {
        distance = minDist;
      }
    }

    return distance;
  }

  /**
   * Calculates the distance between real position and a specific point in the grid
   * @param x real X-coord
   * @param y real Y-coord
   * @param targetX X target point (grid)
   * @param targetY Y target point (grid)
   * @param grid reference grid
   * @return The real distance or the current minDist if the point is out of the grid or empty
   */
  private static boolean collision(int x, int y, int targetX, int targetY, BubbleSprite[][] grid) {
    boolean collision = false;

    if (targetX >= 0 && targetX < 8 && targetY >= 0 && targetY < 13 && grid[targetX][targetY] != null) {
      int dx = (targetX << 5) - ((targetY % 2) << 4) - x;
      int dy = targetY * 28 - y;

      collision = dx * dx + dy * dy < BubbleSprite.minDistance;
    }

    return collision;
  }

  /**
   * Retrieves the set of position in the grid that are currently under the moving ball 
   * @param x real X-coord
   * @param y real Y-coord
   * @return 
   */
  private static int[][] toCheck(int x, int y) {
    int[][] toCheck = new int[4][2];

    int topY = y / 28;
    int topX = (x + ((topY % 2) << 4)) >> 5;

    toCheck[0][0] = topX;
    toCheck[0][1] = topY;
    toCheck[1][0] = topX + 1;
    toCheck[1][1] = topY;
    toCheck[2][0] = topX + 1 - (topY % 2);
    toCheck[2][1] = topY + 1;

    toCheck[3][1] = topY + 1;    
    if (((x & 16) ^ (((topY & 1) << 4))) == 0) {
      toCheck[3][0] = topX - (topY % 2);
    } else {
      toCheck[3][0] = topX + 2 - (topY % 2);
    }

    return toCheck;
  }

  /**
   * Check state of all bubbles
   * @param x X-coord of the new bubble
   * @param y Y-Coord of the new bubble
   * @param color Color of new new bubble
   * @param grid Grid of all known bubbles
   * @return A grid with all the new states. If the new bubble doesn't change anything, values are only potential
   */
  public static int[][] checkState(int x, int y, int color, BubbleSprite[][] grid) {
    int[][] outGrid = new int[8][13];
    outGrid[x][y] = STATE_REMOVE;
    checkNeighbors(x, y, grid, outGrid, false);
    int nbRemove = 1;

    boolean changed = true;
    while (changed) {
      changed = false;

      for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 13; j++) {
          if (outGrid[i][j] == STATE_CHECK_NEXT) {
            if (isColor(i, j, color, grid, null)) {
              outGrid[i][j] = STATE_REMOVE;
              nbRemove++;
              changed = true;

              checkNeighbors(i, j, grid, outGrid, false);
            } else {
              outGrid[i][j] = STATE_INTERMEDIATE_CHECK;
            }
          }
        }
      }
    }

    // Check for positions that are (potentially) not attached anymore
    for (int i = 0; i < 8; i++) {
      if (grid[i][0] != null && (outGrid[i][0] == STATE_UNDEFINED || outGrid[i][0] == STATE_INTERMEDIATE_CHECK)) {
        outGrid[i][0] = STATE_CHECK_NEXT;
      }
    }

    changed = true;
    while (changed) {
      changed = false;

      for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 13; j++) {
          if (outGrid[i][j] == STATE_CHECK_NEXT) {
            outGrid[i][j] = STATE_ATTACHED;
            changed = true;

            checkNeighbors(i, j, grid, outGrid, true);
          }
        }
      }
    }

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 13; j++) {
        if (grid[i][j] != null && (outGrid[i][j] == STATE_UNDEFINED || outGrid[i][j] == STATE_INTERMEDIATE_CHECK)) {
          if (nbRemove >= 3) {
            outGrid[i][j] = STATE_DETACHED;
          } else {
            outGrid[i][j] = STATE_POTENTIAL_DETACHED;
          }
        }

        if (outGrid[i][j] == STATE_REMOVE && nbRemove < 3) {
          outGrid[i][j] = STATE_POTENTIAL_REMOVE;
        }
      }
    }

    return outGrid;
  }

  private static void checkNeighbors(int x, int y, BubbleSprite[][] grid, int[][] outGrid, boolean ignoreStayState) {

    if (x > 0) {
      changeState(x-1, y, grid, outGrid, ignoreStayState);
    }

    if (x < 7) {
      changeState(x+1, y, grid, outGrid, ignoreStayState);
    }

    if (y > 0) {
      changeState(x, y-1, grid, outGrid, ignoreStayState);
      if (y % 2 == 0) {
        if (x < 7) {
          changeState(x+1, y-1, grid, outGrid, ignoreStayState);
        }
      }
      else {
        if (x > 0) {
          changeState(x-1, y-1, grid, outGrid, ignoreStayState);
        }        
      }
    }

    if (y < 11) {
      changeState(x, y+1, grid, outGrid, ignoreStayState);
      if (y % 2 == 0) {
        if (x < 7) {
          changeState(x+1, y+1, grid, outGrid, ignoreStayState);
        }
      }
      else {
        if (x > 0) {
          changeState(x-1, y+1, grid, outGrid, ignoreStayState);
        }        
      }
    }
  }

  private static void changeState(int x, int y, BubbleSprite[][] grid, int[][] outGrid, boolean ignoreStayState) {
    if (ignoreStayState) {
      if (grid[x][y] != null && (outGrid[x][y] == STATE_UNDEFINED || outGrid[x][y] == STATE_INTERMEDIATE_CHECK)) {
        outGrid[x][y] = STATE_CHECK_NEXT;
      }
    } else {
      if (grid[x][y] != null && outGrid[x][y] == STATE_UNDEFINED) {
        outGrid[x][y] = STATE_CHECK_NEXT;
      }      
    }
  }

  /**
   * Check if a bubble falling at a given position has a neighbor of the same color
   * @param x grid X-Coord of the bubble 
   * @param y grid Y-Coord of the bubble
   * @param color Color of the bubble
   * @param grid Grid of all known bubbles
   * @param alreadyChecked Balls alread reviewed (may be null)
   * @return
   */
  private static boolean hasNeighbor(int x, int y, int color, BubbleSprite[][] grid, boolean[][] alreadyChecked) {
    boolean neighbor = false;

    if (x > 0) {
      neighbor |= isColor(x-1, y, color, grid, alreadyChecked);
    }

    if (x < 7) {
      neighbor |= isColor(x+1, y, color, grid, alreadyChecked);
    }

    if (y > 0) {
      neighbor |= isColor(x, y-1, color, grid, alreadyChecked);
      if (y % 2 == 0) {
        if (x < 7) {
          neighbor |= isColor(x+1, y-1, color, grid, alreadyChecked);
        }
      }
      else {
        if (x > 0) {
          neighbor |= isColor(x-1, y-1, color, grid, alreadyChecked);
        }        
      }
    }

    if (y < 11) {
      neighbor |= isColor(x, y+1, color, grid, alreadyChecked);
      if (y % 2 == 0) {
        if (x < 7) {
          neighbor |= isColor(x+1, y+1, color, grid, alreadyChecked);
        }
      }
      else {
        if (x > 0) {
          neighbor |= isColor(x-1, y+1, color, grid, alreadyChecked);
        }        
      }
    }

    return neighbor;
  }

  /**
   * Check if a specific position is of a given color
   * @param x
   * @param y
   * @param color
   * @param grid
   * @param alreadyChecked
   * @return
   */
  private static boolean isColor(int x, int y, int color, BubbleSprite[][] grid, boolean[][] alreadyChecked) {
    boolean isColor = false;

    if (grid[x][y] != null && grid[x][y].getColor() == color && (alreadyChecked == null || !alreadyChecked[x][y])) {
      isColor = true;
      if (alreadyChecked != null) {
        alreadyChecked[x][y] = true;
      }
    }

    return isColor;
  }

  // TODO : A remplacer par une version prenant toutes les boules d'un coup???
  // XXX utiliser hasNeighbor

  /**
   * @param grid
   * @return {x, y, points} or null if no position is available
   */
  public static int[] chainReaction(int color, BubbleSprite[][] grid) {

    int bestX = 0;
    int bestY = 0;
    int bestCount = 0;

    int[] output = null;
    boolean[][] alreadyChecked = new boolean[8][13]; 

    for (int j = 0; j < 13; j++) {
      for (int i = 0; i < 8; i++) {
        if (grid[i][j] == null && (i != 0 || (j & 1) == 0)) {
          if (hasNeighbor(i, j, color, grid, alreadyChecked)) {

            // Check grid
            int newCount = 0;
            int[][] cGrid = checkState(i, j, color, grid);

            if (cGrid != null) {            
              for (int cj = 0; cj < 13; cj++) {
                for (int ci = 0; ci < 8; ci++) {
                  if (cGrid[ci][cj] == STATE_REMOVE || cGrid[ci][cj] == STATE_DETACHED) {
                    newCount++;
                    alreadyChecked[ci][cj] = true;
                  }
                }
              }

              if (newCount > bestCount) {
                bestX = i;
                bestY = j;
                bestCount = newCount;
              }
            }
          }
        }
      }
    }

    if (bestCount > 0) {
      output = new int[] {bestX, bestY, bestCount};
    }

    return output;
  }
}
