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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.media.SoundPool;

public class SoundManager {
  private static final int MAX_STREAMS_PER_POOL = 4;

  private Context                  context;
  private List<SoundPoolContainer> containers;

  public SoundManager(Context context) {
    this.context    = context;
    this.containers =
        Collections.synchronizedList(new ArrayList<SoundPoolContainer>());
  }

  public final void cleanUp() {
    for (SoundPoolContainer container : containers) {
      container.release();
    }
    containers.clear();
    containers = null;
    context    = null;
  }

  public void loadSound(String id, int resId) {
    try {
      for (SoundPoolContainer container : containers) {
        if (container.contains(id)) {
          return;
        }
      }
      for (SoundPoolContainer container : containers) {
        if (!container.isFull()) {
          container.load(id, resId);
          return;
        }
      }
      SoundPoolContainer container = new SoundPoolContainer(context);
      containers.add (container);
      container .load(id, resId);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void playSound(String id, int resId) {
    if (FrozenBubble.getSoundOn()) {
      for (SoundPoolContainer container : containers) {
        if (container.contains(id)) {
          container.play(id, resId);
          break;
        }
      }
    }
  }

  public void onPause() {
    for (SoundPoolContainer container : containers) {
      container.onPause();
    }
  }

  public void onResume() {
    for (SoundPoolContainer container : containers) {
      container.onResume();
    }
  }

  private class SoundPoolContainer {
    private AtomicInteger        size;
    private Context              context;
    private Map<String, Integer> soundMap;
    private SoundPool            soundPool;

    public SoundPoolContainer(Context context) {
      this.context   = context;
      this.size      = new AtomicInteger(0);
      this.soundMap  =
          new ConcurrentHashMap<String, Integer>(MAX_STREAMS_PER_POOL);
      this.soundPool =
          new SoundPool(MAX_STREAMS_PER_POOL,
                        android.media.AudioManager.STREAM_MUSIC, 0);
    }

    public void load(String id, int resId) {
      try {
        size.incrementAndGet();
        soundMap.put(id, soundPool.load(context, resId, 1));
      } catch (Exception e) {
        size.decrementAndGet();
        e.printStackTrace();
      }
    }

    public void play(String id, int resId) {
      android.media.AudioManager audioManager =
          (android.media.AudioManager) context.
          getSystemService(Context.AUDIO_SERVICE);
      final float streamVolume =
          (float)audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) /
          (float)audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
      Integer soundId = soundMap.get(id);

      if (soundId != null) try {
        soundPool.play(soundId, streamVolume, streamVolume, 1, 0, 1f);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void release() {
      soundPool.release();
      soundPool = null;
      soundMap .clear();
      soundMap  = null;
      context   = null;
    }

    public boolean contains(String id) {
      return soundMap.containsKey(id);
    }

    public boolean isFull() {
      return size.get() >= MAX_STREAMS_PER_POOL;
    }

    public void onPause() {
      try {
        soundPool.autoPause();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void onResume() {
      try {
        soundPool.autoResume();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
