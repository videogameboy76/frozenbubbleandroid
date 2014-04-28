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

/* This file is derived from the LunarView.java file which is part of
 * the Lunar Lander game included with Android documentation.  The
 * copyright notice for the Lunar Lander is reproduced below.
 */

/*
 * Copyright (c) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package org.jfedor.frozenbubble;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Vector;

import org.gsanson.frozenbubble.MalusBar;
import org.jfedor.frozenbubble.GameScreen.eventEnum;
import org.jfedor.frozenbubble.GameScreen.gameEnum;
import org.jfedor.frozenbubble.GameScreen.stateEnum;
import org.jfedor.frozenbubble.MultiplayerGameView.NetGameInterface.NetworkStatus;
import org.jfedor.frozenbubble.MultiplayerGameView.NetGameInterface.RemoteInterface;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.efortin.frozenbubble.ComputerAI;
import com.efortin.frozenbubble.HighscoreDO;
import com.efortin.frozenbubble.HighscoreManager;
import com.efortin.frozenbubble.NetworkGameManager;
import com.efortin.frozenbubble.NetworkGameManager.GameFieldData;
import com.efortin.frozenbubble.NetworkGameManager.PlayerAction;
import com.efortin.frozenbubble.NetworkGameManager.connectEnum;
import com.efortin.frozenbubble.VirtualInput;

public class MultiplayerGameView extends SurfaceView
  implements SurfaceHolder.Callback {

  public static final int  GAMEFIELD_WIDTH          = 320;
  public static final int  GAMEFIELD_HEIGHT         = 480;
  public static final int  EXTENDED_GAMEFIELD_WIDTH = 640;

  /*
   * The following screen orientation definitions were added to
   * ActivityInfo in API level 9.
   */
  public final static int SCREEN_ORIENTATION_SENSOR_LANDSCAPE  = 6;
  public final static int SCREEN_ORIENTATION_SENSOR_PORTRAIT   = 7;
  public final static int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8;
  public final static int SCREEN_ORIENTATION_REVERSE_PORTRAIT  = 9;

  private int                   numPlayer1GamesWon;
  private int                   numPlayer2GamesWon;
  private Context               mContext;
  private MultiplayerGameThread mGameThread;
  private NetworkGameManager    mNetworkManager;
  private RemoteInterface       remoteInterface;
  private ComputerAI            mOpponent;
  private VirtualInput          mLocalInput;
  private VirtualInput          mRemoteInput;
  private PlayerInput           mPlayer1;
  private PlayerInput           mPlayer2;
  private boolean               muteKeyToggle  = false;
  private boolean               pauseKeyToggle = false;

  //********************************************************************
  // Listener interface for various events
  //********************************************************************

  /**
   * Game event listener user set.
   * @author Eric Fortin
   *
   */
  public interface GameListener {
    public abstract void onGameEvent(eventEnum event);
  }

  GameListener mGameListener;

  public void setGameListener(GameListener gl) {
    mGameListener = gl;
  }

  /**
   * Network game interface.  This interface declares methods that must
   * be implemented by the network management class to implement a
   * distributed network multiplayer game.
   * @author Eric Fortin
   *
   */
  public interface NetGameInterface {
    /**
     * This class encapsulates player action and game field storage for
     * use by the game thread to determine when to process remote player
     * actions and game field bubble grid synchronization tasks.
     * @author Eric Fortin
     *
     */
    public class RemoteInterface {
      public boolean       gotAction;
      public boolean       gotFieldData;
      public PlayerAction  playerAction;
      public GameFieldData gameFieldData;

      public RemoteInterface(PlayerAction action, GameFieldData fieldData) {
        gotAction = false;
        gotFieldData = false;
        playerAction = action;
        gameFieldData = fieldData;
      }

      public void cleanUp() {
        gotAction = false;
        gotFieldData = false;
        playerAction = null;
        gameFieldData = null;
      }
    };

    /**
     * A class to encapsulate all network status variables used in
     * drawing the network status screen.
     * @author efortin
     *
     */
    public class NetworkStatus {
      public int     localPlayerId;
      public int     remotePlayerId;
      public boolean isConnected;
      public boolean reservedGameId;
      public boolean playerJoined;
      public boolean gotFieldData;
      public boolean gotPrefsData;
      public boolean readyToPlay;
      public String  localIpAddress;
      public String  remoteIpAddress;

      public NetworkStatus() {
        isConnected     = false;
        reservedGameId  = false;
        playerJoined    = false;
        gotFieldData    = false;
        gotPrefsData    = false;
        readyToPlay     = false;
        localIpAddress  = null;
        remoteIpAddress = null;
      }
    };

    /*
     * Force the implementer to supply the following methods.
     */
    public abstract void checkRemoteChecksum();
    public abstract void cleanUp();
    public abstract boolean gameIsReadyForAction();
    public abstract short getLatestRemoteActionId();
    public abstract boolean getRemoteAction();
    public abstract PlayerAction getRemoteActionPreview();
    public abstract RemoteInterface getRemoteInterface();
    public abstract void newGame();
    public abstract void pause();
    public abstract void sendLocalPlayerAction(int playerId,
                                               boolean compress,
                                               boolean launch,
                                               boolean swap,
                                               int keyCode,
                                               int launchColor,
                                               int nextColor,
                                               int newNextColor,
                                               int attackBarBubbles,
                                               byte attackBubbles[],
                                               double aimPosition);
    public abstract void setLocalChecksum(short checksum);
    public abstract void setRemoteChecksum(short checksum);
    public abstract void unPause();
    public abstract void updateNetworkStatus(NetworkStatus status);
  }

  /**
   * This class encapsulates player input action variables and methods.
   * <p>This is to provide a common interface to the game independent
   * of the input source.
   * @author Eric Fortin
   *
   */
  class PlayerInput extends VirtualInput {
    private boolean mCenter        = false;
    private boolean mDown          = false;
    private boolean mLeft          = false;
    private boolean mRight         = false;
    private boolean mUp            = false;
    private double  mTrackballDx   = 0;
    private boolean mTouchSwap     = false;
    private double  mTouchX;
    private double  mTouchY;
    private boolean mTouchFireATS  = false;
    private double  mTouchDxATS    = 0;
    private double  mTouchLastX    = 0;

    /**
     * Construct and configure this player input instance.
     * @param id - the player ID, e.g.,
     * <code>VirtualInput.PLAYER1</code>.
     * @param type - <code>true</code> if the player is a simulation.
     * @param remote - <code>true</code> if this player is playing on a
     * remote machine, <code>false</code> if this player is local.
     * @see VirtualInput
     */
    public PlayerInput(int id, boolean type, boolean remote) {
      init();
      configure(id, type, remote);
    }

    /**
     * Check if a center button press action is active.
     * @return True if the player pressed the center button.
     */
    public boolean actionCenter() {
      boolean tempCenter = mWasCenter;
      mWasCenter = false;
      return tempCenter;
    }

    /**
     * Check if a bubble launch action is active.
     * @return True if the player is launching a bubble.
     */
    public boolean actionUp() {
      boolean tempFire = mWasCenter || mWasUp;
      mWasCenter = false;
      mWasUp = false;
      return mCenter || mUp || tempFire;
    }

    /**
     * Check if a move left action is active.
     * @return True if the player is moving left.
     */
    public boolean actionLeft() {
      boolean tempLeft = mWasLeft;
      mWasLeft = false;
      return mLeft || tempLeft;
    }

    /**
     * Check if a move right action is active.
     * @return True if the player is moving right.
     */
    public boolean actionRight() {
      boolean tempRight = mWasRight;
      mWasRight = false;
      return mRight || tempRight;
    }

    /**
     * Check if a bubble swap action is active.
     * @return True if the player is swapping the launch bubble.
     */
    public boolean actionDown() {
      boolean tempSwap = mWasDown || mTouchSwap;
      mWasDown = false;
      mTouchSwap = false;
      return mDown || tempSwap;
    }

    /**
     * Check if a touchscreen initiated bubble launch is active.
     * @return True if the player is launching a bubble.
     */
    public boolean actionTouchFire() {
      boolean tempFire = mTouchFire;
      mTouchFire = false;
      return tempFire;
    }

    /**
     * Check if an ATS (aim-then-shoot) touchscreen initiated bubble
     * launch is active.
     * @return True if the player is launching a bubble.
     */
    public boolean actionTouchFireATS() {
      boolean tempFire = mTouchFireATS;
      mTouchFireATS = false;
      return tempFire;
    }

    /**
     * Based on the provided keypress, check if it corresponds to a new
     * player action.
     * @param keyCode
     * @return True if the current keypress indicates a new player action.
     */
    public boolean checkNewActionKeyPress(int keyCode) {
      return (!mLeft && !mRight && !mCenter && !mUp && !mDown) &&
             ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ||
              (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) ||
              (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) ||
              (keyCode == KeyEvent.KEYCODE_DPAD_UP) ||
              (keyCode == KeyEvent.KEYCODE_DPAD_DOWN));
    }

    /**
     * Obtain the ATS (aim-then-shoot) touch horizontal position change.
     * @return The horizontal touch change in position.
     */
    public double getTouchDxATS() {
      double tempDx = mTouchDxATS;
      mTouchDxATS = 0;
      return tempDx;
    }

    /**
     * Obtain the horizontal touch position.
     * @return The horizontal touch position.
     */
    public double getTouchX() {
      return mTouchX;
    }

    /**
     * Obtain the vertical touch position.
     * @return The vertical touch position.
     */
    public double getTouchY() {
      return mTouchY;
    }

    /**
     * Obtain the trackball position change.
     * @return The trackball position change.
     */
    public double getTrackBallDx() {
      double tempDx = mTrackballDx;
      mTrackballDx = 0;
      return tempDx;
    }

    public void init() {
      this.init_vars();
      mTrackballDx  = 0;
      mTouchFire    = false;
      mTouchSwap    = false;
      mTouchFireATS = false;
      mTouchDxATS   = 0;
    }

    /**
     * Process key presses.
     * @param keyCode
     * @return True if the key press was processed, false if not.
     */
    public boolean setKeyDown(int keyCode) {
      if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        mLeft    = true;
        mWasLeft = true;
        return true;
      }
      else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        mRight    = true;
        mWasRight = true;
        return true;
      }
      else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        mCenter    = true;
        mWasCenter = true;
        return true;
      }
      else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
        mUp    = true;
        mWasUp = true;
        return true;
      }
      else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        mDown    = true;
        mWasDown = true;
        return true;
      }
      return false;
    }

    /**
     * Process key releases.
     * @param keyCode
     * @return True if the key release was processed, false if not.
     */
    public boolean setKeyUp(int keyCode) {
      if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        mLeft = false;
        return true;
      }
      else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        mRight = false;
        return true;
      }
      else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        mCenter = false;
        return true;
      }
      else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
        mUp = false;
        return true;
      }
      else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        mDown = false;
        return true;
      }
      return false;
    }

    public boolean setTouchEvent(int event, double x, double y) {
      if (mGameThread.mMode == stateEnum.RUNNING) {
        // Set the values used when Point To Shoot is on.
        if (event == MotionEvent.ACTION_DOWN) {
          if (y < MultiplayerGameThread.TOUCH_FIRE_Y_THRESHOLD) {
            mTouchFire = true;
            mTouchX = x;
            mTouchY = y;
          }
          else if (Math.abs(x - 318) <=
                   MultiplayerGameThread.TOUCH_SWAP_X_THRESHOLD)
            mTouchSwap = true;
        }

        // Set the values used when Aim Then Shoot is on.
        if (event == MotionEvent.ACTION_DOWN) {
          if (y < MultiplayerGameThread.ATS_TOUCH_FIRE_Y_THRESHOLD) {
            mTouchFireATS = true;
          }
          mTouchLastX = x;
        }
        else if (event == MotionEvent.ACTION_MOVE) {
          if (y >= MultiplayerGameThread.ATS_TOUCH_FIRE_Y_THRESHOLD) {
            mTouchDxATS = (x - mTouchLastX) *
            MultiplayerGameThread.ATS_TOUCH_COEFFICIENT;
          }
          mTouchLastX = x;
        }
        return true;
      }
      return false;
    }

    /**
     * Accumulate the change in trackball horizontal position.
     * @param trackBallDX
     */
    public void setTrackBallDx(double trackBallDX) {
      mTrackballDx += trackBallDX;
    }
  }

  private boolean checkImmediateAction() {
    boolean actNow = false;
    /*
     * Preview the current action if one is available to see if it
     * contains an asynchronous action (e.g., launch bubble swap).
     */
    PlayerAction previewAction = mNetworkManager.getRemoteActionPreview();

    if (previewAction != null) {
      actNow = previewAction.compress || previewAction.swapBubble;
    }

    return actNow;
  }

  private void monitorRemotePlayer() {
    if ((mNetworkManager != null) && (mRemoteInput != null)) {
      /*
       * Check the remote player interface for game field updates.
       * Reject the game field data if it doesn't correspond to the
       * latest remote player game field.  This is determined based on
       * whether the game field data action ID matches the latest remote
       * player action ID.
       */
      if (remoteInterface.gotFieldData) {
        if (mNetworkManager.getLatestRemoteActionId() ==
            remoteInterface.gameFieldData.localActionID) {
          setPlayerGameField(remoteInterface.gameFieldData);
        }
        remoteInterface.gotFieldData = false;
      }

      /*
       * Once the game is ready, if the game thread is not running, then
       * allow the remote player to update the game thread state.
       *
       * If an asynchronous action is available or we are clear to
       * perform a synchronous action, retrieve and clear the current
       * available action from the action queue.
       */
      if (mNetworkManager.gameIsReadyForAction() && (checkImmediateAction() ||
          mRemoteInput.mGameRef.getOkToFire() ||
          (mGameThread.mMode != stateEnum.RUNNING))) {
        if (mNetworkManager.getRemoteAction()) {
          setPlayerAction(remoteInterface.playerAction);
          remoteInterface.gotAction = false;
        }
        else if (mRemoteInput.mGameRef.getOkToFire()) {
          mNetworkManager.checkRemoteChecksum();
        }
      }
    }
  }

  /**
   * Set the player action for a remote player - as in a person playing
   * via a client device over a network.
   * @param newAction - the object containing the remote input info.
   */
  private synchronized void setPlayerAction(PlayerAction newAction) {
    if (newAction == null) {
      return;
    }

    VirtualInput playerRef;

    if (newAction.playerID == VirtualInput.PLAYER1) {
      playerRef = mPlayer1;
    }
    else if (newAction.playerID == VirtualInput.PLAYER2) {
      playerRef = mPlayer2;
    }
    else {
      return;
    }

    if (mGameThread != null)
      mGameThread.updateStateOnEvent(null);

    /*
     * Set the launcher bubble colors.
     */
    if ((newAction.launchBubbleColor  > -1) &&
        (newAction.launchBubbleColor  <  8) &&
        (newAction.nextBubbleColor    > -1) &&
        (newAction.nextBubbleColor    <  8) &&
        (newAction.newNextBubbleColor > -1) &&
        (newAction.newNextBubbleColor <  8)) {
      playerRef.mGameRef.setLaunchBubbleColors(newAction.launchBubbleColor,
                                               newAction.nextBubbleColor,
                                               newAction.newNextBubbleColor);
    }

    /*
     * Set the launcher aim position.
     */
    playerRef.mGameRef.setPosition(newAction.aimPosition);

    /*
     * Process a compressor lower request.
     */
    if (newAction.compress) {
      playerRef.mGameRef.lowerCompressor(true);
    }

    /*
     * Process a bubble launch request.
     */
    if (newAction.launchBubble) {
      playerRef.setAction(KeyEvent.KEYCODE_DPAD_UP, true);
    }

    /*
     * Process a bubble swap request.
     */
    if (newAction.swapBubble) {
      playerRef.setAction(KeyEvent.KEYCODE_DPAD_DOWN, false);
    }

    /*
     * Process a pause/play button toggle request.
     */
    if (newAction.keyCode == (byte) KeyEvent.KEYCODE_P) {
      if (mGameThread != null) {
        mGameThread.toggleKeyPress(KeyEvent.KEYCODE_P, true, false);
      }
    }

    /*
     * Set the current value of the attack bar.
     */
    if (newAction.attackBarBubbles > -1) {
      playerRef.mGameRef.malusBar.setAttackBubbles(newAction.attackBarBubbles,
                                                   newAction.attackBubbles);
    }
  }

  /**
   * Set the game field for a remote player - as in a person playing
   * via a client device over a network.
   * @param newGameField - the object containing the remote field data.
   */
  private void setPlayerGameField(GameFieldData newField) {
    if (newField == null) {
      return;
    }

    VirtualInput playerRef;

    if (newField.playerID == VirtualInput.PLAYER1) {
      playerRef = mPlayer1;
    }
    else if (newField.playerID == VirtualInput.PLAYER2) {
      playerRef = mPlayer2;
    }
    else {
      return;
    }

    /*
     * Set the bubble grid.  Note this must be done before lowering the
     * compressor, as bubbles will be created using a non-compressed
     * game field!
     */
    playerRef.mGameRef.setGrid(newField.gameField);

    /*
     * Lower the compressor and bubbles in play to the required number
     * of compressor steps.
     */
    playerRef.mGameRef.setCompressorSteps(newField.compressorSteps);

    /*
     * Set the launcher bubble colors.
     */
    playerRef.mGameRef.setLaunchBubbleColors(newField.launchBubbleColor,
                                             newField.nextBubbleColor,
                                             playerRef.mGameRef.getNewNextColor());

    /*
     * Set the current value of the attack bar.
     */
    playerRef.mGameRef.malusBar.setAttackBubbles(newField.attackBarBubbles,
                                                 null);
  }

  class MultiplayerGameThread extends Thread {

    private static final int FRAME_DELAY = 40;

    public static final double TRACKBALL_COEFFICIENT      = 5;
    public static final double TOUCH_BUTTON_THRESHOLD     = 16;
    public static final double TOUCH_FIRE_Y_THRESHOLD     = 380;
    public static final double TOUCH_SWAP_X_THRESHOLD     = 14;
    public static final double ATS_TOUCH_COEFFICIENT      = 0.2;
    public static final double ATS_TOUCH_FIRE_Y_THRESHOLD = 350;

    private boolean mImagesReady = false;
    private boolean mRun         = false;
    private boolean mShowNetwork = false;
    private boolean mShowScores  = false;
    private boolean mSurfaceOK   = false;

    private int    mDisplayDX;
    private int    mDisplayDY;
    private double mDisplayScale;
    private long   mLastTime;
    private int    mPlayer1DX;
    private int    mPlayer2DX;

    private stateEnum mMode;
    private stateEnum mModeWas;

    private Bitmap mBackgroundOrig;
    private Bitmap[] mBubblesOrig;
    private Bitmap[] mBubblesBlindOrig;
    private Bitmap[] mFrozenBubblesOrig;
    private Bitmap[] mTargetedBubblesOrig;
    private Bitmap mBubbleBlinkOrig;
    private Bitmap mGameWonOrig;
    private Bitmap mGameLostOrig;
    private Bitmap mGamePausedOrig;
    private Bitmap mHurryOrig;
    private Bitmap mPauseButtonOrig;
    private Bitmap mPlayButtonOrig;
    private Bitmap mPenguinsOrig;
    private Bitmap mPenguins2Orig;
    private Bitmap mCompressorHeadOrig;
    private Bitmap mCompressorOrig;
    private Bitmap mLifeOrig;
    private Bitmap mFontImageOrig;
    private Bitmap mBananaOrig;
    private Bitmap mTomatoOrig;
    private BmpWrap mBackground;
    private BmpWrap[] mBubbles;
    private BmpWrap[] mBubblesBlind;
    private BmpWrap[] mFrozenBubbles;
    private BmpWrap[] mTargetedBubbles;
    private BmpWrap mBubbleBlink;
    private BmpWrap mGameWon;
    private BmpWrap mGameLost;
    private BmpWrap mGamePaused;
    private BmpWrap mHurry;
    private BmpWrap mPauseButton;
    private BmpWrap mPlayButton;
    private BmpWrap mPenguins;
    private BmpWrap mPenguins2;
    private BmpWrap mCompressorHead;
    private BmpWrap mCompressor;
    private BmpWrap mLife;
    private BmpWrap mFontImage;
    private BmpWrap mBanana;
    private BmpWrap mTomato;

    private BubbleFont    mFont;
    private Drawable      mLauncher;  // drawable because we rotate it
    private FrozenGame    mFrozenGame1;
    private FrozenGame    mFrozenGame2;
    private LevelManager  mLevelManager;
    private MalusBar      malusBar1;
    private MalusBar      malusBar2;
    private SoundManager  mSoundManager;
    private SurfaceHolder mSurfaceHolder;

    private final HighscoreManager mHighScoreManager;

    Vector<BmpWrap> mImageList;

    public void cleanUp() {
      synchronized (mSurfaceHolder) {
        // I don't really understand why all this is necessary.
        // I used to get a crash (an out-of-memory error) once every six or
        // seven times I started the game.  I googled the error and someone
        // said you have to call recycle() on all the bitmaps and set
        // the pointers to null to facilitate garbage collection.  So I did
        // and the crashes went away.
        mFrozenGame1 = null;
        mFrozenGame2 = null;
        mImagesReady = false;

        boolean imagesScaled = (mBackgroundOrig == mBackground.bmp);
        mBackgroundOrig.recycle();
        mBackgroundOrig = null;

        for (int i = 0; i < mBubblesOrig.length; i++) {
          mBubblesOrig[i].recycle();
          mBubblesOrig[i] = null;
        }
        mBubblesOrig = null;

        for (int i = 0; i < mBubblesBlindOrig.length; i++) {
          mBubblesBlindOrig[i].recycle();
          mBubblesBlindOrig[i] = null;
        }
        mBubblesBlindOrig = null;

        for (int i = 0; i < mFrozenBubblesOrig.length; i++) {
          mFrozenBubblesOrig[i].recycle();
          mFrozenBubblesOrig[i] = null;
        }
        mFrozenBubblesOrig = null;

        for (int i = 0; i < mTargetedBubblesOrig.length; i++) {
          mTargetedBubblesOrig[i].recycle();
          mTargetedBubblesOrig[i] = null;
        }
        mTargetedBubblesOrig = null;

        mBubbleBlinkOrig.recycle();
        mBubbleBlinkOrig = null;
        mGameWonOrig.recycle();
        mGameWonOrig = null;
        mGameLostOrig.recycle();
        mGameLostOrig = null;
        mGamePausedOrig.recycle();
        mGamePausedOrig = null;
        mHurryOrig.recycle();
        mHurryOrig = null;
        mPauseButtonOrig.recycle();
        mPauseButtonOrig = null;
        mPlayButtonOrig.recycle();
        mPlayButtonOrig = null;
        mPenguinsOrig.recycle();
        mPenguinsOrig = null;
        mPenguins2Orig.recycle();
        mPenguins2Orig = null;
        mCompressorHeadOrig.recycle();
        mCompressorHeadOrig = null;
        mCompressorOrig.recycle();
        mCompressorOrig = null;
        mLifeOrig.recycle();
        mLifeOrig = null;
        mBananaOrig.recycle();
        mBananaOrig = null;
        mTomatoOrig.recycle();
        mTomatoOrig = null;

        if (imagesScaled) {
          mBackground.bmp.recycle();
          for (int i = 0; i < mBubbles.length; i++) {
            mBubbles[i].bmp.recycle();
          }

          for (int i = 0; i < mBubblesBlind.length; i++) {
            mBubblesBlind[i].bmp.recycle();
          }

          for (int i = 0; i < mFrozenBubbles.length; i++) {
            mFrozenBubbles[i].bmp.recycle();
          }

          for (int i = 0; i < mTargetedBubbles.length; i++) {
            mTargetedBubbles[i].bmp.recycle();
          }

          mBubbleBlink.bmp.recycle();
          mGameWon.bmp.recycle();
          mGameLost.bmp.recycle();
          mGamePaused.bmp.recycle();
          mHurry.bmp.recycle();
          mPauseButton.bmp.recycle();
          mPlayButton.bmp.recycle();
          mPenguins.bmp.recycle();
          mPenguins2.bmp.recycle();
          mCompressorHead.bmp.recycle();
          mCompressor.bmp.recycle();
          mLife.bmp.recycle();
          mBanana.bmp.recycle();
          mTomato.bmp.recycle();
        }
        mBackground.bmp = null;
        mBackground = null;

        for (int i = 0; i < mBubbles.length; i++) {
          mBubbles[i].bmp = null;
          mBubbles[i] = null;
        }
        mBubbles = null;

        for (int i = 0; i < mBubblesBlind.length; i++) {
          mBubblesBlind[i].bmp = null;
          mBubblesBlind[i] = null;
        }
        mBubblesBlind = null;

        for (int i = 0; i < mFrozenBubbles.length; i++) {
          mFrozenBubbles[i].bmp = null;
          mFrozenBubbles[i] = null;
        }
        mFrozenBubbles = null;

        for (int i = 0; i < mTargetedBubbles.length; i++) {
          mTargetedBubbles[i].bmp = null;
          mTargetedBubbles[i] = null;
        }
        mTargetedBubbles = null;

        mBubbleBlink.bmp = null;
        mBubbleBlink = null;
        mGameWon.bmp = null;
        mGameWon = null;
        mGameLost.bmp = null;
        mGameLost = null;
        mGamePaused.bmp = null;
        mGamePaused = null;
        mHurry.bmp = null;
        mHurry = null;
        mPauseButton.bmp = null;
        mPauseButton = null;
        mPlayButton.bmp = null;
        mPlayButton = null;
        mPenguins.bmp = null;
        mPenguins = null;
        mPenguins2.bmp = null;
        mPenguins2 = null;
        mCompressorHead.bmp = null;
        mCompressorHead = null;
        mCompressor.bmp = null;
        mCompressor = null;
        mLife.bmp = null;
        mLife = null;
        mBanana.bmp = null;
        mBanana = null;
        mTomato.bmp = null;
        mTomato = null;

        mImageList = null;
        mSoundManager.cleanUp();
        mSoundManager = null;
        mLevelManager = null;

        if (mHighScoreManager != null) {
          mHighScoreManager.close();
        }
      }
    }

    private void doDraw(Canvas canvas) {
      //Log.i("frozen-bubble", "doDraw()");
      if (! mImagesReady) {
        //Log.i("frozen-bubble", "!mImagesReady, returning");
        return;
      }
      if ((mDisplayDX > 0) || (mDisplayDY > 0)) {
        //Log.i("frozen-bubble", "Drawing black background.");
        canvas.drawRGB(0, 0, 0);
      }
      drawBackground(canvas);
      drawWinTotals(canvas);
      mFrozenGame1.paint(canvas, mDisplayScale, mPlayer1DX, mDisplayDY);
      mFrozenGame2.paint(canvas, mDisplayScale, mPlayer2DX, mDisplayDY);
    }

    /**
     * Process key presses.  This must be allowed to run regardless of
     * the game state to correctly handle initial game conditions.
     * @param keyCode - the static KeyEvent key identifier.
     * @param msg - the key action message.
     * @return - <code>true</code> if the key action is processed.
     * @see android.view.View#onKeyDown(int, android.view.KeyEvent)
     */
    boolean doKeyDown(int keyCode, KeyEvent msg) {
      synchronized (mSurfaceHolder) {
        /*
         * Only update the game state if this is a fresh key press.
         */
        if (mLocalInput.checkNewActionKeyPress(keyCode))
          updateStateOnEvent(null);

        /*
         * Process the key press if it is a function key.
         */
        toggleKeyPress(keyCode, true, true);

        /*
         * Process the key press if it is a game input key.
         */
        return mLocalInput.setKeyDown(keyCode);
      }
    }

    /**
     * Process key releases.  This must be allowed to run regardless of
     * the game state in order to properly clear key presses.
     * @param keyCode - the static KeyEvent key identifier.
     * @param msg - the key action message.
     * @return - <code>true</code> if the key action is processed.
     * @see android.view.View#onKeyUp(int, android.view.KeyEvent)
     */
    boolean doKeyUp(int keyCode, KeyEvent msg) {
      synchronized (mSurfaceHolder) {
        return mLocalInput.setKeyUp(keyCode);
      }
    }

    /**
     * This method handles screen touch motion events.
     * <p>This method will be called three times in succession for each
     * touch, to process <code>ACTION_DOWN</code>,
     * <code>ACTION_UP</code>, and <code>ACTION_MOVE</code>.
     * @param event - the motion event
     * @return <code>true</code> if the event was handled..
     */
    boolean doTouchEvent(MotionEvent event) {
      synchronized (mSurfaceHolder) {
        double x_offset;
        double x = xFromScr(event.getX());
        double y = yFromScr(event.getY());

        if (mLocalInput.playerID == VirtualInput.PLAYER1)
          x_offset = 0;
        else
          x_offset = -318;
        /*
         * Check for a pause button sprite press.  This will toggle the
         * pause button sprite between pause and play.  If the game was
         * previously paused by the pause button, ignore screen touches
         * that aren't on the pause button sprite.
         */
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          if ((Math.abs(x - 183) <= TOUCH_BUTTON_THRESHOLD) &&
              (Math.abs(y - 460) <= TOUCH_BUTTON_THRESHOLD)) {
            toggleKeyPress(KeyEvent.KEYCODE_P, false, true);
          }
          else if (toggleKeyState(KeyEvent.KEYCODE_P))
            return false;
        }

        /*
         * Update the game state (paused, running, etc.) if necessary.
         */
        if(updateStateOnEvent(event))
          return true;

        /*
         * If the game is running and the pause button sprite was pressed,
         * pause the game.
         */
        if ((mMode == stateEnum.RUNNING) &&
            (toggleKeyState(KeyEvent.KEYCODE_P)))
          pause();

        /*
         * Process the screen touch event.
         */
        return mLocalInput.setTouchEvent(event.getAction(), x + x_offset, y);
      }
    }

    /**
     * Process trackball motion events.
     * <p>This method only processes trackball motion for the purpose of
     * aiming the launcher.  The trackball has no effect on the game
     * state, much like moving a mouse cursor over a screen does not
     * perform any intrinsic actions in most applications.
     * @param event - the motion event associated with the trackball.
     * @return This function returns <code>true</code> if the trackball
     * motion was processed, which notifies the caller that this method
     * handled the motion event and no other handling is necessary.
     */
    boolean doTrackballEvent(MotionEvent event) {
      synchronized (mSurfaceHolder) {
        if (mMode == stateEnum.RUNNING) {
          if (event.getAction() == MotionEvent.ACTION_MOVE) {
            mLocalInput.setTrackBallDx(event.getX() * TRACKBALL_COEFFICIENT);
            return true;
          }
        }
        return false;
      }
    }

    private void drawAboutScreen(Canvas canvas) {
      canvas.drawRGB(0, 0, 0);
      int x = 168;
      int y = 20;
      int ysp = 26;
      int indent = 10;
      int orientation = getScreenOrientation();

      if (orientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT)
        x += GAMEFIELD_WIDTH/2;
      else if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        x -= GAMEFIELD_WIDTH/2;

      mFont.print("original frozen bubble:", x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("guillaume cottenceau", x + indent, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("alexis younes", x + indent, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("amaury amblard-ladurantie", x + indent, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("matthias le bidan", x + indent, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      y += ysp;
      mFont.print("java version:", x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("glenn sanson", x + indent, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      y += ysp;
      mFont.print("android port:", x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("aleksander fedorynski", x + indent, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("eric fortin", x + indent, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += 2 * ysp;
      mFont.print("android port source code", x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("is available at:", x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("http://code.google.com", x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;
      mFont.print("/p/frozenbubbleandroid", x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
    }

    private void drawBackground(Canvas c) {
      Sprite.drawImage(mBackground, 0, 0, c, mDisplayScale,
                       mDisplayDX, mDisplayDY);
    }

    /**
     * Draw the high score screen for multiplayer game mode.
     * <p>The objective of multiplayer game mode is endurance - fire as
     * many bubbles as possible for as long as possible.  Thus the high
     * score will exhibit the most shots fired during the longest game.
     * @param canvas - the drawing canvas to display the scores on.
     * @param level - the level difficulty index.
     */
    private void drawHighScoreScreen(Canvas canvas, int level) {
      if (mHighScoreManager == null) {
        mShowScores = false;
        return;
      }

      canvas.drawRGB(0, 0, 0);
      int x = 168;
      int y = 20;
      int ysp = 26;
      int indent = 10;
      int orientation = getScreenOrientation();

      if (orientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT)
        x += GAMEFIELD_WIDTH/2;
      else if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        x -= GAMEFIELD_WIDTH/2;

      mFont.print("highscore for " +
                  LevelManager.DifficultyStrings[mHighScoreManager.getLevel()],
                  x, y, canvas, mDisplayScale, mDisplayDX, mDisplayDY);
      y += 2 * ysp;

      List<HighscoreDO> hlist = mHighScoreManager.getLowScore(level, 15);
      long lastScoreId = mHighScoreManager.getLastScoreId();
      int i = 1;
      for (HighscoreDO hdo : hlist) {
        String you = "";
        if (lastScoreId == hdo.getId()) {
          you = "|";
        }
        // TODO: Add player name support.
        // mFont.print(you + i++ + " - " + hdo.getName().toLowerCase()
        // + " - "
        // + hdo.getShots()
        // + " - " + (hdo.getTime() / 1000)
        // + " sec", x + indent,
        // y, canvas,
        // mDisplayScale, mDisplayDX, mDisplayDY);
        mFont.print(you + i++ + " - "
          + hdo.getShots()
          + " shots - "
          + (hdo.getTime() / 1000)
          + " sec", x + indent,
          y, canvas,
          mDisplayScale, mDisplayDX, mDisplayDY);
        y += ysp;
      }
    }

    private void drawNetworkScreen(Canvas canvas) {
      if (mNetworkManager == null) {
        mShowNetwork = false;
        return;
      }

      canvas.drawRGB(0, 0, 0);
      int x = 168;
      int y = 20;
      int ysp = 26;
      int orientation = getScreenOrientation();

      if (orientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT)
        x += GAMEFIELD_WIDTH/2;
      else if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        x -= GAMEFIELD_WIDTH/2;

      NetworkStatus status = new NetworkStatus();
      mNetworkManager.updateNetworkStatus(status);

      if (status.isConnected) {
        mFont.print("internet status: ]", x, y, canvas,
                    mDisplayScale, mDisplayDX, mDisplayDY);
        y += ysp;
      }
      else {
        mFont.print("internet status: _", x, y, canvas,
                    mDisplayScale, mDisplayDX, mDisplayDY);
        y += ysp;
      }

      mFont.print("my address: " + status.localIpAddress, x, y, canvas,
          mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;

      mFont.print("connect to: " + status.remoteIpAddress, x, y, canvas,
          mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;

      if (status.reservedGameId) {
        mFont.print("checking for games...|", x, y, canvas,
                    mDisplayScale, mDisplayDX, mDisplayDY);
        y += ysp;
      }
      else {
        mFont.print("checking for games...", x, y, canvas,
                    mDisplayScale, mDisplayDX, mDisplayDY);
        return;
      }

      mFont.print("open game slot found!", x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += ysp;

      if (status.playerJoined) {
        mFont.print("waiting for player " + status.remotePlayerId + "...|",
                    x, y, canvas, mDisplayScale, mDisplayDX, mDisplayDY);
        y += ysp;
      }
      else {
        mFont.print("waiting for player " + status.remotePlayerId + "...",
                    x, y, canvas, mDisplayScale, mDisplayDX, mDisplayDY);
        return;
      }

      if (status.localPlayerId == VirtualInput.PLAYER2) {
        if (status.gotPrefsData || status.readyToPlay) {
          mFont.print("getting preferences...|", x, y, canvas,
                      mDisplayScale, mDisplayDX, mDisplayDY);
          y += ysp;
        }
        else {
          mFont.print("getting preferences...", x, y, canvas,
                      mDisplayScale, mDisplayDX, mDisplayDY);
          return;
        }
      }

      if (status.gotFieldData || status.readyToPlay) {
        mFont.print("getting data...|", x, y, canvas,
                    mDisplayScale, mDisplayDX, mDisplayDY);
        y += ysp;
      }
      else {
        mFont.print("getting data...", x, y, canvas,
                    mDisplayScale, mDisplayDX, mDisplayDY);
        return;
      }

      if (status.readyToPlay) {
        mFont.print("waiting for game start...|", x, y, canvas,
                    mDisplayScale, mDisplayDX, mDisplayDY);
        y += ysp;
      }
      else {
        mFont.print("waiting for game start...", x, y, canvas,
                    mDisplayScale, mDisplayDX, mDisplayDY);
        return;
      }

      mFont.print("tap to begin playing!", x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
    }

    private void drawWinTotals(Canvas canvas) {
      int y = 433;
      int x = GAMEFIELD_WIDTH - 40;
      int gamesWon1 = numPlayer1GamesWon;
      int gamesWon2 = numPlayer2GamesWon;

      if (numPlayer1GamesWon < 10) {
        x += 12;
        x += mFont.paintChar(Character.forDigit(gamesWon1, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
      }
      else if (numPlayer1GamesWon < 100) {
        x += 5;
        x += mFont.paintChar(Character.forDigit(gamesWon1 / 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        x += mFont.paintChar(Character.forDigit(gamesWon1 % 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
      }
      else {
        x += mFont.paintChar(Character.forDigit(gamesWon1 / 100, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        gamesWon1 -= 100 * (gamesWon1 / 100);
        x += mFont.paintChar(Character.forDigit(gamesWon1 / 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        x += mFont.paintChar(Character.forDigit(gamesWon1 % 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
      }
      x += 7;
      x += mFont.paintChar('-', x, y, canvas,
                           mDisplayScale, mDisplayDX, mDisplayDY);
      x += 7;
      if (numPlayer2GamesWon < 10) {
        x += mFont.paintChar(Character.forDigit(gamesWon2, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
      }
      else if (numPlayer2GamesWon < 100) {
        x += mFont.paintChar(Character.forDigit(gamesWon2 / 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        x += mFont.paintChar(Character.forDigit(gamesWon2 % 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
      }
      else {
        x += mFont.paintChar(Character.forDigit(gamesWon2 / 100, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        gamesWon1 -= 100 * (gamesWon2 / 100);
        x += mFont.paintChar(Character.forDigit(gamesWon2 / 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        x += mFont.paintChar(Character.forDigit(gamesWon2 % 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
      }
    }

    public int getCurrentLevelIndex() {
      synchronized (mSurfaceHolder) {
        return mLevelManager.getLevelIndex();
      }
    }

    private int getScreenOrientation() {
      /*
       * The method getOrientation() was deprecated in API level 8.
       *
       * For API level 8 or greater, use getRotation().
       */
      int rotation = ((Activity) mContext).getWindowManager().
        getDefaultDisplay().getOrientation();
      DisplayMetrics dm = new DisplayMetrics();
      ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(dm);
      int width  = dm.widthPixels;
      int height = dm.heightPixels;
      int orientation;
      /*
       * The orientation determination is based on the natural orienation
       * mode of the device, which can be either portrait, landscape, or
       * square.
       *
       * After the natural orientation is determined, convert the device
       * rotation into a fully qualified orientation.
       */
      if ((((rotation == Surface.ROTATION_0  ) ||
            (rotation == Surface.ROTATION_180)) && (height > width)) ||
          (((rotation == Surface.ROTATION_90 ) ||
            (rotation == Surface.ROTATION_270)) && (width  > height))) {
        /*
         * Natural orientation is portrait.
         */
        switch(rotation) {
          case Surface.ROTATION_0:
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            break;
          case Surface.ROTATION_90:
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            break;
          case Surface.ROTATION_180:
            orientation = SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            break;
          case Surface.ROTATION_270:
            orientation = SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            break;
          default:
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            break;              
        }
      }
      else {
        /*
         * Natural orientation is landscape or square.
         */
        switch(rotation) {
          case Surface.ROTATION_0:
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            break;
          case Surface.ROTATION_90:
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            break;
          case Surface.ROTATION_180:
            orientation = SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            break;
          case Surface.ROTATION_270:
            orientation = SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            break;
          default:
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            break;              
        }
      }

      return orientation;
    }

    private BmpWrap NewBmpWrap() {
      int new_img_id = mImageList.size();
      BmpWrap new_img = new BmpWrap(new_img_id);
      mImageList.addElement(new_img);
      return new_img;
    }

    public MultiplayerGameThread(SurfaceHolder surfaceHolder) {
      //Log.i("frozen-bubble", "GameThread()");
      mSurfaceHolder = surfaceHolder;
      Resources res = mContext.getResources();
      setState(stateEnum.PAUSED);

      BitmapFactory.Options options = new BitmapFactory.Options();

      /*
       * The Options.inScaled field is only available starting at API 4.
       */
      try {
        Field f = options.getClass().getField("inScaled");
        f.set(options, Boolean.FALSE);
      } catch (Exception ignore) {}

      mBackgroundOrig = BitmapFactory.decodeResource(
        res, R.drawable.background2, options);
      mBubblesOrig = new Bitmap[8];
      mBubblesOrig[0] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_1, options);
      mBubblesOrig[1] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_2, options);
      mBubblesOrig[2] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_3, options);
      mBubblesOrig[3] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_4, options);
      mBubblesOrig[4] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_5, options);
      mBubblesOrig[5] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_6, options);
      mBubblesOrig[6] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_7, options);
      mBubblesOrig[7] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_8, options);
      mBubblesBlindOrig = new Bitmap[8];
      mBubblesBlindOrig[0] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_colourblind_1, options);
      mBubblesBlindOrig[1] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_colourblind_2, options);
      mBubblesBlindOrig[2] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_colourblind_3, options);
      mBubblesBlindOrig[3] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_colourblind_4, options);
      mBubblesBlindOrig[4] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_colourblind_5, options);
      mBubblesBlindOrig[5] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_colourblind_6, options);
      mBubblesBlindOrig[6] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_colourblind_7, options);
      mBubblesBlindOrig[7] = BitmapFactory.decodeResource(
        res, R.drawable.bubble_colourblind_8, options);
      mFrozenBubblesOrig = new Bitmap[8];
      mFrozenBubblesOrig[0] = BitmapFactory.decodeResource(
        res, R.drawable.frozen_1, options);
      mFrozenBubblesOrig[1] = BitmapFactory.decodeResource(
        res, R.drawable.frozen_2, options);
      mFrozenBubblesOrig[2] = BitmapFactory.decodeResource(
        res, R.drawable.frozen_3, options);
      mFrozenBubblesOrig[3] = BitmapFactory.decodeResource(
        res, R.drawable.frozen_4, options);
      mFrozenBubblesOrig[4] = BitmapFactory.decodeResource(
        res, R.drawable.frozen_5, options);
      mFrozenBubblesOrig[5] = BitmapFactory.decodeResource(
        res, R.drawable.frozen_6, options);
      mFrozenBubblesOrig[6] = BitmapFactory.decodeResource(
        res, R.drawable.frozen_7, options);
      mFrozenBubblesOrig[7] = BitmapFactory.decodeResource(
        res, R.drawable.frozen_8, options);
      mTargetedBubblesOrig = new Bitmap[6];
      mTargetedBubblesOrig[0] = BitmapFactory.decodeResource(
        res, R.drawable.fixed_1, options);
      mTargetedBubblesOrig[1] = BitmapFactory.decodeResource(
        res, R.drawable.fixed_2, options);
      mTargetedBubblesOrig[2] = BitmapFactory.decodeResource(
        res, R.drawable.fixed_3, options);
      mTargetedBubblesOrig[3] = BitmapFactory.decodeResource(
        res, R.drawable.fixed_4, options);
      mTargetedBubblesOrig[4] = BitmapFactory.decodeResource(
        res, R.drawable.fixed_5, options);
      mTargetedBubblesOrig[5] = BitmapFactory.decodeResource(
        res, R.drawable.fixed_6, options);
      mBubbleBlinkOrig = BitmapFactory.decodeResource(
        res, R.drawable.bubble_blink, options);
      mGameWonOrig = BitmapFactory.decodeResource(
        res, R.drawable.win_panel, options);
      mGameLostOrig = BitmapFactory.decodeResource(
        res, R.drawable.lose_panel, options);
      mGamePausedOrig = BitmapFactory.decodeResource(
        res, R.drawable.pause_panel, options);
      mHurryOrig = BitmapFactory.decodeResource(
        res, R.drawable.hurry, options);
      mPauseButtonOrig = BitmapFactory.decodeResource(
        res, R.drawable.pause_button, options);
      mPlayButtonOrig = BitmapFactory.decodeResource(
        res, R.drawable.play_button, options);
      mPenguinsOrig = BitmapFactory.decodeResource(
        res, R.drawable.penguins, options);
      mPenguins2Orig = BitmapFactory.decodeResource(
        res, R.drawable.penguins2, options);
      mCompressorHeadOrig = BitmapFactory.decodeResource(
        res, R.drawable.compressor, options);
      mCompressorOrig = BitmapFactory.decodeResource(
        res, R.drawable.compressor_body, options);
      mLifeOrig = BitmapFactory.decodeResource(
        res, R.drawable.life, options);
      mFontImageOrig = BitmapFactory.decodeResource(
        res, R.drawable.bubble_font, options);
      mBananaOrig = BitmapFactory.decodeResource(
        res, R.drawable.banana, options);
      mTomatoOrig = BitmapFactory.decodeResource(
        res, R.drawable.tomato, options);

      mImageList = new Vector<BmpWrap>();

      mBubbles = new BmpWrap[8];
      for (int i = 0; i < mBubbles.length; i++) {
        mBubbles[i] = NewBmpWrap();
      }

      mBubblesBlind = new BmpWrap[8];
      for (int i = 0; i < mBubblesBlind.length; i++) {
        mBubblesBlind[i] = NewBmpWrap();
      }

      mFrozenBubbles = new BmpWrap[8];
      for (int i = 0; i < mFrozenBubbles.length; i++) {
        mFrozenBubbles[i] = NewBmpWrap();
      }

      mTargetedBubbles = new BmpWrap[6];
      for (int i = 0; i < mTargetedBubbles.length; i++) {
        mTargetedBubbles[i] = NewBmpWrap();
      }

      mBackground     = NewBmpWrap();
      mBubbleBlink    = NewBmpWrap();
      mGameWon        = NewBmpWrap();
      mGameLost       = NewBmpWrap();
      mGamePaused     = NewBmpWrap();
      mHurry          = NewBmpWrap();
      mPauseButton    = NewBmpWrap();
      mPlayButton     = NewBmpWrap();
      mPenguins       = NewBmpWrap();
      mPenguins2      = NewBmpWrap();
      mCompressorHead = NewBmpWrap();
      mCompressor     = NewBmpWrap();
      mLife           = NewBmpWrap();
      mFontImage      = NewBmpWrap();
      mBanana         = NewBmpWrap();
      mTomato         = NewBmpWrap();

      mFont             = new BubbleFont(mFontImage);
      mLauncher         = res.getDrawable(R.drawable.launcher);
      mSoundManager     = new SoundManager(mContext);

      /*
       * Only keep a high score database when the opponent is the CPU.
       */
      if (mRemoteInput.isCPU) {
        mHighScoreManager = new HighscoreManager(getContext(),
                                                 HighscoreManager.
                                                 MULTIPLAYER_DATABASE_NAME);
      }
      else {
        mHighScoreManager = null;
      }

      mLevelManager = new LevelManager(0, FrozenBubble.getDifficulty());
      newGame();
    }

    public void newGame() {
      synchronized (mSurfaceHolder) {
        malusBar1 = new MalusBar(MultiplayerGameView.GAMEFIELD_WIDTH - 164, 40,
                                 mBanana, mTomato);
        malusBar2 = new MalusBar(MultiplayerGameView.GAMEFIELD_WIDTH + 134, 40,
                                 mBanana, mTomato);
        mFrozenGame1 = new FrozenGame(mBackground, mBubbles, mBubblesBlind,
                                      mFrozenBubbles, mTargetedBubbles,
                                      mBubbleBlink, mGameWon, mGameLost,
                                      mGamePaused, mHurry,
                                      mPauseButton, mPlayButton, mPenguins,
                                      mCompressorHead, mCompressor,
                                      malusBar2, mLauncher,
                                      mSoundManager, mLevelManager,
                                      mHighScoreManager, mNetworkManager,
                                      mPlayer1);
        mPlayer1.setGameRef(mFrozenGame1);
        mFrozenGame2 = new FrozenGame(mBackground, mBubbles, mBubblesBlind,
                                      mFrozenBubbles, mTargetedBubbles,
                                      mBubbleBlink, mGameWon, mGameLost,
                                      mGamePaused, mHurry,
                                      null, null, mPenguins2,
                                      mCompressorHead, mCompressor,
                                      malusBar1, mLauncher,
                                      mSoundManager, mLevelManager,
                                      null, mNetworkManager,
                                      mPlayer2);
        mPlayer2.setGameRef(mFrozenGame2);
        if (mHighScoreManager != null) {
          mHighScoreManager.startLevel(mLevelManager.getLevelIndex());
        }
        if (mNetworkManager != null) {
          mNetworkManager.newGame();
          mShowNetwork = true;
        }
      }
    }

    public void pause() {
      synchronized (mSurfaceHolder) {
        if (mMode == stateEnum.RUNNING) {
          setState(stateEnum.PAUSED);

          if (mGameListener != null)
            mGameListener.onGameEvent(eventEnum.GAME_PAUSED);
          if (mFrozenGame1 != null)
            mFrozenGame1.pause();
          if (mFrozenGame2 != null)
            mFrozenGame2.pause();
          if (mHighScoreManager != null)
            mHighScoreManager.pauseLevel();
        }
      }
    }

    public void pauseButtonPressed(boolean pauseKeyPressed) {
      if (mFrozenGame1 != null)
        mFrozenGame1.pauseButtonPressed(pauseKeyPressed);
    }

    private void resizeBitmaps() {
      //Log.i("frozen-bubble", "resizeBitmaps()");
      scaleFrom(mBackground, mBackgroundOrig);
      for (int i = 0; i < mBubblesOrig.length; i++) {
        scaleFrom(mBubbles[i], mBubblesOrig[i]);
      }
      for (int i = 0; i < mBubblesBlind.length; i++) {
        scaleFrom(mBubblesBlind[i], mBubblesBlindOrig[i]);
      }
      for (int i = 0; i < mFrozenBubbles.length; i++) {
        scaleFrom(mFrozenBubbles[i], mFrozenBubblesOrig[i]);
      }
      for (int i = 0; i < mTargetedBubbles.length; i++) {
        scaleFrom(mTargetedBubbles[i], mTargetedBubblesOrig[i]);
      }
      scaleFrom(mBubbleBlink, mBubbleBlinkOrig);
      scaleFrom(mGameWon, mGameWonOrig);
      scaleFrom(mGameLost, mGameLostOrig);
      scaleFrom(mGamePaused, mGamePausedOrig);
      scaleFrom(mHurry, mHurryOrig);
      scaleFrom(mPauseButton, mPauseButtonOrig);
      scaleFrom(mPlayButton, mPlayButtonOrig);
      scaleFrom(mPenguins, mPenguinsOrig);
      scaleFrom(mPenguins2, mPenguins2Orig);
      scaleFrom(mCompressorHead, mCompressorHeadOrig);
      scaleFrom(mCompressor, mCompressorOrig);
      scaleFrom(mLife, mLifeOrig);
      scaleFrom(mFontImage, mFontImageOrig);
      scaleFrom(mBanana, mBananaOrig);
      scaleFrom(mTomato, mTomatoOrig);
      //Log.i("frozen-bubble", "resizeBitmaps done.");
      mImagesReady = true;
    }

    /**
     * Restores game state from the indicated Bundle. Typically called
     * when the Activity is being restored after having been previously
     * destroyed.
     * @param savedState - Bundle containing the game state.
     */
    public synchronized void restoreState(Bundle map) {
      synchronized (mSurfaceHolder) {
        setState(stateEnum.PAUSED);
        numPlayer1GamesWon = map.getInt("numPlayer1GamesWon", 0);
        numPlayer2GamesWon = map.getInt("numPlayer2GamesWon", 0);
        mFrozenGame1     .restoreState(map, mImageList);
        mFrozenGame2     .restoreState(map, mImageList);
        mLevelManager    .restoreState(map);
        if (mHighScoreManager != null) {
          mHighScoreManager.restoreState(map);
        }
      }
    }

    public void resumeGame() {
      synchronized (mSurfaceHolder) {
        if (mMode == stateEnum.RUNNING) {
          mFrozenGame1     .resume();
          mFrozenGame2     .resume();
          if (mHighScoreManager != null) {
            mHighScoreManager.resumeLevel();
          }
        }
      }
    }

    @Override
    public void run() {
      while (mRun) {
        long now = System.currentTimeMillis();
        long delay = FRAME_DELAY + mLastTime - now;
        if (delay > 0) {
          try {
            sleep(delay);
          } catch (InterruptedException e) {}
        }
        mLastTime = now;
        Canvas c = null;
        try {
          if (surfaceOK()) {
            c = mSurfaceHolder.lockCanvas(null);
            if (c != null) {
              synchronized (mSurfaceHolder) {
                if (mRun) {
                  monitorRemotePlayer();
                  if (mMode == stateEnum.ABOUT) {
                    drawAboutScreen(c);
                  }
                  else if (mMode == stateEnum.PAUSED) {
                    if (mNetworkManager != null) {
                      if (mShowNetwork)
                        drawNetworkScreen(c);
                      else
                        doDraw(c);
                    }
                    else if ((mHighScoreManager != null) && mShowScores)
                      drawHighScoreScreen(c, mHighScoreManager.getLevel());
                    else
                      doDraw(c);
                  }
                  else {
                    if (mMode == stateEnum.RUNNING) {
                      if (mModeWas != stateEnum.RUNNING)  {
                        if (mGameListener != null)
                          mGameListener.onGameEvent(eventEnum.GAME_RESUME);

                        mModeWas = stateEnum.RUNNING;
                        resumeGame();
                      }
                      updateGameState();
                    }
                    doDraw(c);
                  }
                }
              }
            }
          }
        } finally {
          /*
           * Do this in a finally so that if an exception is thrown
           * during the above, we don't leave the Surface in an
           * inconsistent state.
           */
          if (c != null)
            mSurfaceHolder.unlockCanvasAndPost(c);
        }
      }
    }

    /**
     * Dump game state to the provided Bundle. Typically called when the
     * Activity is being suspended.
     * @return Bundle with this view's state
     */
    public Bundle saveState(Bundle map) {
      synchronized (mSurfaceHolder) {
        if (map != null) {
          map.putInt("numPlayers", 2);
          map.putInt("numPlayer1GamesWon", numPlayer1GamesWon);
          map.putInt("numPlayer2GamesWon", numPlayer2GamesWon);
          mFrozenGame1     .saveState(map);
          mFrozenGame2     .saveState(map);
          mLevelManager    .saveState(map);
          if (mHighScoreManager != null) {
            mHighScoreManager.saveState(map);
          }
        }
      }
      return map;
    }

    private void scaleFrom(BmpWrap image, Bitmap bmp) {
      if ((image.bmp != null) && (image.bmp != bmp)) {
        image.bmp.recycle();
      }

      if ((mDisplayScale > 0.99999) && (mDisplayScale < 1.00001)) {
        image.bmp = bmp;
        return;
      }

      int dstWidth  = (int)(bmp.getWidth()  * mDisplayScale);
      int dstHeight = (int)(bmp.getHeight() * mDisplayScale);
      image.bmp = Bitmap.createScaledBitmap(bmp, dstWidth, dstHeight, true);
    }

    public void setPosition(double value) {
      mLocalInput.mGameRef.setPosition(value);
    }

    public void setRunning(boolean b) {
      mRun = b;
    }

    public void setState(stateEnum newMode) {
      synchronized (mSurfaceHolder) {
        /*
         * Only update the previous mode storage if the new mode is
         * different from the current mode, in case the same mode is
         * being set multiple times.
         *
         * The transition from state to state must be preserved in case
         * a separate execution thread that checks for state transitions
         * does not get a chance to run between calls to this method.
         */
        if (newMode != mMode)
          mModeWas = mMode;

        mMode = newMode;
      }
    }

    public void setSurfaceOK(boolean ok) {
      synchronized (mSurfaceHolder) {
        mSurfaceOK = ok;
      }
    }

    public void setSurfaceSize(int width, int height) {
      float newHeight    = height;
      float newWidth     = width;
      float gameHeight   = GAMEFIELD_HEIGHT;
      float gameWidth    = GAMEFIELD_WIDTH;
      float extGameWidth = EXTENDED_GAMEFIELD_WIDTH;
      synchronized (mSurfaceHolder) {
        if ((newWidth / newHeight) >= (gameWidth / gameHeight)) {
          mDisplayScale = (1.0 * newHeight) / gameHeight;
          mDisplayDX = (int)((newWidth - (mDisplayScale * extGameWidth)) / 2);
          mDisplayDY = 0;
        }
        else {
          mDisplayScale = (1.0 * newWidth) / gameWidth;
          /*
           * When rotate to shoot targeting mode is selected during a
           * multiplayer game, then the screen orientation is forced to
           * landscape.
           *
           * In portrait mode during a multiplayer game, display just
           * one game field.  Depending on which portrait mode it is,
           * display player one or player two.  For normal portrait
           * orientation, show player one, and for reverse portrait,
           * show player two.
           */
          if (FrozenBubble.getTargetMode() == FrozenBubble.ROTATE_TO_SHOOT) {
            mDisplayDX = 0;
          }
          else {
            int orientation = getScreenOrientation();
            if (orientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT)
              mDisplayDX = (int)(-mDisplayScale * gameWidth);
            else
              mDisplayDX = 0;
          }
          mDisplayDY = (int)((newHeight - (mDisplayScale * gameHeight)) / 2);
        }
        mPlayer1DX = (int) (mDisplayDX - (mDisplayScale * ( gameWidth / 2 )));
        mPlayer2DX = (int) (mDisplayDX + (mDisplayScale * ( gameWidth / 2 )));
        resizeBitmaps();
      }
    }

    /**
     * Create a CPU opponent object (if necessary) and start the thread.
     */
    public void startOpponent() {
      if (mOpponent != null) {
        mOpponent.stopThread();
        mOpponent = null;
      }
      if (mRemoteInput.isCPU) {
        if (mRemoteInput.playerID == VirtualInput.PLAYER2)
          mOpponent = new ComputerAI(mFrozenGame2, mRemoteInput);
        else
          mOpponent = new ComputerAI(mFrozenGame1, mRemoteInput);
        mOpponent.start();
      }
    }

    public boolean surfaceOK() {
      synchronized (mSurfaceHolder) {
        return mSurfaceOK;
      }
    }

    /**
     * Process function key presses.  Function keys toggle features on
     * and off (e.g., game paused on/off, sound on/off, etc.).
     * @param keyCode - the key code to process.
     * @param updateNow - if true, apply state changes.
     * @param transmit - if true and this is a network game, send the
     * key code over the network.
     */
    public void toggleKeyPress(int keyCode,
                               boolean updateNow,
                               boolean transmit) {
      if (keyCode == KeyEvent.KEYCODE_M)
        muteKeyToggle = !muteKeyToggle;
      else if (keyCode == KeyEvent.KEYCODE_P) {
        if (transmit && (mNetworkManager != null)) {
          mNetworkManager.sendLocalPlayerAction(mLocalInput.playerID,
              false, false, false, keyCode, -1, -1, -1, -1, null,
              mLocalInput.mGameRef.launchBubblePosition);
        }
        pauseKeyToggle = !pauseKeyToggle;
        mGameThread.pauseButtonPressed(pauseKeyToggle);
        if (updateNow) {
          updateStateOnEvent(null);
          /*
           * If the game is running and the pause button was pressed,
           * pause the game.
           */
          if (pauseKeyToggle && (mMode == stateEnum.RUNNING))
            pause();
        }
      }
    }

    /**
     * Obtain the current state of a feature toggle key.
     * @param keyCode
     * @return The state of the desired feature toggle key flag.
     */
    public boolean toggleKeyState(int keyCode) {
      if (keyCode == KeyEvent.KEYCODE_M)
        return muteKeyToggle;
      else if (keyCode == KeyEvent.KEYCODE_P)
        return pauseKeyToggle;

      return false;
    }

    /**
     * updateStateOnEvent() - a common method to process motion events
     * to set the game state.  When the motion event has been fully
     * processed, this function will return true, otherwise if the
     * calling method should also process the motion event, this
     * function will return false.
     * @param event - the MotionEvent to process for the purpose of
     * updating the game state.  If this parameter is null, then the
     * game state is forced to update if applicable based on the current
     * game state.
     * @return This function returns <code>true</code> to inform the
     * calling function that the game state has been updated and that no
     * further processing is necessary, and <code>false</code> to
     * indicate that the caller should continue processing the motion
     * event.
     */
    private boolean updateStateOnEvent(MotionEvent event) {
      boolean event_action_down = false;

      if (event == null)
        event_action_down = true;
      else if (event.getAction() == MotionEvent.ACTION_DOWN)
        event_action_down = true;

      if (event_action_down) {
        switch (mMode) {
          case ABOUT:
            setState(stateEnum.RUNNING);
            return true;

          case PAUSED:
            if (mNetworkManager != null) {
              if (mShowNetwork) {
                if (mNetworkManager.gameIsReadyForAction()) {
                  mShowNetwork = false;
                  setState(stateEnum.RUNNING);
                  if (mGameListener != null) {
                    mGameListener.onGameEvent(eventEnum.LEVEL_START);
                  }
                }
                return true;
              }
            }
            else if (mShowScores) {
              mShowScores = false;
              setState(stateEnum.RUNNING);
              if (mGameListener != null) {
                mGameListener.onGameEvent(eventEnum.LEVEL_START);
              }
              return true;
            }
            setState(stateEnum.RUNNING);
            break;

          case RUNNING:
          default:
            break;
        }
      }
      return false;
    }

    private void updateGameState() {
      if ((mFrozenGame1 == null) || (mFrozenGame2 == null) ||
          ((mOpponent == null) && mRemoteInput.isCPU)) {
        return;
      }

      gameEnum game1State = mFrozenGame1.play(mPlayer1.actionLeft(),
                                              mPlayer1.actionRight(),
                                              mPlayer1.actionUp(),
                                              mPlayer1.actionDown(),
                                              mPlayer1.getTrackBallDx(),
                                              mPlayer1.actionTouchFire(),
                                              mPlayer1.getTouchX(),
                                              mPlayer1.getTouchY(),
                                              mPlayer1.actionTouchFireATS(),
                                              mPlayer1.getTouchDxATS());

      gameEnum game2State = mFrozenGame2.play(mPlayer2.actionLeft(),
                                              mPlayer2.actionRight(),
                                              mPlayer2.actionUp(),
                                              mPlayer2.actionDown(),
                                              mPlayer2.getTrackBallDx(),
                                              mPlayer2.actionTouchFire(),
                                              mPlayer2.getTouchX(),
                                              mPlayer2.getTouchY(),
                                              mPlayer2.actionTouchFireATS(),
                                              mPlayer2.getTouchDxATS());

      /*
       * If playing a network game, update the bubble grid checksums.
       */
      if (mNetworkManager != null) {
        mNetworkManager.setLocalChecksum(mLocalInput.mGameRef.gridChecksum);
        mNetworkManager.setRemoteChecksum(mRemoteInput.mGameRef.gridChecksum);
      }

      /*
       * If playing a CPU opponent, notify the computer that the current
       * action has been processed and we are ready for a new action.
       */
      if (mOpponent != null) {
        mOpponent.clearAction();
      }

      /*
       * Obtain the number of attack bubbles to add to each player's
       * attack bar that are being sent by their respective opponents.
       */
      malusBar1.addBubbles(mFrozenGame1.getSendToOpponent());
      malusBar2.addBubbles(mFrozenGame2.getSendToOpponent());

      gameEnum game1Result = mFrozenGame1.getGameResult();
      gameEnum game2Result = mFrozenGame2.getGameResult();

      /*
       * When one player wins or loses, the other player is
       * automatically designated the loser or winner, respectively.
       */
      if (game1Result != gameEnum.PLAYING) {
        if ((game1Result == gameEnum.WON) ||
            (game1Result == gameEnum.NEXT_WON)) {
          mFrozenGame2.setGameResult(gameEnum.LOST);
        }
        else {
          mFrozenGame2.setGameResult(gameEnum.WON);
        }
      }
      else if (game2Result != gameEnum.PLAYING) {
        if ((game2Result == gameEnum.WON) ||
            (game2Result == gameEnum.NEXT_WON)) {
          if (mHighScoreManager != null) {
            mHighScoreManager.lostLevel();
          }
          mFrozenGame1.setGameResult(gameEnum.LOST);
        }
        else {
          if (mHighScoreManager != null) {
            mHighScoreManager.endLevel(mFrozenGame1.nbBubbles);
          }
          mFrozenGame1.setGameResult(gameEnum.WON);
        }
      }

      /*
       * Only start a new game when player 1 provides input when the
       * opponent is the CPU, because the CPU is prone to sneaking a
       * launch attempt in after the game is decided.
       *
       * Otherwise, the first player to provide input initiates the
       * new game.
       */
      if (((game1State == gameEnum.NEXT_LOST) ||
          (game1State == gameEnum.NEXT_WON)) ||
          (!mRemoteInput.isCPU &&
           ((game2State == gameEnum.NEXT_LOST) ||
            (game2State == gameEnum.NEXT_WON)))){
        if ((game1State == gameEnum.NEXT_WON) ||
            (game2State == gameEnum.NEXT_LOST)) {
          numPlayer1GamesWon++;
        }
        else {
          numPlayer2GamesWon++;
        }

        if (mNetworkManager == null) {
          mShowScores = true;
        }

        pause();
        newGame();

        if (mRemoteInput.isCPU) {
          startOpponent();
        }
      }
    }

    /**
     * Use the player 1 offset to calculate the horizontal offset to
     * apply a raw horizontal position to the playfield.
     * @param x - the raw horizontal position.
     * @return The adjusted horizontal position.
     */
    private double xFromScr(float x) {
      return (x - mPlayer1DX) / mDisplayScale;
    }

    private double yFromScr(float y) {
      return (y - mDisplayDY) / mDisplayScale;
    }
  }

  /**
   * <code>MultiplayerGameView</code> class constructor.
   * @param context - the application context.
   * @param attrs - the compiled XML attributes for the superclass.
   */
  public MultiplayerGameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    //Log.i("frozen-bubble", "MultiplayerGameView constructor");
    init(context, (int) VirtualInput.PLAYER1, FrozenBubble.LOCALE_LOCAL);
  }

  /**
   * <code>MultiplayerGameView</code> class constructor.
   * @param context - the application context.
   * @param myPlayerId - the local player ID (1 or 2).
   * @param gameLocale - the game topology, which can be either local,
   * or distributed over various network types.
   */
  public MultiplayerGameView(Context context, int myPlayerId, int gameLocale) {
    super(context);
    //Log.i("frozen-bubble", "MultiplayerGameView constructor");
    init(context, myPlayerId, gameLocale);
  }

  /**
   * <code>MultiplayerGameView</code> object initialization.
   * @param context - the application context.
   * @param myPlayerId - the local player ID (1 or 2).
   * @param gameLocale - the game topology, which can be either local,
   * or distributed over various network types.
   */
  private void init(Context context, int myPlayerId, int gameLocale) {
    mContext = context;
    SurfaceHolder holder = getHolder();
    holder.addCallback(this);
    mOpponent = null;

    numPlayer1GamesWon = 0;
    numPlayer2GamesWon = 0;

    /*
     * If this game is being played purely locally, then the opponent is
     * CPU controlled.  Otherwise the opponent is a remote player.
     *
     * TODO: add the ability to support multiple local players via
     * multi-touch, and the ability to specify any player as CPU
     * controlled.
     */
    boolean isCPU;
    boolean isRemote;

    if (gameLocale == FrozenBubble.LOCALE_LOCAL) {
      isCPU    = true;
      isRemote = false;
    }
    else {
      isCPU    = false;
      isRemote = true;
    }

    if (myPlayerId == VirtualInput.PLAYER1) {
      mPlayer1 = new PlayerInput(VirtualInput.PLAYER1, false, false);
      mPlayer2 = new PlayerInput(VirtualInput.PLAYER2, isCPU, isRemote);
      mLocalInput = mPlayer1;
      mRemoteInput = mPlayer2;
    }
    else {
      mPlayer1 = new PlayerInput(VirtualInput.PLAYER1, isCPU, isRemote);
      mPlayer2 = new PlayerInput(VirtualInput.PLAYER2, false, false);
      mLocalInput = mPlayer2;
      mRemoteInput = mPlayer1;
    }

    /*
     * Create a network game manager if this is a network game.
     */
    mNetworkManager = null;
    if ((gameLocale == FrozenBubble.LOCALE_LAN) ||
        (gameLocale == FrozenBubble.LOCALE_INTERNET)) {
      connectEnum connectType;
      if (gameLocale == FrozenBubble.LOCALE_LAN) {
        connectType = connectEnum.UDP_MULTICAST;
      }
      else {
        connectType = connectEnum.UDP_UNICAST;
      }
      mNetworkManager = new NetworkGameManager(context,
                                               connectType,
                                               mLocalInput,
                                               mRemoteInput);
      remoteInterface = mNetworkManager.getRemoteInterface();
    }

    /*
     * Give this view focus-ability for improved compatibility with
     * various input devices.
     */
    setFocusable(true);
    setFocusableInTouchMode(true);

    /*
     * Create and start the game thread.
     */
    mGameThread = new MultiplayerGameThread(holder);
    mGameThread.setRunning(true);
    mGameThread.start();
  }

  public MultiplayerGameThread getThread() {
    return mGameThread;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent msg) {
    //Log.i("frozen-bubble", "MultiplayerGameView.onKeyDown()");
    return mGameThread.doKeyDown(keyCode, msg);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent msg) {
    //Log.i("frozen-bubble", "MultiplayerGameView.onKeyUp()");
    return mGameThread.doKeyUp(keyCode, msg);
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    //Log.i("frozen-bubble", "event.getX(): " + event.getX());
    //Log.i("frozen-bubble", "event.getY(): " + event.getY());
    return mGameThread.doTrackballEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return mGameThread.doTouchEvent(event);
  }

  @Override
  public void onWindowFocusChanged(boolean hasWindowFocus) {
    //Log.i("frozen-bubble", "MultiplayerGameView.onWindowFocusChanged()");
    if (!hasWindowFocus) {
      if (mNetworkManager != null) {
        mNetworkManager.pause();
      }
      if (mGameThread != null)
        mGameThread.pause();
    }
    else {
      if (mNetworkManager != null) {
        mNetworkManager.unPause();
      }
    }
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width,
                             int height) {
    //Log.i("frozen-bubble", "MultiplayerGameView.surfaceChanged");
    mGameThread.setSurfaceSize(width, height);
  }

  public void surfaceCreated(SurfaceHolder holder) {
    //Log.i("frozen-bubble", "MultiplayerGameView.surfaceCreated()");
    mGameThread.setSurfaceOK(true);
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    //Log.i("frozen-bubble", "MultiplayerGameView.surfaceDestroyed()");
    mGameThread.setSurfaceOK(false);
  }

  public void cleanUp() {
    //Log.i("frozen-bubble", "MultiplayerGameView.cleanUp()");
    cleanUpNetworkManager();

    mPlayer1.init();
    mPlayer2.init();

    if (mOpponent != null)
      mOpponent.stopThread();
    mOpponent = null;

    mGameThread.cleanUp();
  }

  private void cleanUpNetworkManager() {
    if (mNetworkManager != null)
      mNetworkManager.cleanUp();
    mNetworkManager = null;
  }
}
