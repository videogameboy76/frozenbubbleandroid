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

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Android Accelerometer Sensor Manager Archetype
 *
 * <p>   Licensed under GPL v3:
 * <br>  http://www.gnu.org/licenses/gpl-3.0.html
 *
 * @author Antoine Vianey
 *
 */
public class AccelerometerManager {
  private static Sensor sensor;
  private static SensorManager sensorManager;

  /** indicates whether or not Accelerometer Sensor is supported */
  private static Boolean supported;
  /** indicates whether or not Accelerometer Sensor is running */
  private static boolean running = false;

  public interface AccelerometerListener {
    public void onAccelerationChanged(float x, float y, float z);
  }

  // you could use an OrientationListener array instead
  // if you plans to use more than one listener
  private static AccelerometerListener listener;

  /**
   * Returns true if the manager is listening to orientation changes
   */
  public static boolean isListening() {
    return running;
  }

  /**
   * Unregisters listeners.
   */
  public static void stopListening() {
    running = false;

    try {
      if (sensorManager != null && sensorEventListener != null) {
        sensorManager.unregisterListener(sensorEventListener);
      }
    } catch (Exception e) {}
  }

  /**
   * Returns true if at least one Accelerometer sensor is available.
   */
  public static boolean isSupported(Context context) {
    if (supported == null) {
      sensorManager =
        (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
      List<Sensor> sensors =
        sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
      supported = Boolean.valueOf(sensors.size() > 0);
    }
    return supported;
  }

  /**
   * Registers a listener and starts listening.
   *
   * @param accelerometerListener
   *        - Callback for accelerometer events.
   */
  public static void startListening(Context context,
    AccelerometerListener accelerometerListener) {
    sensorManager =
      (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    List<Sensor> sensors =
      sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

    if (sensors.size() > 0) {
      sensor   = sensors.get(0);
      running  =
        sensorManager.registerListener(sensorEventListener, sensor,
                                       SensorManager.SENSOR_DELAY_FASTEST);
      listener = accelerometerListener;
    }
  }

  /**
   * The listener that listens to events from the accelerometer listener.
   */
  private static SensorEventListener sensorEventListener = 
    new SensorEventListener() {
    private float filter = 0.1f;
    private float avgX   = 0.0f;
    private float avgY   = 0.0f;
    private float avgZ   = 0.0f;

    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void onSensorChanged(SensorEvent event) {
      avgX = event.values[0] * filter + (avgX * (1.0f-filter));
      avgY = event.values[1] * filter + (avgY * (1.0f-filter));
      avgZ = event.values[2] * filter + (avgZ * (1.0f-filter));

      listener.onAccelerationChanged(avgX, avgY, avgZ);
    }
  };
}
