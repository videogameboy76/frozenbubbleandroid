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
// This file is derived from the LunarView.java file which is part of
// the Lunar Lander game included with Android documentation.  The copyright
// notice for the Lunar Lander is reproduced below.
/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.jfedor.frozenbubble;

import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.MotionEvent;
import java.util.Vector;

import android.util.Log;

class GameView extends SurfaceView implements SurfaceHolder.Callback {
  class GameThread extends Thread {
    private static final int FRAME_DELAY = 40;

    public static final int STATE_RUNNING = 1;
    public static final int STATE_PAUSE = 2;
    public static final int STATE_ABOUT = 4;

    public static final int GAMEFIELD_WIDTH = 320;
    public static final int GAMEFIELD_HEIGHT = 480;
    public static final int EXTENDED_GAMEFIELD_WIDTH = 640;

    private static final double TRACKBALL_COEFFICIENT = 5;
    private static final double TOUCH_COEFFICIENT = 0.2;
    private static final double TOUCH_FIRE_Y_THRESHOLD = 350;

    private int mCanvasHeight = 1;
    private int mCanvasWidth = 1;
    private long mLastTime;
    private int mMode;
    private boolean mRun = false;

    private boolean mLeft = false;
    private boolean mRight = false;
    private boolean mUp = false;
    private boolean mFire = false;
    private boolean mWasLeft = false;
    private boolean mWasRight = false;
    private boolean mWasFire = false;
    private boolean mWasUp = false;
    private double mTrackballDX = 0;
    private double mTouchDX = 0;
    private double mTouchLastX;
    private boolean mTouchFire = false;

    private SurfaceHolder mSurfaceHolder;
    private boolean mSurfaceOK = false;

    private double mDisplayScale;
    private int mDisplayDX;
    private int mDisplayDY;

    private FrozenGame mFrozenGame;

    private boolean mImagesReady = false;

    private Bitmap mBackgroundOrig;
    private Bitmap[] mBubblesOrig;
    private Bitmap[] mBubblesBlindOrig;
    private Bitmap[] mFrozenBubblesOrig;
    private Bitmap[] mTargetedBubblesOrig;
    private Bitmap mBubbleBlinkOrig;
    private Bitmap mGameWonOrig;
    private Bitmap mGameLostOrig;
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
    private BmpWrap mHurry;
    private BmpWrap mPenguins;
    private BmpWrap mCompressorHead;
    private BmpWrap mCompressor;
    private BmpWrap mLife;
    private BmpWrap mFontImage;
    // Launcher has to be a drawable, not a bitmap, because we rotate it.
    private Drawable mLauncher;
    private SoundManager mSoundManager;
    private LevelManager mLevelManager;
    private BubbleFont mFont;

    Vector mImageList;

    public int getCurrentLevelIndex()
    {
      synchronized (mSurfaceHolder) {
        return mLevelManager.getLevelIndex();
      }
    }

    private BmpWrap NewBmpWrap()
    {
      int new_img_id = mImageList.size();
      BmpWrap new_img = new BmpWrap(new_img_id);
      mImageList.addElement(new_img);
      return new_img;
    }

    public GameThread(SurfaceHolder surfaceHolder, byte[] customLevels,
                      int startingLevel)
    {
      //Log.i("frozen-bubble", "GameThread()");
      mSurfaceHolder = surfaceHolder;
      Resources res = mContext.getResources();
      setState(STATE_PAUSE);

      BitmapFactory.Options options = new BitmapFactory.Options();

      // The Options.inScaled field is only available starting at API 4.
      try {
        Field f = options.getClass().getField("inScaled");
        f.set(options, Boolean.FALSE);
      } catch (Exception ignore) { }

      mBackgroundOrig =
        BitmapFactory.decodeResource(res, R.drawable.background, options);
      mBubblesOrig = new Bitmap[8];
      mBubblesOrig[0] = BitmapFactory.decodeResource(res, R.drawable.bubble_1,
                                                     options);
      mBubblesOrig[1] = BitmapFactory.decodeResource(res, R.drawable.bubble_2,
                                                     options);
      mBubblesOrig[2] = BitmapFactory.decodeResource(res, R.drawable.bubble_3,
                                                     options);
      mBubblesOrig[3] = BitmapFactory.decodeResource(res, R.drawable.bubble_4,
                                                     options);
      mBubblesOrig[4] = BitmapFactory.decodeResource(res, R.drawable.bubble_5,
                                                     options);
      mBubblesOrig[5] = BitmapFactory.decodeResource(res, R.drawable.bubble_6,
                                                     options);
      mBubblesOrig[6] = BitmapFactory.decodeResource(res, R.drawable.bubble_7,
                                                     options);
      mBubblesOrig[7] = BitmapFactory.decodeResource(res, R.drawable.bubble_8,
                                                     options);
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
      mBubbleBlinkOrig =
        BitmapFactory.decodeResource(res, R.drawable.bubble_blink, options);
      mGameWonOrig = BitmapFactory.decodeResource(res, R.drawable.win_panel,
                                                  options);
      mGameLostOrig = BitmapFactory.decodeResource(res, R.drawable.lose_panel,
                                                   options);
      mHurryOrig = BitmapFactory.decodeResource(res, R.drawable.hurry, options);
      mPenguinsOrig = BitmapFactory.decodeResource(res, R.drawable.penguins,
                                                   options);
      mCompressorHeadOrig =
        BitmapFactory.decodeResource(res, R.drawable.compressor, options);
      mCompressorOrig =
        BitmapFactory.decodeResource(res, R.drawable.compressor_body, options);
      mLifeOrig = BitmapFactory.decodeResource(res, R.drawable.life, options);
      mFontImageOrig =
        BitmapFactory.decodeResource(res, R.drawable.bubble_font, options);

      mImageList = new Vector();

      mBackground = NewBmpWrap();
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
      mBubbleBlink = NewBmpWrap();
      mGameWon = NewBmpWrap();
      mGameLost = NewBmpWrap();
      mHurry = NewBmpWrap();
      mPenguins = NewBmpWrap();
      mCompressorHead = NewBmpWrap();
      mCompressor = NewBmpWrap();
      mLife = NewBmpWrap();
      mFontImage = NewBmpWrap();

      mFont = new BubbleFont(mFontImage);
      mLauncher = res.getDrawable(R.drawable.launcher);

      mSoundManager = new SoundManager(mContext);

      if (null == customLevels) {
        try {
          InputStream is = mContext.getAssets().open("levels.txt");
          int size = is.available();
          byte[] levels = new byte[size];
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
      } else {
        // We were launched by the level editor.
        mLevelManager = new LevelManager(customLevels, startingLevel);
      }

      mFrozenGame = new FrozenGame(mBackground, mBubbles, mBubblesBlind,
                                   mFrozenBubbles, mTargetedBubbles,
                                   mBubbleBlink, mGameWon, mGameLost,
                                   mHurry, mPenguins, mCompressorHead,
                                   mCompressor, mLauncher, 
                                   mSoundManager, mLevelManager);
    }

    private void scaleFrom(BmpWrap image, Bitmap bmp)
    {
      if (image.bmp != null && image.bmp != bmp) {
        image.bmp.recycle();
      }

      if (mDisplayScale > 0.99999 && mDisplayScale < 1.00001) {
        image.bmp = bmp;
        return;
      }
      int dstWidth = (int)(bmp.getWidth() * mDisplayScale);
      int dstHeight = (int)(bmp.getHeight() * mDisplayScale);
      image.bmp = Bitmap.createScaledBitmap(bmp, dstWidth, dstHeight, true);
    }

    private void resizeBitmaps()
    {
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
      scaleFrom(mHurry, mHurryOrig);
      scaleFrom(mPenguins, mPenguinsOrig);
      scaleFrom(mCompressorHead, mCompressorHeadOrig);
      scaleFrom(mCompressor, mCompressorOrig);
      scaleFrom(mLife, mLifeOrig);
      scaleFrom(mFontImage, mFontImageOrig);
      //Log.i("frozen-bubble", "resizeBitmaps done.");
      mImagesReady = true;
    }

    public void pause()
    {
      synchronized (mSurfaceHolder) {
        if (mMode == STATE_RUNNING) {
          setState(STATE_PAUSE);
        }
      }
    }

    public void newGame()
    {
      synchronized (mSurfaceHolder) {
        mLevelManager.goToFirstLevel();
        mFrozenGame = new FrozenGame(mBackground, mBubbles, mBubblesBlind,
                                     mFrozenBubbles, mTargetedBubbles,
                                     mBubbleBlink, mGameWon, mGameLost,
                                     mHurry, mPenguins, mCompressorHead,
                                     mCompressor, mLauncher, 
                                     mSoundManager, mLevelManager);
      }
    }

    @Override
    public void run()
    {
      while (mRun) {
        long now = System.currentTimeMillis();
        long delay = FRAME_DELAY + mLastTime - now;
        if (delay > 0) {
          try{
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
                  if (mMode == STATE_ABOUT) {
                    drawAboutScreen(c);
                  } else {
                    if (mMode == STATE_RUNNING) {
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
          if (c != null) {
            mSurfaceHolder.unlockCanvasAndPost(c);
          }
        }
      }
    }

    /**
     * Dump game state to the provided Bundle. Typically called when the
     * Activity is being suspended.
     *
     * @return Bundle with this view's state
     */
    public Bundle saveState(Bundle map) {
      synchronized (mSurfaceHolder) {
        if (map != null) {
          mFrozenGame.saveState(map);
          mLevelManager.saveState(map);
        }
      }
      return map;
    }

    /**
     * Restores game state from the indicated Bundle. Typically called when
     * the Activity is being restored after having been previously
     * destroyed.
     *
     * @param savedState Bundle containing the game state
     */
    public synchronized void restoreState(Bundle map) {
      synchronized (mSurfaceHolder) {
        setState(STATE_PAUSE);
        mFrozenGame.restoreState(map, mImageList);
        mLevelManager.restoreState(map);
      }
    }

    public void setRunning(boolean b) {
      mRun = b;
    }

    public void setState(int mode) {
      synchronized (mSurfaceHolder) {
        mMode = mode;
      }
    }

    public void setSurfaceOK(boolean ok)
    {
      synchronized (mSurfaceHolder) {
        mSurfaceOK = ok;
      }
    }

    public boolean surfaceOK()
    {
      synchronized (mSurfaceHolder) {
        return mSurfaceOK;
      }
    }

    public void setSurfaceSize(int width, int height)
    {
      synchronized (mSurfaceHolder) {
        mCanvasWidth = width;
        mCanvasHeight = height;
        if (width / height >= GAMEFIELD_WIDTH / GAMEFIELD_HEIGHT) {
          mDisplayScale = 1.0 * height / GAMEFIELD_HEIGHT;
          mDisplayDX =
              (int)((width - mDisplayScale * EXTENDED_GAMEFIELD_WIDTH) / 2);
          mDisplayDY = 0;
        } else {
          mDisplayScale = 1.0 * width / GAMEFIELD_WIDTH;
          mDisplayDX = (int)(-mDisplayScale *
                             (EXTENDED_GAMEFIELD_WIDTH - GAMEFIELD_WIDTH) / 2);
          mDisplayDY = (int)((height - mDisplayScale * GAMEFIELD_HEIGHT) / 2);
        }
        resizeBitmaps();
      }
    }

    boolean doKeyDown(int keyCode, KeyEvent msg)
    {
      synchronized (mSurfaceHolder) {
        if (mMode != STATE_RUNNING) {
          setState(STATE_RUNNING);
        }

        if (mMode == STATE_RUNNING) {
          //Log.i("frozen-bubble", "STATE RUNNING");
          if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            mLeft = true;
            mWasLeft = true;
            return true;
          } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            mRight = true;
            mWasRight = true;
            return true;
          } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            mFire = true;
            mWasFire = true;
            return true;
          } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            mUp = true;
            mWasUp = true;
            return true;
          }
        }

        return false;
      }
    }

    boolean doKeyUp(int keyCode, KeyEvent msg)
    {
      synchronized (mSurfaceHolder) {
        if (mMode == STATE_RUNNING) {
          if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            mLeft = false;
            return true;
          } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            mRight = false;
            return true;
          } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            mFire = false;
            return true;
          } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            mUp = false;
            return true;
          }
        }
        return false;
      }
    }

    boolean doTrackballEvent(MotionEvent event)
    {
      synchronized (mSurfaceHolder) {
        if (mMode != STATE_RUNNING) {
          setState(STATE_RUNNING);
        }

        if (mMode == STATE_RUNNING) {
          if (event.getAction() == MotionEvent.ACTION_MOVE) {
            mTrackballDX += event.getX() * TRACKBALL_COEFFICIENT;
            return true;
          }
        }
        return false;
      }
    }

    private double xFromScr(float x)
    {
      return (x - mDisplayDX) / mDisplayScale;
    }

    private double yFromScr(float y)
    {
      return (y - mDisplayDY) / mDisplayScale;
    }

    boolean doTouchEvent(MotionEvent event)
    {
      synchronized (mSurfaceHolder) {
        if (mMode != STATE_RUNNING) {
          setState(STATE_RUNNING);
        }

        double x = xFromScr(event.getX());
        double y = yFromScr(event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          if (y < TOUCH_FIRE_Y_THRESHOLD) {
            mTouchFire = true;
          }
          mTouchLastX = x;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
          if (y >= TOUCH_FIRE_Y_THRESHOLD) {
            mTouchDX = (x - mTouchLastX) * TOUCH_COEFFICIENT;
          }
          mTouchLastX = x;
        }
        return true;
      }
    }

    private void drawBackground(Canvas c)
    {
      Sprite.drawImage(mBackground, 0, 0, c, mDisplayScale,
                       mDisplayDX, mDisplayDY);
    }

    private void drawLevelNumber(Canvas canvas)
    {
      int y = 433;
      int x;
      int level = mLevelManager.getLevelIndex() + 1;
      if (level < 10) {
        x = 185;
        mFont.paintChar(Character.forDigit(level, 10), x, y, canvas,
                        mDisplayScale, mDisplayDX, mDisplayDY);
      } else if (level < 100) {
        x = 178;
        x += mFont.paintChar(Character.forDigit(level / 10, 10), x, y, canvas,
                             mDisplayScale, mDisplayDX, mDisplayDY);
        mFont.paintChar(Character.forDigit(level % 10, 10), x, y, canvas,
                        mDisplayScale, mDisplayDX, mDisplayDY);
      } else {
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

    private void drawAboutScreen(Canvas canvas)
    {
      canvas.drawRGB(0, 0, 0);
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

    private void doDraw(Canvas canvas)
    {
      //Log.i("frozen-bubble", "doDraw()");
      if (!mImagesReady) {
        //Log.i("frozen-bubble", "!mImagesReady, returning");
        return;
      }
      if (mDisplayDX > 0 || mDisplayDY > 0) {
        //Log.i("frozen-bubble", "Drawing black background.");
        canvas.drawRGB(0, 0, 0);
      }
      drawBackground(canvas);
      drawLevelNumber(canvas);
      mFrozenGame.paint(canvas, mDisplayScale, mDisplayDX, mDisplayDY);
    }

    private void updateGameState() {
      if (mFrozenGame.play(mLeft || mWasLeft, mRight || mWasRight,
                           mFire || mUp || mWasFire || mWasUp || mTouchFire,
                           mTrackballDX, mTouchDX)) {
        // Lost or won.  Need to start over.  The level is already
        // incremented if this was a win.
        mFrozenGame = new FrozenGame(mBackground, mBubbles, mBubblesBlind,
                                     mFrozenBubbles, mTargetedBubbles,
                                     mBubbleBlink, mGameWon, mGameLost,
                                     mHurry, mPenguins, mCompressorHead,
                                     mCompressor, mLauncher, mSoundManager,
                                     mLevelManager);
      }
      mWasLeft = false;
      mWasRight = false;
      mWasFire = false;
      mWasUp = false;
      mTrackballDX = 0;
      mTouchFire = false;
      mTouchDX = 0;
    }

    public void cleanUp() {
      synchronized (mSurfaceHolder) {
        // I don't really understand why all this is necessary.
        // I used to get a crash (an out-of-memory error) once every six or
        // seven times I started the game.  I googled the error and someone
        // said you have to call recycle() on all the bitmaps and set
        // the pointers to null to facilitate garbage collection.  So I did
        // and the crashes went away.
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
        mFrozenGame = null;
      }
    }
  }

  private Context mContext;
  private GameThread thread;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    //Log.i("frozen-bubble", "GameView constructor");

    mContext = context;
    SurfaceHolder holder = getHolder();
    holder.addCallback(this);
    
    thread = new GameThread(holder, null, 0);
    setFocusable(true);
    setFocusableInTouchMode(true);

    thread.setRunning(true);
    thread.start();
  }

  public GameView(Context context, byte[] levels, int startingLevel)
  {
    super(context);
    //Log.i("frozen-bubble", "GameView constructor");

    mContext = context;
    SurfaceHolder holder = getHolder();
    holder.addCallback(this);
    
    thread = new GameThread(holder, levels, startingLevel);
    setFocusable(true);
    setFocusableInTouchMode(true);

    thread.setRunning(true);
    thread.start();
  }
  
  public GameThread getThread() {
    return thread;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent msg) {
    //Log.i("frozen-bubble", "GameView.onKeyDown()");
    return thread.doKeyDown(keyCode, msg);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent msg) {
    //Log.i("frozen-bubble", "GameView.onKeyUp()");
    return thread.doKeyUp(keyCode, msg);
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    //Log.i("frozen-bubble", "event.getX(): " + event.getX());
    //Log.i("frozen-bubble", "event.getY(): " + event.getY());
    return thread.doTrackballEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return thread.doTouchEvent(event);
  }

  @Override
  public void onWindowFocusChanged(boolean hasWindowFocus) {
    //Log.i("frozen-bubble", "GameView.onWindowFocusChanged()");
    if (!hasWindowFocus) {
      thread.pause();
    }
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width,
                             int height) {
    //Log.i("frozen-bubble", "GameView.surfaceChanged");
    thread.setSurfaceSize(width, height);
  }

  public void surfaceCreated(SurfaceHolder holder) {
    //Log.i("frozen-bubble", "GameView.surfaceCreated()");
    thread.setSurfaceOK(true);
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    //Log.i("frozen-bubble", "GameView.surfaceDestroyed()");
    thread.setSurfaceOK(false);
  }

  public void cleanUp() {
    //Log.i("frozen-bubble", "GameView.cleanUp()");
    thread.cleanUp();
    mContext = null;
  }
}
