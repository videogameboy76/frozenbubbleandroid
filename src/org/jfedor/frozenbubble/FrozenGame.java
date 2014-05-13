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

import org.gsanson.frozenbubble.MalusBar;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.efortin.frozenbubble.CRC16;
import com.efortin.frozenbubble.HighscoreManager;
import com.efortin.frozenbubble.NetworkGameManager;
import com.efortin.frozenbubble.VirtualInput;

public class FrozenGame extends GameScreen {
  private final int[] columnX = { 190, 206, 232, 248, 264,
                                  280, 296, 312, 328, 344,
                                  360, 376, 392, 408, 424 };

  public final static int HORIZONTAL_MOVE = 0;
  public final static int FIRE            = 1;

  public final static double MIN_LAUNCH_DIRECTION   = 1.0;
  public final static double START_LAUNCH_DIRECTION = 20.0;
  public final static double MAX_LAUNCH_DIRECTION   = 39.0;

  public final static int KEY_UP    = 38;
  public final static int KEY_LEFT  = 37;
  public final static int KEY_RIGHT = 39;
  public final static int KEY_SHIFT = 16;

  public static final int HURRY_ME_TIME = 480;
  public static final int RELEASE_TIME  = 300;

  BmpWrap background;
  BmpWrap[] bubbles;
  BmpWrap[] bubblesBlind;
  BmpWrap[] frozenBubbles;
  BmpWrap[] targetedBubbles;
  Random random;

  LaunchBubbleSprite launchBubble;
  double launchBubblePosition;

  PenguinSprite penguin;

  byte newSteps;
  Compressor compressor;

  ImageSprite nextBubble;
  int currentColor, nextColor, newNextColor;

  BubbleSprite movingBubble;
  BubbleManager bubbleManager;
  LevelManager levelManager;
  MalusBar malusBar;
  HighscoreManager highscoreManager;
  NetworkGameManager networkManager;

  Vector<Sprite> falling;
  Vector<Sprite> goingUp;
  Vector<Sprite> jumping;

  BubbleSprite[][] bubblePlay;
  byte[][] newGrid;

  BmpWrap gameWon, gameLost;

  BmpWrap bubbleBlink;
  int blinkDelay;

  ImageSprite hurrySprite;
  int hurryTime;

  ImageSprite pauseButtonSprite;
  ImageSprite playButtonSprite;
  ImageSprite pausedSprite;

  SoundManager soundManager;

  boolean endOfGame;
  boolean frozenify;
  boolean isRemote;
  boolean readyToFire;
  boolean swapPressed;
  gameEnum playResult;
  short gridChecksum;
  int fixedBubbles;
  int frozenifyX, frozenifyY;
  int nbBubbles;
  int player;
  int sendToOpponent;

  Drawable launcher;
  BmpWrap penguins;

  public FrozenGame(BmpWrap background_arg,
                    BmpWrap[] bubbles_arg,
                    BmpWrap[] bubblesBlind_arg,
                    BmpWrap[] frozenBubbles_arg,
                    BmpWrap[] targetedBubbles_arg,
                    BmpWrap bubbleBlink_arg,
                    BmpWrap gameWon_arg,
                    BmpWrap gameLost_arg,
                    BmpWrap gamePaused_arg,
                    BmpWrap hurry_arg,
                    BmpWrap pauseButton_arg,
                    BmpWrap playButton_arg,
                    BmpWrap penguins_arg,
                    BmpWrap compressorHead_arg,
                    BmpWrap compressor_arg,
                    MalusBar malusBar_arg,
                    Drawable launcher_arg,
                    SoundManager soundManager_arg,
                    LevelManager levelManager_arg,
                    HighscoreManager highscoreManager_arg,
                    NetworkGameManager networkManager_arg,
                    VirtualInput input_arg) {
    random               = new Random(System.currentTimeMillis());
    launcher             = launcher_arg;
    penguins             = penguins_arg;
    background           = background_arg;
    bubbles              = bubbles_arg;
    bubblesBlind         = bubblesBlind_arg;
    frozenBubbles        = frozenBubbles_arg;
    targetedBubbles      = targetedBubbles_arg;
    bubbleBlink          = bubbleBlink_arg;
    gameWon              = gameWon_arg;
    gameLost             = gameLost_arg;
    soundManager         = soundManager_arg;
    levelManager         = levelManager_arg;
    highscoreManager     = highscoreManager_arg;
    networkManager       = networkManager_arg;
    malusBar             = malusBar_arg;
    playResult           = gameEnum.PLAYING;
    launchBubblePosition = START_LAUNCH_DIRECTION;
    readyToFire          = false;
    swapPressed          = false;

    /*
     * Initialize game modifier variables.
     */
    if (input_arg != null) {
      player    = input_arg.playerID;
      isRemote  = input_arg.isRemote;
    }
    else {
      player    = VirtualInput.PLAYER1;
      isRemote  = false;
    }

    /*
     * Create objects for all the game graphics.
     */
    if ((pauseButton_arg != null) && (playButton_arg != null)) {
      pauseButtonSprite = new ImageSprite(new Rect(167, 444, 32, 32),
                                          pauseButton_arg);
      playButtonSprite  = new ImageSprite(new Rect(167, 444, 32, 32),
                                          playButton_arg);
      this.addSprite(pauseButtonSprite);
    }
    else {
      pauseButtonSprite = null;
      playButtonSprite  = null;
    }

    penguin = new PenguinSprite(getPenguinRect(player), penguins_arg, random);
    this.addSprite(penguin);

    newSteps     = -1;
    compressor   = new Compressor(compressorHead_arg, compressor_arg);
    hurrySprite  = new ImageSprite(new Rect(203, 265, 203 + 240, 265 + 90),
                                   hurry_arg);
    pausedSprite = new ImageSprite(new Rect(152, 190, 337, 116),
                                   gamePaused_arg);

    if (malusBar != null)
      this.addSprite(malusBar);

    falling = new Vector<Sprite>();
    goingUp = new Vector<Sprite>();
    jumping = new Vector<Sprite>();

    bubblePlay    = new BubbleSprite[8][13];
    bubbleManager = new BubbleManager(bubbles);
    newGrid       = null;

    /*
     * Load the current level to the bubble play grid.
     */
    byte[][] currentLevel = levelManager.getCurrentLevel();
    if (currentLevel == null) {
      //Log.i("frozen-bubble", "Level not available.");
      return;
    }

    for (int j = 0; j < 12; j++) {
      for (int i = j%2; i < 8; i++) {
        if (currentLevel[i][j] != -1) {
          BubbleSprite newOne = new BubbleSprite(
            new Rect(190+i*32-(j%2)*16, 44+j*28, 32, 32),
            currentLevel[i][j],
            bubbles[currentLevel[i][j]], bubblesBlind[currentLevel[i][j]],
            frozenBubbles[currentLevel[i][j]], bubbleBlink, bubbleManager,
            soundManager, this);
          bubblePlay[i][j] = newOne;
          this.addSprite(newOne);
        }
      }
    }

    /*
     * Initialize the launch bubbles.
     */
    currentColor = bubbleManager.nextBubbleIndex(random);
    nextColor    = bubbleManager.nextBubbleIndex(random);

    if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL) {
      nextBubble = new ImageSprite(new Rect(302, 440, 302 + 32, 440 + 32),
                                   bubbles[nextColor]);
    }
    else {
      nextBubble = new ImageSprite(new Rect(302, 440, 302 + 32, 440 + 32),
                                   bubblesBlind[nextColor]);
    }

    this.addSprite(nextBubble);
    launchBubble = new LaunchBubbleSprite(currentColor, 
                                          launchBubblePosition,
                                          launcher, bubbles, bubblesBlind);
    this.spriteToBack(launchBubble);

    /*
     * Initialize game metrics.
     */
    nbBubbles      = 0;
    sendToOpponent = 0;
  }

  public FrozenGame(BmpWrap background_arg,
                    BmpWrap[] bubbles_arg,
                    BmpWrap[] bubblesBlind_arg,
                    BmpWrap[] frozenBubbles_arg,
                    BmpWrap[] targetedBubbles_arg,
                    BmpWrap bubbleBlink_arg,
                    BmpWrap gameWon_arg,
                    BmpWrap gameLost_arg,
                    BmpWrap gamePaused_arg,
                    BmpWrap hurry_arg,
                    BmpWrap penguins_arg,
                    BmpWrap compressorHead_arg,
                    BmpWrap compressor_arg,
                    Drawable launcher_arg,
                    SoundManager soundManager_arg,
                    LevelManager levelManager_arg,
                    HighscoreManager highscoreManager_arg) {
    this(background_arg, bubbles_arg, bubblesBlind_arg, frozenBubbles_arg,
         targetedBubbles_arg, bubbleBlink_arg, gameWon_arg, gameLost_arg,
         gamePaused_arg, hurry_arg, null, null, penguins_arg, compressorHead_arg,
         compressor_arg, null, launcher_arg, soundManager_arg,
         levelManager_arg, highscoreManager_arg, null, null);
  }

  public void addAttackBubbles(int attackBubbles) {
    sendToOpponent += attackBubbles;
  }

  public void addFallingBubble(BubbleSprite sprite) {
    if (malusBar != null)
      malusBar.releaseTime = 0;
    sendToOpponent++;
    spriteToFront(sprite);
    falling.addElement(sprite);
  }

  public void addJumpingBubble(BubbleSprite sprite) {
    spriteToFront(sprite);
    jumping.addElement(sprite);
  }

  private void blinkLine(int number) {
    int move = number%2;
    int column = (number+1) >> 1;

    for (int i = move; i < 13; i++) {
      if (bubblePlay[column][i] != null) {
        bubblePlay[column][i].blink();
      }
    }
  }

  public void calculateGridChecksum() {
    CRC16 gridCRC = new CRC16(0);

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 12; j++) {
        if (bubblePlay[i][j] != null) {
          gridCRC.update(bubblePlay[i][j].getColor());
        }
      }
    }

    gridChecksum = (short) gridCRC.getValue();
  }

  private boolean checkLost() {
    boolean lost = false;

    if (!endOfGame) {
      if (movingBubble != null) {
        if (movingBubble.fixed() && !movingBubble.released() &&
            (movingBubble.getSpritePosition().y >= 380)) {
          lost = true;
        }
      }

      int steps = compressor.getSteps();

      for (int i = 0; i < 8; i++) {
        if (bubblePlay[i][12 - steps] != null) {
          lost = true;
          break;
        }
      }

      if (lost) {
        penguin.updateState(PenguinSprite.STATE_GAME_LOST);
        if (highscoreManager != null)
          highscoreManager.lostLevel();
        playResult = gameEnum.LOST;
        endOfGame = true;
        initFrozenify();
        soundManager.playSound(FrozenBubble.SOUND_LOST);
      }
    }

    return playResult == gameEnum.LOST;
  }

  public void clampLaunchPosition() {
    if (launchBubblePosition < MIN_LAUNCH_DIRECTION) {
      launchBubblePosition = MIN_LAUNCH_DIRECTION;
    }
    if (launchBubblePosition > MAX_LAUNCH_DIRECTION) {
      launchBubblePosition = MAX_LAUNCH_DIRECTION;
    }
  }

  public void deleteFallingBubble(BubbleSprite sprite) {
    removeSprite(sprite);
    falling.removeElement(sprite);
  }

  /**
   * Remove the designated goingUp bubble sprite from the vector of
   * attack bubbles because it is now inserted into the game grid.  The
   * sprite is not removed from the vector of all sprites in the game
   * because it has been added to the play field.
   * @param sprite - the attack bubble inserted into the game grid.
   */
  public void deleteGoingUpBubble(BubbleSprite sprite) {
    goingUp.removeElement(sprite);
  }

  public void deleteJumpingBubble(BubbleSprite sprite) {
    removeSprite(sprite);
    jumping.removeElement(sprite);
  }

  private void frozenify() {
    frozenifyX--;
    if (frozenifyX < 0) {
      frozenifyX = 7;
      frozenifyY--;

      if (frozenifyY < 0) {
        frozenify = false;
        this.addSprite(new ImageSprite(new Rect(152, 190, 337, 116),
                                       gameLost));
        soundManager.playSound(FrozenBubble.SOUND_NOH);
        return;
      }
    }

    while ((bubblePlay[frozenifyX][frozenifyY] == null) && (frozenifyY >= 0)) {
      frozenifyX--;
      if (frozenifyX < 0) {
        frozenifyX = 7;
        frozenifyY--;

        if (frozenifyY < 0) {
          frozenify = false;
          this.addSprite(new ImageSprite(new Rect(152, 190, 337, 116),
                                         gameLost));
          soundManager.playSound(FrozenBubble.SOUND_NOH);
          return;
        }
      }
    }

    this.spriteToBack(bubblePlay[frozenifyX][frozenifyY]);
    bubblePlay[frozenifyX][frozenifyY].frozenify();

    this.spriteToBack(launchBubble);
  }

  public int getAttackBarBubbles() {
    return malusBar.getAttackBarBubbles();
  }

  public int getCompressorSteps() {
    return compressor.getSteps();
  }

  public int getCurrentColor() {
    return currentColor;
  }

  public gameEnum getGameResult() {
    return playResult;
  }

  public BubbleSprite[][] getGrid() {
    return bubblePlay;
  }

  public double getMoveDown() {
    return compressor.getMoveDown();
  }

  public int getNewNextColor() {
    return newNextColor;
  }

  public int getNextColor() {
    return nextColor;
  }

  public boolean getOkToFire() {
    return (movingBubble == null) && (playResult == gameEnum.PLAYING) &&
           ((goingUp.size() == 0) || (networkManager == null)) && readyToFire;
  }

  private Rect getPenguinRect(int player) {
    if (player == 1)
      return new Rect(361, 436, 361 + PenguinSprite.PENGUIN_WIDTH - 2,
                      436 + PenguinSprite.PENGUIN_HEIGHT - 2);
    else
      return new Rect(221, 436, 221 + PenguinSprite.PENGUIN_WIDTH - 2,
                      436 + PenguinSprite.PENGUIN_HEIGHT - 2);
  }

  public double getPosition() {
    return launchBubblePosition;
  }

  public Random getRandom() {
    return random;
  }

  /**
   * Obtain this player's <code>sendToOpponent</code> value, which is
   * the number of attack bubbles to add to the opponent's attack bar.
   * @return The number of attack bubbles to add to the opponent's
   * attack bar.
   */
  public int getSendToOpponent() {
    return sendToOpponent;
  }

  private void initFrozenify() {
    ImageSprite freezeLaunchBubble =
      new ImageSprite(new Rect(301, 389, 34, 42), frozenBubbles[currentColor]);
    ImageSprite freezeNextBubble =
      new ImageSprite(new Rect(301, 439, 34, 42), frozenBubbles[nextColor]);

    this.addSprite(freezeLaunchBubble);
    this.addSprite(freezeNextBubble);

    frozenifyX = 7;
    frozenifyY = 12;
    frozenify  = true;
  }

  /**
   * Lower the bubbles in play and drop the compressor the required
   * number of steps.
   * @param playSound - <code>true</code> to play the compression sound.
   */
  private void lowerCompressorSteps(boolean playSound) {
    for (int index = 0; index < newSteps; index++) {
      lowerCompressor(playSound);
    }
    newSteps = -1;
  }

  /**
   * Lower the bubbles in play and drop the compressor a step.
   * @param playSound - <code>true</code> to play the compression sound.
   */
  public void lowerCompressor(boolean playSound) {
    fixedBubbles = 0;

    if (playSound) {
      soundManager.playSound(FrozenBubble.SOUND_NEWROOT);
    }

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 12; j++) {
        if (bubblePlay[i][j] != null) {
          bubblePlay[i][j].moveDown();

          if ((bubblePlay[i][j].getSpritePosition().y >= 380) && !endOfGame) {
            penguin.updateState(PenguinSprite.STATE_GAME_LOST);
            if (highscoreManager != null)
              highscoreManager.lostLevel();
            playResult = gameEnum.LOST;
            endOfGame = true;
            initFrozenify();
            if (playSound) {
              soundManager.playSound(FrozenBubble.SOUND_LOST);
            }
          }
        }
      }
    }

    compressor.moveDown();
  }

  /**
   * Move the launched bubble.
   * @return <code>true</code> if the compressor was lowered.
   */
  public boolean manageMovingBubble() {
    boolean compressed = false;

    if (movingBubble != null) {
      movingBubble.move();
      if (movingBubble.fixed()) {
        if (!checkLost()) {
          /*
           * If there are no bubbles in the bubble manager, then the
           * player has won the game.  The bubble manager counts bubbles
           * that are fixed in position on the bubble grid.  Thus if
           * there are attack bubbles in motion when the bubble manager
           * is cleared, then the attack bubbles will be added to the
           * bubble manager when they stick to the bubble grid after the
           * player has already won the game.  This may need to change.
           */
          if (bubbleManager.countBubbles() == 0) {
            penguin.updateState(PenguinSprite.STATE_GAME_WON);
            this.addSprite(new ImageSprite(new Rect(152, 190,
                                                    152 + 337,
                                                    190 + 116), gameWon));
            if (highscoreManager != null)
              highscoreManager.endLevel(nbBubbles);
            playResult = gameEnum.WON;
            endOfGame = true;
            soundManager.playSound(FrozenBubble.SOUND_WON);
          }
          else if ((malusBar == null) || FrozenBubble.getCompressor()) {
            fixedBubbles++;
            blinkDelay = 0;

            if ((fixedBubbles == 8) && !isRemote) {
              lowerCompressor(true);
              compressed = true;
            }
          }
        }
        movingBubble = null;
      }
    }
    return compressed;
  }

  public void paint(Canvas c, double scale, int dx, int dy) {
    compressor.paint(c, scale, dx, dy);
    if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL) {
      nextBubble.changeImage(bubbles[nextColor]);
    }
    else {
      nextBubble.changeImage(bubblesBlind[nextColor]);
    }
    super.paint(c, scale, dx, dy);
  }

  public void pause() {
    this.removeSprite(pausedSprite);
    this.addSprite(pausedSprite);
  }

  public void pauseButtonPressed(boolean paused) {
    if (paused) {
      if (pauseButtonSprite != null) {
        this.removeSprite(pauseButtonSprite);
      }
      if (playButtonSprite != null) {
        this.removeSprite(playButtonSprite);
        this.addSprite(playButtonSprite);
      }
    }
    else {
      if (playButtonSprite != null) {
        this.removeSprite(playButtonSprite);
      }
      if (pauseButtonSprite != null) {
        this.removeSprite(pauseButtonSprite);
        this.addSprite(pauseButtonSprite);
      }
    }
  }

  public gameEnum play(boolean key_left, boolean key_right,
                       boolean key_fire, boolean key_swap,
                       double trackball_dx,
                       boolean touch_fire, double touch_x, double touch_y,
                       boolean ats_touch_fire, double ats_touch_dx) {
    boolean ats = FrozenBubble.getAimThenShoot();
    boolean bubbleLaunched = false;
    boolean compressed = false;
    int[]   move = new int[2];
    int     attackBarBubbles = 0;
    int     currentColorWas = currentColor;
    int     nextColorWas = nextColor;
    int     numAttackBubbles = 0;

    if (malusBar != null) {
      sendToOpponent = 0;
      attackBarBubbles = malusBar.getAttackBarBubbles();
    }

    if ((ats && ats_touch_fire) || (!ats && touch_fire)) {
      key_fire = true;
    }

    if (key_left && !key_right) {
      move[HORIZONTAL_MOVE] = KEY_LEFT;
    }
    else if (key_right && !key_left) {
      move[HORIZONTAL_MOVE] = KEY_RIGHT;
    }
    else {
      move[HORIZONTAL_MOVE] = 0;
    }

    if (key_fire) {
      move[FIRE] = KEY_UP;
    }
    else {
      move[FIRE] = 0;
    }

    if (key_swap) {
      if (!swapPressed) {
        swapNextLaunchBubble();
        swapPressed = true;
      }
    }
    else {
      swapPressed = false;
    }

    if (!ats && touch_fire && !isRemote && (movingBubble == null)) {
      double xx = touch_x - 318;
      double yy = 406 - touch_y;
      launchBubblePosition = (Math.PI - Math.atan2(yy, xx)) * 40.0 / Math.PI;
      clampLaunchPosition();
    }

    if ((move[FIRE] == 0) || touch_fire) {
      readyToFire = true;
    }

    /*
     * If the option to rush the player is disabled or this game
     * represents the remote player in a network game, initialize
     * hurryTime to disable automatic bubbles launches.
     */
    if (FrozenBubble.getDontRushMe() || isRemote) {
      hurryTime = 1;
    }

    if (endOfGame && readyToFire) {
      if (move[FIRE] == KEY_UP) {
        if (playResult == gameEnum.WON) {
          levelManager.goToNextLevel();
          playResult = gameEnum.NEXT_WON;
        }
        else {
          playResult = gameEnum.NEXT_LOST;
        }
        return playResult;
      }
      else {
        penguin.updateState(PenguinSprite.STATE_VOID);

        /*
         * If the game is over because of bubble overflow, wait until
         * all the bubbles have stopped moving to freeze them.
         */
        if (frozenify && (goingUp.size() == 0) && (movingBubble == null)) {
          frozenify();
        }
      }
    }
    else {
      if ((move[FIRE] == KEY_UP) || (hurryTime > HURRY_ME_TIME)) {
        if (getOkToFire()) {
          nbBubbles++;
          movingBubble = new BubbleSprite(new Rect(302, 390, 32, 32),
                                          launchBubblePosition,
                                          currentColor,
                                          bubbles[currentColor],
                                          bubblesBlind[currentColor],
                                          frozenBubbles[currentColor],
                                          targetedBubbles, bubbleBlink,
                                          bubbleManager, soundManager, this);
          this.addSprite(movingBubble);
          bubbleLaunched = true;
          currentColor = nextColor;

          if (isRemote) {
            nextColor = newNextColor;
          }
          else {
            nextColor = bubbleManager.nextBubbleIndex(random);
          }

          if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL) {
            nextBubble.changeImage(bubbles[nextColor]);
          }
          else {
            nextBubble.changeImage(bubblesBlind[nextColor]);
          }

          launchBubble.changeColor(currentColor);
          penguin.updateState(PenguinSprite.STATE_FIRE);
          soundManager.playSound(FrozenBubble.SOUND_LAUNCH);
          readyToFire = false;
          hurryTime = 0;

          if (malusBar != null) {
            malusBar.releaseTime = RELEASE_TIME;
          }

          removeSprite(hurrySprite);
        }
        else {
          penguin.updateState(PenguinSprite.STATE_VOID);
        }
      }
      else {
        double dx = 0;
        if (move[HORIZONTAL_MOVE] == KEY_LEFT) {
          dx -= 1;
        }
        if (move[HORIZONTAL_MOVE] == KEY_RIGHT) {
          dx += 1;
        }
        dx += trackball_dx;
        if (ats) {
          dx += ats_touch_dx;
        }
        launchBubblePosition += dx;
        clampLaunchPosition();
        launchBubble.changeDirection(launchBubblePosition);
        updatePenguinState(dx);
      }
    }

    /*
     * The moving bubble is moved twice, which produces smoother
     * animation. Thus the moving bubble effectively moves at twice the
     * animation speed with respect to other bubbles that are only
     * moved once per iteration.
     */
    compressed  = manageMovingBubble();
    compressed |= manageMovingBubble();

    if ((movingBubble == null) && !endOfGame) {
      hurryTime++;
      if (malusBar != null)
        malusBar.releaseTime++;
      /*
       * If hurryTime == 2 (1 + 1) we could be in the "Don't rush me"
       * mode.  Remove the sprite just in case the user switched
       * to this mode when the "Hurry" sprite was shown, to make it
       * disappear.
       */
      if (hurryTime == 2) {
        removeSprite(hurrySprite);
      }
      if (hurryTime >= 240) {
        if (hurryTime%40 == 10) {
          addSprite(hurrySprite);
          soundManager.playSound(FrozenBubble.SOUND_HURRY);
        }
        else if (hurryTime%40 == 35) {
          removeSprite(hurrySprite);
        }
      }
      if (malusBar != null) {
        if (getOkToFire() && (attackBarBubbles > 0) &&
            ((malusBar.releaseTime > RELEASE_TIME) || isRemote)) {
          numAttackBubbles = releaseBubbles();
          malusBar.releaseTime = 0;
        }
      }
    }

    if ((malusBar == null) || FrozenBubble.getCompressor()) {
      if (fixedBubbles == 6) {
        if (blinkDelay < 15) {
          blinkLine(blinkDelay);
        }
        blinkDelay++;
        if (blinkDelay == 40) {
          blinkDelay = 0;
        }
      }
      else if (fixedBubbles == 7) {
        if (blinkDelay < 15) {
          blinkLine(blinkDelay);
        }
        blinkDelay++;
        if (blinkDelay == 25) {
          blinkDelay = 0;
        }
      }
    }

    for (int i = 0; i < falling.size(); i++) {
      ((BubbleSprite)falling.elementAt(i)).fall();
    }

    for (int i = 0; i < goingUp.size(); i++) {
      ((BubbleSprite)goingUp.elementAt(i)).goUp();
    }

    for (int i = 0; i < jumping.size(); i++) {
      ((BubbleSprite)jumping.elementAt(i)).jump();
    }

    /*
     * In a multiplayer game, check if the player lost due to attack
     * bubbles overflowing the play area.
     */
    if (malusBar != null) {
      checkLost();
    }

    /*
     * Perform game synchronization tasks.
     */
    synchronizeNetworkGame();
    synchronizeBubbleManager();

    /*
     * If this player is the local player and is participating in a
     * network game, transmit the local player action to the remote
     * player if an action occurred.
     */
    if ((networkManager != null) && (malusBar != null)) {
      if (bubbleLaunched || compressed || swapPressed ||
          (numAttackBubbles > 0)) {
        if (bubbleLaunched || (numAttackBubbles > 0)) {
          gridChecksum = 0;
        }
        if (!isRemote) {
          networkManager.sendLocalPlayerAction(player,
                                               compressed,
                                               bubbleLaunched,
                                               swapPressed,
                                               0,
                                               currentColorWas,
                                               nextColorWas,
                                               nextColor,
                                               attackBarBubbles,
                                               malusBar.attackBubbles,
                                               launchBubblePosition);
        }
      }
      else if ((gridChecksum == 0) && getOkToFire()) {
        calculateGridChecksum();
      }
    }

    if (malusBar != null) {
      malusBar.clearAttackBubbles();
    }

    return gameEnum.PLAYING;
  }

  /**
   * Populate random columns in a row of attack bubbles to launch onto
   * the game field.
   * <p>In an actual play field, the rows alternate between a maximum 7
   * and 8 bubbles per row.  Thus 7 bubbles are sent up as that is the
   * maximum number of bubbles that can fit in each alternating row.
   * <p>There are 15 distinct positions ("lanes") for bubbles to occupy
   * between two consecutive rows.  Thus we send up a maximum 7 bubbles
   * in randomly selected "lanes" from the 15 available.
   * @return The number of attack bubbles launched.
   */
  private int releaseBubbles() {
    if (malusBar == null) {
      return 0;
    }

    int numBubblesLaunched = 0;

    /*
     * If this game represents a remote player, the the attack bubbles
     * are calculated on the remote machine and sent over the network.
     * Simply use the supplied attack bubble buffer to initiate attack
     * bubble launches. 
     */
    if (isRemote) {
      for (int i = 0; i < 15; i++) {
        if (malusBar.attackBubbles[i] >= 0) {
          numBubblesLaunched++;
          int color = malusBar.attackBubbles[i];
          BubbleSprite malusBubble = new BubbleSprite(
            new Rect(columnX[i], 44+15*28, 32, 32),
            START_LAUNCH_DIRECTION,
            color, bubbles[color], bubblesBlind[color],
            frozenBubbles[color], targetedBubbles, bubbleBlink,
            bubbleManager, soundManager, this);
          goingUp.add(malusBubble);
          this.addSprite(malusBubble);
        }
      }
      malusBar.removeAttackBubbles(numBubblesLaunched);
    }
    else if (malusBar.getAttackBarBubbles() > 0) {
      boolean[] lanes = new boolean[15];
      int malusBalls = malusBar.removeLine();
      int pos;

      while (malusBalls > 0) {
        pos = random.nextInt(15);
        if (!lanes[pos]) {
          lanes[pos] = true;
          malusBalls--;
        }
      }

      for (int i = 0; i < 15; i++) {
        if (lanes[i]) {
          numBubblesLaunched++;
          int color = random.nextInt(FrozenBubble.getDifficulty());
          malusBar.setAttackBubble(i, color);
          BubbleSprite malusBubble = new BubbleSprite(
            new Rect(columnX[i], 44+15*28, 32, 32),
            START_LAUNCH_DIRECTION,
            color, bubbles[color], bubblesBlind[color],
            frozenBubbles[color], targetedBubbles, bubbleBlink,
            bubbleManager, soundManager, this);
          goingUp.add(malusBubble);
          this.addSprite(malusBubble);
        }
      }
    }

    return numBubblesLaunched;
  }

  private Sprite restoreSprite(Bundle map, Vector<BmpWrap> imageList, int i) {
    int left = map.getInt(String.format("%d-%d-left", player, i));
    int right = map.getInt(String.format("%d-%d-right", player, i));
    int top = map.getInt(String.format("%d-%d-top", player, i));
    int bottom = map.getInt(String.format("%d-%d-bottom", player, i));
    int type = map.getInt(String.format("%d-%d-type", player, i));
    if (type == Sprite.TYPE_BUBBLE) {
      int color = map.getInt(String.format("%d-%d-color", player, i));
      double moveX = map.getDouble(String.format("%d-%d-moveX", player, i));
      double moveY = map.getDouble(String.format("%d-%d-moveY", player, i));
      double realX = map.getDouble(String.format("%d-%d-realX", player, i));
      double realY = map.getDouble(String.format("%d-%d-realY", player, i));
      boolean fixed = map.getBoolean(String.format("%d-%d-fixed", player, i));
      boolean blink = map.getBoolean(String.format("%d-%d-blink", player, i));
      boolean released =
          map.getBoolean(String.format("%d-%d-released", player, i));
      boolean checkJump =
          map.getBoolean(String.format("%d-%d-checkJump", player, i));
      boolean checkFall =
          map.getBoolean(String.format("%d-%d-checkFall", player, i));
      int fixedAnim = map.getInt(String.format("%d-%d-fixedAnim", player, i));
      boolean frozen =
          map.getBoolean(String.format("%d-%d-frozen", player, i));
      Point lastOpenPosition = new Point(
          map.getInt(String.format("%d-%d-lastOpenPosition.x", player, i)),
          map.getInt(String.format("%d-%d-lastOpenPosition.y", player, i)));
      return new BubbleSprite(new Rect(left, top, right, bottom),
                              color, moveX, moveY, realX, realY,
                              fixed, blink, released, checkJump, checkFall,
                              fixedAnim,
                              (frozen ? frozenBubbles[color] : bubbles[color]),
                              lastOpenPosition,
                              bubblesBlind[color],
                              frozenBubbles[color],
                              targetedBubbles, bubbleBlink,
                              bubbleManager, soundManager, this);
    }
    else if (type == Sprite.TYPE_IMAGE) {
      int imageId = map.getInt(String.format("%d-%d-imageId", player, i));
      return new ImageSprite(new Rect(left, top, right, bottom),
                             (BmpWrap)imageList.elementAt(imageId));
    }
    else if (type == Sprite.TYPE_LAUNCH_BUBBLE) {
      int currentColor =
          map.getInt(String.format("%d-%d-currentColor", player, i));
      double currentDirection =
        map.getDouble(String.format("%d-%d-currentDirection", player, i));
      return new LaunchBubbleSprite(currentColor, currentDirection,
                                    launcher, bubbles, bubblesBlind);
    }
    else if (type == Sprite.TYPE_PENGUIN) {
      int currentPenguin =
          map.getInt(String.format("%d-%d-currentPenguin", player, i));
      int count = map.getInt(String.format("%d-%d-count", player, i));
      int finalState =
          map.getInt(String.format("%d-%d-finalState", player, i));
      int nextPosition =
          map.getInt(String.format("%d-%d-nextPosition", player, i));

      return new PenguinSprite(getPenguinRect(player), penguins, random,
                               currentPenguin, count, finalState,
                               nextPosition);
    }
    else {
      Log.e("frozen-bubble", "Unrecognized sprite type: " + type);
      return null;
    }
  }

  public void restoreState(Bundle map, Vector<BmpWrap> imageList) {
    Vector<Sprite> savedSprites = new Vector<Sprite>();
    int numSavedSprites =
        map.getInt(String.format("%d-numSavedSprites", player));
    for (int i = 0; i < numSavedSprites; i++) {
      savedSprites.addElement(restoreSprite(map, imageList, i));
    }

    restoreSprites(map, savedSprites, player);

    jumping = new Vector<Sprite>();
    int numJumpingSprites =
        map.getInt(String.format("%d-numJumpingSprites", player));
    for (int i = 0; i < numJumpingSprites; i++) {
      int spriteIdx = map.getInt(String.format("%d-jumping-%d", player, i));
      jumping.addElement(savedSprites.elementAt(spriteIdx));
    }
    goingUp = new Vector<Sprite>();
    int numGoingUpSprites =
        map.getInt(String.format("%d-numGoingUpSprites", player));
    for (int i = 0; i < numGoingUpSprites; i++) {
      int spriteIdx = map.getInt(String.format("%d-goingUp-%d", player, i));
      goingUp.addElement(savedSprites.elementAt(spriteIdx));
    }
    falling = new Vector<Sprite>();
    int numFallingSprites =
        map.getInt(String.format("%d-numFallingSprites", player));
    for (int i = 0; i < numFallingSprites; i++) {
      int spriteIdx = map.getInt(String.format("%d-falling-%d", player, i));
      falling.addElement(savedSprites.elementAt(spriteIdx));
    }
    bubblePlay = new BubbleSprite[8][13];
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 13; j++) {
        int spriteIdx =
            map.getInt(String.format("%d-play-%d-%d", player, i, j));
        if (spriteIdx != -1) {
          bubblePlay[i][j] = (BubbleSprite)savedSprites.elementAt(spriteIdx);
        }
        else {
          bubblePlay[i][j] = null;
        }
      }
    }
    int launchBubbleId =
        map.getInt(String.format("%d-launchBubbleId", player));
    launchBubble = (LaunchBubbleSprite)savedSprites.elementAt(launchBubbleId);
    launchBubblePosition =
        map.getDouble(String.format("%d-launchBubblePosition", player));
    if (malusBar != null) {
      malusBar.restoreState(map, player);
      this.addSprite(malusBar);
    }
    int penguinId = map.getInt(String.format("%d-penguinId", player));
    penguin = (PenguinSprite)savedSprites.elementAt(penguinId);
    compressor.restoreState(map, player);
    int nextBubbleId = map.getInt(String.format("%d-nextBubbleId", player));
    nextBubble = (ImageSprite)savedSprites.elementAt(nextBubbleId);
    currentColor = map.getInt(String.format("%d-currentColor", player));
    nextColor = map.getInt(String.format("%d-nextColor", player));
    int movingBubbleId =
        map.getInt(String.format("%d-movingBubbleId", player));
    if (movingBubbleId == -1) {
      movingBubble = null;
    }
    else {
      movingBubble = (BubbleSprite)savedSprites.elementAt(movingBubbleId);
    }
    bubbleManager.restoreState(map, player);
    fixedBubbles = map.getInt(String.format("%d-fixedBubbles", player));
    nbBubbles = map.getInt(String.format("%d-nbBubbles", player));
    sendToOpponent = map.getInt(String.format("%d-sendToOpponent", player));
    blinkDelay = map.getInt(String.format("%d-blinkDelay", player));
    int hurryId = map.getInt(String.format("%d-hurryId", player));
    hurrySprite = (ImageSprite)savedSprites.elementAt(hurryId);
    hurryTime = map.getInt(String.format("%d-hurryTime", player));
    int pausedId = map.getInt(String.format("%d-pausedId", player));
    pausedSprite = (ImageSprite)savedSprites.elementAt(pausedId);
    readyToFire = map.getBoolean(String.format("%d-readyToFire", player));
    endOfGame = map.getBoolean(String.format("%d-endOfGame", player));
    frozenify = map.getBoolean(String.format("%d-frozenify", player));
    frozenifyX = map.getInt(String.format("%d-frozenifyX", player));
    frozenifyY = map.getInt(String.format("%d-frozenifyY", player));
  }

  public void resume() {
    this.removeSprite(pausedSprite);
  }

  public void saveState(Bundle map) {
    Vector<Sprite> savedSprites = new Vector<Sprite>();

    saveSprites(map, savedSprites, player);

    for (int i = 0; i < jumping.size(); i++) {
      ((Sprite)jumping.elementAt(i)).saveState(map, savedSprites, player);
      map.putInt(String.format("%d-jumping-%d", player, i),
                 ((Sprite)jumping.elementAt(i)).getSavedId());
    }
    map.putInt(String.format("%d-numJumpingSprites", player), jumping.size());
    for (int i = 0; i < goingUp.size(); i++) {
      ((Sprite)goingUp.elementAt(i)).saveState(map, savedSprites, player);
      map.putInt(String.format("%d-goingUp-%d", player, i),
                 ((Sprite)goingUp.elementAt(i)).getSavedId());
    }
    map.putInt(String.format("%d-numGoingUpSprites", player), goingUp.size());
    for (int i = 0; i < falling.size(); i++) {
      ((Sprite)falling.elementAt(i)).saveState(map, savedSprites, player);
      map.putInt(String.format("%d-falling-%d", player, i),
                 ((Sprite)falling.elementAt(i)).getSavedId());
    }
    map.putInt(String.format("%d-numFallingSprites", player), falling.size());
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 13; j++) {
        if (bubblePlay[i][j] != null) {
          bubblePlay[i][j].saveState(map, savedSprites, player);
          map.putInt(String.format("%d-play-%d-%d", player, i, j),
                     bubblePlay[i][j].getSavedId());
        }
        else {
          map.putInt(String.format("%d-play-%d-%d", player, i, j), -1);
        }
      }
    }
    launchBubble.saveState(map, savedSprites, player);
    map.putInt(String.format("%d-launchBubbleId", player),
               launchBubble.getSavedId());
    map.putDouble(String.format("%d-launchBubblePosition", player),
                  launchBubblePosition);
    if (malusBar != null) {
      malusBar.saveState(map, player);
    }
    penguin.saveState(map, savedSprites, player);
    compressor.saveState(map, player);
    map.putInt(String.format("%d-penguinId", player), penguin.getSavedId());
    nextBubble.saveState(map, savedSprites, player);
    map.putInt(String.format("%d-nextBubbleId", player),
               nextBubble.getSavedId());
    map.putInt(String.format("%d-currentColor", player), currentColor);
    map.putInt(String.format("%d-nextColor", player), nextColor);
    if (movingBubble != null) {
      movingBubble.saveState(map, savedSprites, player);
      map.putInt(String.format("%d-movingBubbleId", player),
                 movingBubble.getSavedId());
    }
    else {
      map.putInt(String.format("%d-movingBubbleId", player), -1);
    }
    bubbleManager.saveState(map, player);
    map.putInt(String.format("%d-fixedBubbles", player), fixedBubbles);
    map.putInt(String.format("%d-nbBubbles", player), nbBubbles);
    map.putInt(String.format("%d-sendToOpponent", player), sendToOpponent);
    map.putInt(String.format("%d-blinkDelay", player), blinkDelay);
    hurrySprite.saveState(map, savedSprites, player);
    map.putInt(String.format("%d-hurryId", player), hurrySprite.getSavedId());
    map.putInt(String.format("%d-hurryTime", player), hurryTime);
    pausedSprite.saveState(map, savedSprites, player);
    map.putInt(String.format("%d-pausedId", player), pausedSprite.getSavedId());
    map.putBoolean(String.format("%d-readyToFire", player), readyToFire);
    map.putBoolean(String.format("%d-endOfGame", player), endOfGame);
    map.putBoolean(String.format("%d-frozenify", player), frozenify);
    map.putInt(String.format("%d-frozenifyX", player), frozenifyX);
    map.putInt(String.format("%d-frozenifyY", player), frozenifyY);
    map.putInt(String.format("%d-numSavedSprites", player),
               savedSprites.size());
    for (int i = 0; i < savedSprites.size(); i++) {
      ((Sprite)savedSprites.elementAt(i)).clearSavedId();
    }
  }

  /**
   * Lower the compressor to the specified number of steps.
   * @param newSteps - the number of compressor steps to lower to.
   */
  public void setCompressorSteps(byte newSteps) {
    this.newSteps = newSteps;
  }

  /**
   * Set the game result associated with this player.
   * @param result - GAME_WON if this player won the game, GAME_LOST if
   * this player lost the game.
   */
  public void setGameResult(gameEnum result) {
    if (!endOfGame) {
      playResult = result;
      if (result == gameEnum.WON)
      {
        penguin.updateState(PenguinSprite.STATE_GAME_WON);
        this.addSprite(new ImageSprite(new Rect(152, 190,
                                                152 + 337,
                                                190 + 116), gameWon));
      }
      else if (result == gameEnum.LOST)
      {
        penguin.updateState(PenguinSprite.STATE_GAME_LOST);
        this.addSprite(new ImageSprite(new Rect(152, 190,
                                                152 + 337,
                                                190 + 116), gameLost));
      }
      endOfGame = true;
    }
  }

  private void setGrid() {
    if (newGrid != null) {
      compressor.init();
      falling.clear();
      goingUp.clear();
      jumping.clear();
      bubbleManager.initialize();
      removeAllBubbleSprites();
      for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 13; j++) {
          bubblePlay[i][j] = null;
          if (newGrid[i][j] != -1) {
            bubblePlay[i][j] = new BubbleSprite(
                new Rect(190+i*32-(j%2)*16, 44+j*28, 32, 32),
                newGrid[i][j],
                bubbles[newGrid[i][j]], bubblesBlind[newGrid[i][j]],
                frozenBubbles[newGrid[i][j]], bubbleBlink, bubbleManager,
                soundManager, this);
            this.addSprite(bubblePlay[i][j]);
          }
        }
      }
      newGrid = null;
    }
  }

  public void setGrid(byte[][] newGrid) {
    this.newGrid = new byte[8][13];
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 13; j++) {
        this.newGrid[i][j] = newGrid[i][j];
      }
    }
    synchronizeNetworkGame();
  }

  public void setLaunchBubbleColors(int current, int next, int newNext) {
    currentColor = current;
    nextColor    = next;
    newNextColor = newNext;
    launchBubble.changeColor(currentColor);

    if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL)
      nextBubble.changeImage(bubbles[nextColor]);
    else
      nextBubble.changeImage(bubblesBlind[nextColor]);
  }

  public void setPosition(double value) {
    if (!endOfGame) {
      double dx = value - launchBubblePosition;
      /*
       * For small position changes, don't update the penguin state.
       */
      if ((dx < 0.25) && (dx > -0.25))
        dx = 0;
      launchBubblePosition = value;
      clampLaunchPosition();
      launchBubble.changeDirection(launchBubblePosition);
      updatePenguinState(dx);
    }
  }

  public void swapNextLaunchBubble() {
    if (currentColor != nextColor) {
      int tempColor = currentColor;
      currentColor  = nextColor;
      nextColor     = tempColor;
      launchBubble.changeColor(currentColor);

      if (FrozenBubble.getMode() == FrozenBubble.GAME_NORMAL)
        nextBubble.changeImage(bubbles[nextColor]);
      else
        nextBubble.changeImage(bubblesBlind[nextColor]);

      soundManager.playSound(FrozenBubble.SOUND_WHIP);
    }
  }

  /**
   * This function is an unfortunate patch that is necessitated due to
   * the fact that there is as of yet an unfixed bug in the BubbleSprite
   * management code.
   * <p>Somewhere amongst goUp() and move() in BubbleSprite.java, a flaw
   * exists whereby a bubble is added to the bubble manager, and the
   * bubble sprite is added to the game screen, but the entry in the
   * bubblePlay grid was either rendered null or a bubble superposition
   * in the grid occurred.  The former is suspected, because ensuring
   * the grid location is null before assigning a bubble sprite to it is
   * very rigorously enforced.
   * <p><b>TODO</b> - fix the grid entry bug.
   */
  public void synchronizeBubbleManager() {
    int numBubblesManager = bubbleManager.countBubbles();
    int numBubblesPlay = 0;
    /*
     * Check the bubble sprite grid for occupied locations.
     */
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 13; j++) {
        if (bubblePlay[i][j] != null ) {
          numBubblesPlay++;
        }
      }
    }
    /*
     * If the number of bubble sprite grid entries does not match the
     * number of bubbles in the bubble manager, then we need to re-
     * initialize the bubble manager, and re-initialize all the bubble
     * sprites on the game screen.  You would be unable to win prior to
     * the addition of this synchronization code due to the number of
     * bubbles in the bubble manager never reaching zero, and the excess
     * sprite or sprites would remain stuck on the screen.
     */
    if (numBubblesManager != numBubblesPlay) {
      bubbleManager.initialize();
      removeAllBubbleSprites();
      for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 13; j++) {
          if (bubblePlay[i][j] != null ) {
            bubblePlay[i][j].addToManager();
            this.addSprite(bubblePlay[i][j]);
          }
        }
      }
      for (int i = 0; i < falling.size(); i++) {
        this.addSprite(falling.elementAt(i));
      }
      for (int i = 0; i < goingUp.size(); i++) {
        this.addSprite(goingUp.elementAt(i));
      }
      for (int i = 0; i < jumping.size(); i++) {
        this.addSprite(jumping.elementAt(i));
      }
    }
  }

  /**
   * If this is a network game, process bubble grid and compressor
   * synchronization tasks.
   * <p>To prevent the appearance of glitches, do not synchronize the
   * game field while a launched bubble is in motion.  Since this method
   * is called in every loop of the <code>play()</code> function, all
   * synchronization tasks will be automatically serviced eventually.
   * <p>Note that during game initialization, there is never a launch
   * bubble in motion, so a call to this method at that time will always
   * immediately perform any necessary synchronization tasks.
   */
  private void synchronizeNetworkGame() {
    if ((movingBubble == null) && (networkManager != null)) {
      setGrid();
      lowerCompressorSteps(newSteps == 1);
    }
  }

  public void updatePenguinState(double dx) {
    if (dx < 0) {
      penguin.updateState(PenguinSprite.STATE_TURN_LEFT);
    }
    else if (dx > 0) {
      penguin.updateState(PenguinSprite.STATE_TURN_RIGHT);
    }
    else {
      penguin.updateState(PenguinSprite.STATE_VOID);
    }
  }
}
