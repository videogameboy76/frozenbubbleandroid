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

package com.efortin.frozenbubble;

import org.jfedor.frozenbubble.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference
  implements OnSeekBarChangeListener {

  private final String TAG = getClass().getName();

  private static final String ANDROIDNS =
      "http://schemas.android.com/apk/res/android";
  private static final String FROZENBUBBLENS =
      "https://code.google.com/p/frozenbubbleandroid";
  private static final int DEFAULT_VALUE = 50;

  private int mMaxValue      = 100;
  private int mMinValue      = 0;
  private int mInterval      = 1;
  private int mCurrentValue;
  private String mUnitsLeft  = "";
  private String mUnitsRight = "";
  private SeekBar mSeekBar;
  private TextView mStatusText;

  public SeekBarPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    initPreference(context, attrs);
  }

  public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initPreference(context, attrs);
  }

  private void initPreference(Context context, AttributeSet attrs) {
    setValuesFromXml(attrs);
    mSeekBar = new SeekBar(context, attrs);
    mSeekBar.setMax(mMaxValue - mMinValue);
    mSeekBar.setOnSeekBarChangeListener(this);
  }

  private void setValuesFromXml(AttributeSet attrs) {
    mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 100);
    mMinValue = attrs.getAttributeIntValue(FROZENBUBBLENS, "min", 0);

    mUnitsLeft = getAttributeStringValue(attrs, FROZENBUBBLENS,
        "unitsLeft", "");
    String units = getAttributeStringValue(attrs, FROZENBUBBLENS, "units", "");
    mUnitsRight = getAttributeStringValue(attrs, FROZENBUBBLENS,
        "unitsRight", units);

    try {
      String newInterval = attrs.getAttributeValue(FROZENBUBBLENS, "interval");
      if(newInterval != null)
        mInterval = Integer.parseInt(newInterval);
    }
    catch(Exception e) {
      Log.e(TAG, "Invalid interval value", e);
    }
  }

  private String getAttributeStringValue(AttributeSet attrs, String namespace,
                                         String name, String defaultValue) {
    String value = attrs.getAttributeValue(namespace, name);
    if(value == null)
      value = defaultValue;

    return value;
  }

  @Override
  protected View onCreateView(ViewGroup parent){
      RelativeLayout layout = null;

      try {
        LayoutInflater mInflater = (LayoutInflater)getContext().
            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layout = (RelativeLayout)mInflater.
            inflate(R.layout.seek_bar_preference, parent, false);
      }
      catch(Exception e)
      {
        Log.e(TAG, "Error creating seek bar preference", e);
      }

      return layout;
  }

  @Override
  public void onBindView(View view) {
      super.onBindView(view);

      try {
        // move our seekbar to the new view we've been given
        ViewParent oldContainer = mSeekBar.getParent();
        ViewGroup newContainer =
            (ViewGroup)view.findViewById(R.id.seekBarPrefBarContainer);

        if (oldContainer != newContainer) {
          // remove the seekbar from the old view
          if (oldContainer != null) {
            ((ViewGroup)oldContainer).removeView(mSeekBar);
          }
          // remove the existing seekbar (there may not be one) and add ours
          newContainer.removeAllViews();
          newContainer.addView(mSeekBar, ViewGroup.LayoutParams.FILL_PARENT,
                               ViewGroup.LayoutParams.WRAP_CONTENT);
        }
      } catch(Exception ex) {
        Log.e(TAG, "Error binding view: " + ex.toString());
      }

      updateView(view);
  }

  /**
   * Update a SeekBarPreference view with our current state
   * @param view
   */
  protected void updateView(View view) {
    try {
      RelativeLayout layout = (RelativeLayout)view;

      mStatusText = (TextView)layout.findViewById(R.id.seekBarPrefValue);
      mStatusText.setText(String.valueOf(mCurrentValue));
      mStatusText.setMinimumWidth(30);

      mSeekBar.setProgress(mCurrentValue - mMinValue);

      TextView unitsRight =
          (TextView)layout.findViewById(R.id.seekBarPrefUnitsRight);
      unitsRight.setText(mUnitsRight);

      TextView unitsLeft =
          (TextView)layout.findViewById(R.id.seekBarPrefUnitsLeft);
      unitsLeft.setText(mUnitsLeft);
    } catch(Exception e) {
        Log.e(TAG, "Error updating seek bar preference", e);
    }
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress,
                                boolean fromUser) {
    int newValue = progress + mMinValue;

    if(newValue > mMaxValue)
      newValue = mMaxValue;
    else if(newValue < mMinValue)
      newValue = mMinValue;
    else if(mInterval != 1 && newValue % mInterval != 0)
      newValue = Math.round(((float)newValue)/mInterval)*mInterval;

    // change rejected, revert to the previous value
    if(!callChangeListener(newValue)){
      seekBar.setProgress(mCurrentValue - mMinValue); 
      return; 
    }

    // change accepted, store it
    mCurrentValue = newValue;
    mStatusText.setText(String.valueOf(newValue));
    persistInt(newValue);
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
      notifyChanged();
  }

  @Override 
  protected Object onGetDefaultValue(TypedArray ta, int index) {
    int defaultValue = ta.getInt(index, DEFAULT_VALUE);
    return defaultValue;
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
    if(restoreValue) {
      mCurrentValue = getPersistedInt(mCurrentValue);
    }
    else {
      int temp = 0;
      try {
        temp = (Integer)defaultValue;
      }
      catch(Exception ex) {
        Log.e(TAG, "Invalid default value: " + defaultValue.toString());
      }

      persistInt(temp);
      mCurrentValue = temp;
    }
  }

  public void setDefaultValue(int newValue) {
    mCurrentValue = newValue;
  }
}
