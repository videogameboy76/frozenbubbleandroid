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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.jfedor.frozenbubble.GameScreen.eventEnum;
import org.jfedor.frozenbubble.GameScreen.gameEnum;
import org.jfedor.frozenbubble.GameScreen.stateEnum;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.efortin.frozenbubble.HighscoreDO;
import com.efortin.frozenbubble.HighscoreManager;

class GameView extends SurfaceView implements SurfaceHolder.Callback {

  public static final int GAMEFIELD_WIDTH          = 320;
  public static final int GAMEFIELD_HEIGHT         = 480;
  public static final int EXTENDED_GAMEFIELD_WIDTH = 640;

  private boolean    mInterstitialShown = false;
  private boolean    mBlankScreen = false;
  private Context    mContext;
  private GameThread mGameThread;

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

  public void setGameListener (GameListener gl) {
    mGameListener = gl;
  }

  class GameThread extends Thread {

    private static final int FRAME_DELAY = 40;

    private static final double TRACKBALL_COEFFICIENT      = 5;
    private static final double TOUCH_FIRE_Y_THRESHOLD     = 380;
    private static final double TOUCH_SWAP_X_THRESHOLD     = 14;
    private static final double ATS_TOUCH_COEFFICIENT      = 0.2;
    private static final double ATS_TOUCH_FIRE_Y_THRESHOLD = 350;

    private boolean mImagesReady  = false;
    private boolean mRun          = false;
    private boolean mShowScores   = false;
    private boolean mSurfaceOK    = false;
    private boolean mLeft         = false;
    private boolean mRight        = false;
    private boolean mUp           = false;
    private boolean mDown         = false;
    private boolean mFire         = false;
    private boolean mWasLeft      = false;
    private boolean mWasRight     = false;
    private boolean mWasFire      = false;
    private boolean mWasUp        = false;
    private boolean mWasDown      = false;
    private double  mTrackballDX  = 0;
    private boolean mTouchFire    = false;
    private boolean mTouchSwap    = false;
    private double  mTouchX;
    private double  mTouchY;
    private boolean mATSTouchFire = false;
    private double  mATSTouchDX   = 0;
    private double  mATSTouchLastX;

    private int    mDisplayDX;
    private int    mDisplayDY;
    private double mDisplayScale;
    private long   mLastTime;

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
    private Bitmap mPenguinsOrig;
    private Bitmap mCompressorHeadOrig;
    private Bitmap mCompressorOrig;
    private Bitmap mLifeOrig;
    private Bitmap mFontImageOrig;
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
    private BmpWrap mPenguins;
    private BmpWrap mCompressorHead;
    private BmpWrap mCompressor;
    private BmpWrap mLife;
    private BmpWrap mFontImage;

    private BubbleFont    mFont;
    private Drawable      mLauncher;  // drawable because we rotate it
    private FrozenGame    mFrozenGame;
    private LevelManager  mLevelManager;
    private SoundManager  mSoundManager;
    private SurfaceHolder mSurfaceHolder;

    private final HighscoreManager mHighscoreManager;

    Vector<BmpWrap> mImageList;

    public int getCurrentLevelIndex() {
      synchronized (mSurfaceHolder) {
        return mLevelManager.getLevelIndex();
      }
    }

    private BmpWrap NewBmpWrap() {
      int new_img_id = mImageList.size();
      BmpWrap new_img = new BmpWrap(new_img_id);
      mImageList.addElement(new_img);
      return new_img;
    }

    public GameThread(SurfaceHolder surfaceHolder, byte[] customLevels,
                      int startingLevel) {
      //Log.i("frozen-bubble", "GameThread()");
      mSurfaceHolder = surfaceHolder;
      Resources res = mContext.getResources();
      setState(stateEnum.PAUSED);

      BitmapFactory.Options options = new BitmapFactory.Options();

      // The Options.inScaled field is only available starting at API 4.
      try {
        Field f = options.getClass().getField("inScaled");
        f.set(options, Boolean.FALSE);
      } catch (Exception ignore) {}

      mBackgroundOrig = BitmapFactory.decodeResource(
        res, R.drawable.background, options);
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
      mPenguinsOrig = BitmapFactory.decodeResource(
        res, R.drawable.penguins, options);
      mCompressorHeadOrig = BitmapFactory.decodeResource(
        res, R.drawable.compressor, options);
      mCompressorOrig = BitmapFactory.decodeResource(
        res, R.drawable.compressor_body, options);
      mLifeOrig = BitmapFactory.decodeResource(
        res, R.drawable.life, options);
      mFontImageOrig = BitmapFactory.decodeResource(
        res, R.drawable.bubble_font, options);

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
      mPenguins       = NewBmpWrap();
      mCompressorHead = NewBmpWrap();
      mCompressor     = NewBmpWrap();
      mLife           = NewBmpWrap();
      mFontImage      = NewBmpWrap();

      mFont             = new BubbleFont(mFontImage);
      mLauncher         = res.getDrawable(R.drawable.launcher);
      mSoundManager     = new SoundManager(mContext);
      mHighscoreManager = new HighscoreManager(getContext(),
                                               HighscoreManager.
                                               PUZZLE_DATABASE_NAME);

      if (null == customLevels) {
        try {
          InputStream is     = mContext.getAssets().open("levels.txt");
          int         size   = is.available();
          byte[]      levels = new byte[size];
          is.read(levels);
          is.close();
          SharedPreferences sp = mContext.getSharedPreferences(
            FrozenBubble.PREFS_NAME, Context.MODE_PRIVATE);
          startingLevel = sp.getInt("level", 0);
          mLevelManager = new LevelManager(levels, startingLevel);
        } catch (IOException e) {
          // Should never happen.
          throw new RuntimeException(e);
        }
      }
      else {
        // We were launched by the level editor.
        mLevelManager = new LevelManager(customLevels, startingLevel);
      }

      mFrozenGame = new FrozenGame(mBackground, mBubbles, mBubblesBlind,
                                   mFrozenBubbles, mTargetedBubbles,
                                   mBubbleBlink, mGameWon, mGameLost,
                                   mGamePaused, mHurry, mPenguins,
                                   mCompressorHead, mCompressor, mLauncher,
                                   mSoundManager, mLevelManager,
                                   mHighscoreManager);
      mHighscoreManager.startLevel(mLevelManager.getLevelIndex());
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
      scaleFrom(mPenguins, mPenguinsOrig);
      scaleFrom(mCompressorHead, mCompressorHeadOrig);
      scaleFrom(mCompressor, mCompressorOrig);
      scaleFrom(mLife, mLifeOrig);
      scaleFrom(mFontImage, mFontImageOrig);
      //Log.i("frozen-bubble", "resizeBitmaps done.");
      mImagesReady = true;
    }

    public void pause() {
      synchronized (mSurfaceHolder) {
        if (mMode == stateEnum.RUNNING) {
          setState(stateEnum.PAUSED);

          if (mGameListener != null)
            mGameListener.onGameEvent(eventEnum.GAME_PAUSED);
          if (mFrozenGame != null)
            mFrozenGame.pause();
          if (mHighscoreManager != null)
            mHighscoreManager.pauseLevel();
        }
      }
    }

    public void resumeGame() {
      synchronized (mSurfaceHolder) {
        if (mMode == stateEnum.RUNNING) {
          mFrozenGame      .resume();
          mHighscoreManager.resumeLevel();
        }
      }
    }

    public void newGame() {
      synchronized (mSurfaceHolder) {
        mLevelManager.goToFirstLevel();
        mFrozenGame = new FrozenGame(mBackground, mBubbles, mBubblesBlind,
                                     mFrozenBubbles, mTargetedBubbles,
                                     mBubbleBlink, mGameWon, mGameLost,
                                     mGamePaused, mHurry, mPenguins,
                                     mCompressorHead, mCompressor, mLauncher,
                                     mSoundManager, mLevelManager,
                                     mHighscoreManager);
        mHighscoreManager.startLevel(mLevelManager.getLevelIndex());
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
                  if (mMode == stateEnum.ABOUT) {
                    drawAboutScreen(c);
                  }
                  else if (mMode == stateEnum.PAUSED) {
                    if (mShowScores)
                      drawHighScoreScreen(c, mHighscoreManager.getLevel());
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
          // do this in a finally so that if an exception is thrown
          // during the above, we don't leave the Surface in an
          // inconsistent state
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
          map.putInt("numPlayers", 1);
          mFrozenGame      .saveState(map);
          mLevelManager    .saveState(map);
          mHighscoreManager.saveState(map);
        }
      }
      return map;
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
        mFrozenGame      .restoreState(map, mImageList);
        mLevelManager    .restoreState(map);
        mHighscoreManager.restoreState(map);
      }
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
         * The transition from state to state must be preserved in
         * case a separate execution thread that checks for state
         * transitions does not get a chance to run between calls to
         * this method.
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

    public boolean surfaceOK() {
      synchronized (mSurfaceHolder) {
        return mSurfaceOK;
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
          mDisplayDX = (int)(-mDisplayScale * (extGameWidth - gameWidth) / 2);
          mDisplayDY = (int)((newHeight - (mDisplayScale * gameHeight)) / 2);
        }
        resizeBitmaps();
      }
    }

    /**
     * Process key presses.  This must be allowed to run regardless of
     * the game state to correctly handle initial game conditions.
     * @param keyCode - the static KeyEvent key identifier.
     * @param msg - the key action message.
     * @return <code>true</code> if the key action is processed.
     * @see android.view.View#onKeyDown(int, android.view.KeyEvent)
     */
    boolean doKeyDown(int keyCode, KeyEvent msg) {
      synchronized (mSurfaceHolder) {
        /*
         * Only update the game state if this is a fresh key press.
         */
        if ((!mLeft && !mRight && !mFire && !mUp && !mDown) &&
            ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ||
             (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) ||
             (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) ||
             (keyCode == KeyEvent.KEYCODE_DPAD_UP) ||
             (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)))
          updateStateOnEvent(null);

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
          mFire    = true;
          mWasFire = true;
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
    }

    /**
     * Process key releases.  This must be allowed to run regardless of
     * the game state in order to properly clear key presses.
     * @param keyCode - the static KeyEvent key identifier.
     * @param msg - the key action message.
     * @return <code>true</code> if the key action is processed.
     * @see android.view.View#onKeyUp(int, android.view.KeyEvent)
     */
    boolean doKeyUp(int keyCode, KeyEvent msg) {
      synchronized (mSurfaceHolder) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
          mLeft = false;
          return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
          mRight = false;
          return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
          mFire = false;
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
    }

    /**
     * Process trackball motion events.
     * <p>This method only processes trackball motion for the purpose of
     * aiming the launcher.  The trackball has no effect on the game
     * state, much like moving a mouse cursor over a screen does not
     * perform any intrinsic actions in most applications.
     * @param event - the motion event associated with the trackball.
     * @return This function returns true if the trackball motion was
     * processed, which notifies the caller that this method handled the
     * motion event and no other handling is necessary.
     */
    boolean doTrackballEvent(MotionEvent event) {
      synchronized (mSurfaceHolder) {
        if (mMode == stateEnum.RUNNING) {
          if (event.getAction() == MotionEvent.ACTION_MOVE) {
            mTrackballDX += event.getX() * TRACKBALL_COEFFICIENT;
            return true;
          }
        }
        return false;
      }
    }

    private double xFromScr(float x) {
      return (x - mDisplayDX) / mDisplayScale;
    }

    private double yFromScr(float y) {
      return (y - mDisplayDY) / mDisplayScale;
    }

    /**
     * This method to handles touch screen motion events.
     * <p>This method will be called three times in succession for each
     * touch, to process <code>ACTION_DOWN</code>,
     * <code>ACTION_UP</code>, and <code>ACTION_MOVE</code>.
     * @param event - the motion event.
     * @return <code>true</code> if the event was handled.
     */
    boolean doTouchEvent(MotionEvent event) {
      synchronized (mSurfaceHolder) {
        if(updateStateOnEvent(event))
          return true;

        if (mMode == stateEnum.RUNNING) {
          double x = xFromScr(event.getX());
          double y = yFromScr(event.getY());

          /*
           * Set the values used when Point To Shoot is on.
           */
          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (y < TOUCH_FIRE_Y_THRESHOLD) {
              mTouchFire = true;
              mTouchX = x;
              mTouchY = y;
            }
            else if (Math.abs(x - 318) <= TOUCH_SWAP_X_THRESHOLD)
              mTouchSwap = true;
          }

          /*
           * Set the values used when Aim Then Shoot is on.
           */
          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (y < ATS_TOUCH_FIRE_Y_THRESHOLD) {
              mATSTouchFire = true;
            }
            mATSTouchLastX = x;
          }
          else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (y >= ATS_TOUCH_FIRE_Y_THRESHOLD) {
              mATSTouchDX = (x - mATSTouchLastX) * ATS_TOUCH_COEFFICIENT;
            }
            mATSTouchLastX = x;
          }
          return true;
        }
        return false;
      }
    }

    /**
     * updateStateOnEvent() - a common method to process motion events
     * to set the game state.  When the motion event has been fully
     * processed, this function will return true, otherwise if the
     * calling method should also process the motion event, this
     * function will return false.
     * @param event - The MotionEvent to process for the purpose of
     * updating the game state.  If this parameter is null, then the
     * game state is forced to update if applicable based on the current
     * game state.
     * @return This function returns <code>true</code> to inform the
     * calling function that the game state has been updated and that no
     * further processing is necessary, and <code>false</code> to
     * indicate that the caller should continue processing the event.
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
            if (!mBlankScreen) {
              setState(stateEnum.RUNNING);
              return true;
            }
            break;

          case PAUSED:
            if (mShowScores) {
              mShowScores = false;
              nextLevel();
              if (getCurrentLevelIndex() != 0)
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

    private void drawBackground(Canvas c) {
      Sprite.drawImage(mBackground, 0, 0, c, mDisplayScale,
                       mDisplayDX, mDisplayDY);
    }

    private void drawLevelNumber(Canvas canvas) {
      int y = 433;
      int x;
      int level = mLevelManager.getLevelIndex() + 1;
      if (level < 10) {
        x = 185;
        mFont.paintChar(Character.forDigit(level, 10), x, y, canvas,
                        mDisplayScale, mDisplayDX, mDisplayDY);
      }
      else if (level < 100) {
        x = 178;
        x += mFont.paintChar(Character.forDigit(level / 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        mFont.paintChar(Character.forDigit(level % 10, 10), x, y, canvas,
                        mDisplayScale, mDisplayDX, mDisplayDY);
      }
      else {
        x = 173;
        x += mFont.paintChar(Character.forDigit(level / 100, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        level -= 100 * (level / 100);
        x += mFont.paintChar(Character.forDigit(level / 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        mFont.paintChar(Character.forDigit(level % 10, 10), x, y, canvas,
                        mDisplayScale, mDisplayDX, mDisplayDY);
      }
    }

    private void drawAboutScreen(Canvas canvas) {
      canvas.drawRGB(0, 0, 0);
      if (!mBlankScreen) {
        int x = 168;
        int y = 20;
        int ysp = 26;
        int indent = 10;
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
    }

    /**
     * Draw the high score screen for puzzle game mode.
     * <p>The objective of puzzle game mode is efficiency - fire as few
     * bubbles as possible as quickly as possible.  Thus the high score
     * will exhibit the fewest shots fired the quickest.
     * @param canvas - the drawing canvas to display the scores on.
     * @param level - the level index.
     */
    private void drawHighScoreScreen(Canvas canvas, int level) {
      canvas.drawRGB(0, 0, 0);
      int x = 168;
      int y = 20;
      int ysp = 26;
      int indent = 10;

      mFont.print("highscore for level " + (level + 1), x, y, canvas,
                  mDisplayScale, mDisplayDX, mDisplayDY);
      y += 2 * ysp;

      List<HighscoreDO> hlist = mHighscoreManager.getHighScore(level, 15);
      long lastScoreId = mHighscoreManager.getLastScoreId();
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
      drawLevelNumber(canvas);
      mFrozenGame.paint(canvas, mDisplayScale, mDisplayDX, mDisplayDY);
    }

    private void updateGameState() {
      if ((mFrozenGame == null) || (mHighscoreManager == null))
        return;

      gameEnum gameState = mFrozenGame.play(mLeft || mWasLeft,
                                            mRight || mWasRight,
                                            mFire || mUp || mWasFire || mWasUp,
                                            mDown || mWasDown || mTouchSwap,
                                            mTrackballDX,
                                            mTouchFire, mTouchX, mTouchY,
                                            mATSTouchFire, mATSTouchDX);

      if ((gameState == gameEnum.NEXT_LOST) ||
          (gameState == gameEnum.NEXT_WON )) {
        if (gameState == gameEnum.NEXT_WON) {
          mShowScores = true;
          pause();
          if (FrozenBubble.getAdsOn() &&
              (!mInterstitialShown && (new Random().nextInt(10) == 0))) {
            mInterstitialShown = true;
            Intent intent = new Intent(mContext, InterstitialActivity.class);
            mContext.startActivity(intent);
          }
        }
        else {
          nextLevel();
        }

        if (mGameListener != null) {
          if (gameState == gameEnum.NEXT_WON) {
            mGameListener.onGameEvent(eventEnum.GAME_WON);
          }
          else {
            mGameListener.onGameEvent(eventEnum.GAME_LOST);
          }
        }
      }
      mWasLeft      = false;
      mWasRight     = false;
      mWasFire      = false;
      mWasUp        = false;
      mWasDown      = false;
      mTrackballDX  = 0;
      mTouchFire    = false;
      mTouchSwap    = false;
      mATSTouchFire = false;
      mATSTouchDX   = 0;
    }

    private void nextLevel() {
      mFrozenGame = new FrozenGame(mBackground, mBubbles, mBubblesBlind,
                                   mFrozenBubbles, mTargetedBubbles,
                                   mBubbleBlink, mGameWon, mGameLost,
                                   mGamePaused, mHurry, mPenguins,
                                   mCompressorHead, mCompressor, mLauncher,
                                   mSoundManager, mLevelManager,
                                   mHighscoreManager);
      mHighscoreManager.startLevel(mLevelManager.getLevelIndex());
    }

    public void cleanUp() {
      synchronized (mSurfaceHolder) {
        /*
         * I don't really understand why all this is necessary.
         * I used to get a crash (an out-of-memory error) once every six
         * or seven times I started the game.  I googled the error and
         * someone said you have to call recycle() on all the bitmaps
         * and set the pointers to null to facilitate garbage
         * collection.  So I did and the crashes went away.
         */
        mFrozenGame = null;
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
        mPenguinsOrig.recycle();
        mPenguinsOrig = null;
        mCompressorHeadOrig.recycle();
        mCompressorHeadOrig = null;
        mCompressorOrig.recycle();
        mCompressorOrig = null;
        mLifeOrig.recycle();
        mLifeOrig = null;

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
          mPenguins.bmp.recycle();
          mCompressorHead.bmp.recycle();
          mCompressor.bmp.recycle();
          mLife.bmp.recycle();
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
        mPenguins.bmp = null;
        mPenguins = null;
        mCompressorHead.bmp = null;
        mCompressorHead = null;
        mCompressor.bmp = null;
        mCompressor = null;
        mLife.bmp = null;
        mLife = null;

        mImageList = null;
        mSoundManager.cleanUp();
        mSoundManager = null;
        mLevelManager = null;
        mHighscoreManager.close();
      }
    }

    public void setPosition(double value) {
      mFrozenGame.setPosition(value);
    }
  }

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    //Log.i("frozen-bubble", "GameView constructor");

    mContext = context;
    SurfaceHolder holder = getHolder();
    holder.addCallback(this);

    mGameThread = new GameThread(holder, null, 0);
    setFocusable(true);
    setFocusableInTouchMode(true);

    mGameThread.setRunning(true);
    mGameThread.start();
  }

  public GameView(Context context, byte[] levels, int startingLevel) {
    super(context);
    //Log.i("frozen-bubble", "GameView constructor");

    mContext = context;
    SurfaceHolder holder = getHolder();
    holder.addCallback(this);

    mGameThread = new GameThread(holder, levels, startingLevel);
    setFocusable(true);
    setFocusableInTouchMode(true);

    mGameThread.setRunning(true);
    mGameThread.start();
  }

  public GameThread getThread() {
    return mGameThread;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent msg) {
    //Log.i("frozen-bubble", "GameView.onKeyDown()");
    return mGameThread.doKeyDown(keyCode, msg);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent msg) {
    //Log.i("frozen-bubble", "GameView.onKeyUp()");
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
    //Log.i("frozen-bubble", "GameView.onWindowFocusChanged()");
    if (!hasWindowFocus) {
      if (mGameThread != null)
        mGameThread.pause();
    }
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width,
                              int height) {
    //Log.i("frozen-bubble", "GameView.surfaceChanged");
    mGameThread.setSurfaceSize(width, height);
  }

  public void surfaceCreated(SurfaceHolder holder) {
    //Log.i("frozen-bubble", "GameView.surfaceCreated()");
    mGameThread.setSurfaceOK(true);
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    //Log.i("frozen-bubble", "GameView.surfaceDestroyed()");
    mGameThread.setSurfaceOK(false);
  }

  public void cleanUp() {
    //Log.i("frozen-bubble", "GameView.cleanUp()");
    mGameThread.cleanUp();
  }

  /**
   * This is a class that extends TimerTask to resume displaying the
   * game screen as normal after it has been shown as a blank screen.
   * @author Eric Fortin
   *
   */
  class resumeGameScreenTask extends TimerTask {
    @Override
    public void run() {
      mBlankScreen = false;
      cancel();
    }
  };

  /**
   * Display a blank screen (black background) for the specified wait
   * interval.
   * @param clearScreen - If <code>true</code>, show a blank screen for
   * the specified wait interval.  If <code>false</code>, show the
   * normal screen.
   * @param wait - The amount of time to display the blank screen.
   */
  public void clearGameScreen(boolean clearScreen, int wait) {
    mBlankScreen = clearScreen;
    try {
      if (clearScreen) {
        mGameThread.setState(stateEnum.ABOUT);
        Timer timer = new Timer();
        timer.schedule(new resumeGameScreenTask(), wait, wait + 1);
      }
    } catch (IllegalArgumentException illArgEx) {
      illArgEx.printStackTrace();
      mBlankScreen = false;
    } catch (IllegalStateException illStateEx) {
      illStateEx.printStackTrace();
      mBlankScreen = false;
    }
  }
}
