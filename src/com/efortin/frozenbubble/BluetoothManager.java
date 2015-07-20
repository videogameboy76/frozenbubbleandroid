/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright (c) 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright (c) 2003 Glenn Sanson.
 * Additional source - Copyright (c) 2015 Eric Fortin.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

/**
 * <p>Bluetooth socket class.
 * <p>Requires the following permissions:
 * <code>BLUETOOTH</code>
 * <code>BLUETOOTH_ADMIN</code>
 * @author Eric Fortin, Wednesday, May 8, 2013
 */
public class BluetoothManager {
  private static final String LOG_TAG  = UDPSocket.class.getSimpleName();
  private static final UUID   SPP_UUID = UUID.
      fromString("00001101-0000-1000-8000-00805F9B34FB");

  /*
   * BluetoothManager class member variables.
   */
  private boolean                      isServer;
  private boolean                      paused;
  private boolean                      running;
  private int                          deviceIndex;
  private ArrayList<byte[]>            txList         = null;
  private ArrayList<BluetoothListener> listenerList   = null;
  private BluetoothAdapter             myAdapter      = null;
  private BluetoothSocket              mySocket       = null;
  private InputStream                  myInputStream  = null;
  private OutputStream                 myOutputStream = null;
  private String                       remoteName     = "not available";
  private Thread                       myRxThread     = null;
  private Thread                       myTxThread     = null;

  /*
   * Listener interface for various UDP socket events.
   *
   * This interface defines the abstract listener method that needs
   * to be instantiated by the registrar, as well as the various
   * events supported by the interface.
   */
  public interface BluetoothListener {
    public abstract void onBluetoothEvent(byte[] buffer,
                                          int length);
  }

  public void setBluetoothListener(BluetoothListener listener) {
    listenerList.add(listener);
  }

  /**
   * Bluetooth socket class constructor.
   */
  public BluetoothManager(boolean isServer, int deviceIndex) {
    this.isServer    = isServer;
    this.deviceIndex = deviceIndex;
    myInputStream    = null;
    myOutputStream   = null;
    myRxThread       = null;
    myTxThread       = null;
    txList           = null;
    txList           = new ArrayList<byte[]>();
    listenerList     = new ArrayList<BluetoothListener>();
    paused           = false;
    running          = true;
    myRxThread       = new Thread(new BluetoothRxThread(), "myRxThread");
    myRxThread.start();
    myTxThread       = new Thread(new BluetoothTxThread(), "myTxThread");
    myTxThread.start();
  }

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
  public static String bytesToHex(byte[] bytes, int length) {
    char[] hexChars = new char[length * 2];
    for ( int j = 0; j < length; j++ ) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2    ] = hexArray[v >>>  4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Clean up the UDP socket by stopping the thread, closing the UDP
   * socket and freeing resources.
   */
  public void cleanUp() {
    if (listenerList != null) {
      listenerList.clear();
    }
    listenerList = null;
    stopThread();
    if (mySocket != null) {
      try {
        mySocket.close();
      } catch (IOException e) {
        // Auto-generated catch block
        e.printStackTrace();
      }
    }
    mySocket = null;
    if (myInputStream != null) {
      try {
        myInputStream.close();
      } catch (IOException e) {
        // Auto-generated catch block
        e.printStackTrace();
      }
    }
    myInputStream = null;
    if (myOutputStream != null) {
      try {
        myOutputStream.close();
      } catch (IOException e) {
        // Auto-generated catch block
        e.printStackTrace();
      }
    }
    myOutputStream = null;
    if (txList != null) {
      txList.clear();
    }
    txList = null;
  }

  /**
   * Configure the Bluetooth socket settings.
   */
  private void configureBluetoothSocket() {
    mySocket  = null;
    myAdapter = BluetoothAdapter.getDefaultAdapter();

    if (myAdapter != null) {
      if (isServer) {
        try {
          BluetoothServerSocket myServer =
              myAdapter.listenUsingInsecureRfcommWithServiceRecord(getLocalName(),
                                                                   SPP_UUID);
          myAdapter.cancelDiscovery();
          mySocket   = myServer.accept();
          myServer .close();
          remoteName = mySocket.getRemoteDevice().getName();
        } catch (IOException e) {
          // Auto-generated catch block
          e.printStackTrace();
          mySocket = null;
        } catch (NullPointerException e) {
          e.printStackTrace();
          mySocket = null;
        }
      }
      else {
        BluetoothDevice device = getPairedDevice(deviceIndex);
        remoteName = device.getName();
        try {
          mySocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
          myAdapter.cancelDiscovery();
          mySocket .connect();
        } catch (IOException e) {
          // Auto-generated catch block
          e.printStackTrace();
          mySocket = null;
        } catch (NullPointerException e) {
          e.printStackTrace();
          mySocket = null;
        }
      }

      if (mySocket != null) {
        try {
          myInputStream  = mySocket.getInputStream();
          myOutputStream = mySocket.getOutputStream();
        } catch (IOException e) {
          // Auto-generated catch block
          e.printStackTrace();
          try {
            mySocket.close();
          } catch (IOException ioe) {
            ioe.printStackTrace();
          }
          mySocket = null;
        }
      }
    }
  }

  public String getLocalName() {
    String name = "not available";

    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

    if (adapter != null) {
      if (!adapter.isEnabled()) {
        name = "bluetooth disabled";
      }
      else {
        name = adapter.getName();
      }
    }

    return name;
  }

  /**
   * Obtain the desired paired Bluetooth device.  If the desired index
   * is greater than the number of entries in the device list, the last
   * entry is provided instead.
   * @param deviceIndex - the index of the desired Bluetooth device.
   * @return The desired Bluetooth device if it exists, otherwise
   * <code>null</code>.
   */
  public static BluetoothDevice getPairedDevice(int deviceIndex) {
    BluetoothDevice[] pairedDevices = getPairedDevices();

    if ((pairedDevices != null) && (pairedDevices.length > 0)) {
      if (deviceIndex >= pairedDevices.length) {
        deviceIndex = pairedDevices.length - 1;
      }
      return pairedDevices[deviceIndex];
    }
    return null;
  }

  /**
   * Obtain an array of the paired Bluetooth devices.
   * @return The array of paired Bluetooth devices if it exists,
   * otherwise <code>null</code>.
   */
  public static BluetoothDevice[] getPairedDevices() {
    BluetoothAdapter localAdapter = null;

    localAdapter = BluetoothAdapter.getDefaultAdapter();
    if ((localAdapter != null) && !localAdapter.getBondedDevices().isEmpty()) {
      Set<BluetoothDevice> deviceSet = localAdapter.getBondedDevices();
      return deviceSet.toArray(new BluetoothDevice[deviceSet.size()]);
    }
    return null;
  }

  /**
   * Obtain the desired local UUID.  If the desired index is greater
   * than the number of entries in the UUID list, the last entry is
   * provided instead.
   * @param uuidIndex - the index to pull from the local UUID list.
   * @return The desired UUID.
   */
  public UUID getLocalUuid(int uuidIndex) {
    UUID serverUuid = null;
    ParcelUuid[] parcelUuids = getLocalUuids();
    if ((parcelUuids != null) && (parcelUuids.length > 0 )) {
      if (uuidIndex >= parcelUuids.length) {
        uuidIndex = parcelUuids.length - 1;
      }
      serverUuid = UUID.fromString(parcelUuids[uuidIndex].toString());
    }
    return serverUuid;
  }

  private ParcelUuid[] getLocalUuids() {
    Method       method = null;
    ParcelUuid[] uuids  = null;

    if (myAdapter != null) {
      try {
        method = myAdapter.getClass().getMethod("getUuids",  (Class<?>[])null);
        uuids  = (ParcelUuid[]) method.invoke(myAdapter, (Object[])null);
      } catch (SecurityException e1) {
        // Auto-generated catch block
        e1.printStackTrace();
      } catch (NoSuchMethodException e1) {
        // Auto-generated catch block
        e1.printStackTrace();
      } catch (IllegalArgumentException e) {
        // Auto-generated catch block
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        // Auto-generated catch block
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        // Auto-generated catch block
        e.printStackTrace();
      }
    }
    return (uuids);
  }

  /**
   * Obtain the desired UUID of the desired bonded device.  If either
   * index is greater than the number of entries, the last entry is
   * provided instead.
   * @param pairedIndex - the paired device index.
   * @param uuidIndex   - the index of the desired device UUID.
   * @return
   */
  public UUID getPairedUuid(int pairedIndex, int uuidIndex) {
    UUID pairedUuid = null;
    ParcelUuid[] parcelUuids = getPairedUuids(pairedIndex);
    if ((parcelUuids != null) && (parcelUuids.length > 0 )) {
      if (uuidIndex >= parcelUuids.length) {
        uuidIndex = parcelUuids.length - 1;
      }
      pairedUuid = UUID.fromString(parcelUuids[uuidIndex].toString());
    }
    return pairedUuid;
  }

  private ParcelUuid[] getPairedUuids(int pairedIndex) {
    Method            method      = null;
    ParcelUuid[]      uuids       = null;
    BluetoothDevice[] pairedArray = null;

    if (myAdapter != null) {
      Set<BluetoothDevice> pairedDevices = myAdapter.getBondedDevices();
      if ((pairedDevices != null) && (pairedDevices.size() > 0 )) {
        pairedArray = (BluetoothDevice[]) pairedDevices.toArray();
        if (pairedIndex >= pairedArray.length) {
          pairedIndex = pairedArray.length - 1;
        }
      }
      try {
        method = pairedArray[pairedIndex].getClass().getMethod("getUuids",  (Class<?>[])null);
        uuids  = (ParcelUuid[]) method.invoke(pairedArray[pairedIndex], (Object[])null);
      } catch (SecurityException e1) {
        // Auto-generated catch block
        e1.printStackTrace();
      } catch (NoSuchMethodException e1) {
        // Auto-generated catch block
        e1.printStackTrace();
      } catch (IllegalArgumentException e) {
        // Auto-generated catch block
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        // Auto-generated catch block
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        // Auto-generated catch block
        e.printStackTrace();
      }
    }
    return (uuids);
  }

  public String getRemoteName() {
    return remoteName;
  }

  public boolean getIsConnected() {
    return (mySocket != null);
  }

  /**
   * This is the Bluetooth InputStream receive thread declaration.
   * @author Eric Fortin, Saturday, July 18, 2015
   * @see <code>configureBluetoothSocket()</code>
   */
  private class BluetoothRxThread implements Runnable {
    private byte[] rxBuffer = new byte[256];

    /**
     * Receive a Bluetooth InputStream message.
     * <p>Given a nonzero socket timeout, it is expected behavior for
     * this method to catch an <code>InterruptedIOException</code>.
     * This method posts an <code>EVENT_PACKET_RX</code> event to the
     * registered listener upon datagram receipt.
     */
    private void receiveDatagram() {
      if (!paused && running) try {
        if (myInputStream != null) {
          myInputStream.read(rxBuffer, 0, rxBuffer.length);
          byte[] buffer  = rxBuffer.clone();
          int    length  = rxBuffer.length;
  
          if (!paused && running && (length != 0) && (listenerList != null)) {
            int size = listenerList.size();
            while (--size >= 0) {
              listenerList.get(size).onBluetoothEvent(buffer, length);
            }
            Log.d(LOG_TAG, "received "+length+" bytes: 0x" +
                bytesToHex(buffer, length));
          }
        }
      } catch (NullPointerException npe) {
        npe.printStackTrace();
      } catch (InterruptedIOException iioe) {
        /*
         * Receive timeout.  This is expected behavior.
         */
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }

    /**
     * This is the thread's <code>run()</code> call.
     * <p>Receive Bluetooth data.
     */
    @Override
    public void run() {
      while (running) {
        if (mySocket == null) {
          configureBluetoothSocket();
        }
        else {
          if (paused) try {
            synchronized(this) {
              wait();
            }
          } catch (InterruptedException ie) {
            /*
             * Interrupted.  This is expected behavior.
             */
          }
  
          if (!paused && running) {
            receiveDatagram();
          }
        }
      }
    }
  }

  /**
   * This is the Bluetooth OutputStream transmit thread declaration.
   * @author Eric Fortin, Saturday, July 18, 2015
   * @see <code>configureBluetoothSocket()</code>
   */
  private class BluetoothTxThread implements Runnable {
    /**
     * This is the thread's <code>run()</code> call.
     * <p>Send Bluetooth data.
     */
    @Override
    public void run() {
      while (running) {
        if (mySocket != null) {
          if (paused) try {
            synchronized(this) {
              wait();
            }
          } catch (InterruptedException ie) {
            /*
             * Interrupted.  This is expected behavior.
             */
          }
  
          if (!paused && running) {
            sendDatagram();
          }
        }
      }
    }

    /**
     * Extract the next buffer from the FIFO transmit list and send it
     * as a Bluetooth OutputStream message.
     */
    private void sendDatagram() {
      if (!paused && running && (txList != null) && txList.size() > 0) try {
        byte[] bytes;
        synchronized(txList) {
          bytes = txList.get(0);
        }
        if (myOutputStream != null) {
          myOutputStream.write(bytes, 0, bytes.length);
          Log.d(LOG_TAG, "transmitted "+bytes.length+" bytes: 0x" +
              bytesToHex(bytes, bytes.length));
          synchronized(txList) {
            txList.remove(0);
          }
        }
      } catch (NullPointerException npe) {
        npe.printStackTrace();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  public void pause() {
    if (running) {
      paused = true;
    }
  }

  /**
   * Stop and <code>join()</code> the Bluetooth RX and TX threads.
   */
  private void stopThread() {
    paused  = false;
    running = false;
    if (myRxThread != null) {
      synchronized(myRxThread) {
        myRxThread.interrupt();
      }
    }
    if (myTxThread != null) {
      synchronized(myTxThread) {
        myTxThread.interrupt();
      }
    }
    /*
     * Close and join() the Bluetooth thread.
     */
    boolean retry = true;
    while (retry && (myRxThread != null)) {
      try {
        myRxThread.join();
        retry = false;
      } catch (InterruptedException e) {
        /*
         * Keep trying to close the Bluetooth thread.
         */
      }
    }
    retry = true;
    while (retry && (myTxThread != null)) {
      try {
        myTxThread.join();
        retry = false;
      } catch (InterruptedException e) {
        /*
         * Keep trying to close the Bluetooth thread.
         */
      }
    }
  }

  /**
   * Send the desired byte buffer as a UDP datagram packet.
   * @param buffer - the byte buffer to transmit.
   * @return <code>true</code> if the buffer was successfully added to
   * the outgoing datagram transmit list, <code>false</code> if the the
   * buffer was unable to be added to the transmit list.
   */
  public boolean transmit(byte[] buffer) {
    if ((mySocket != null) && (myTxThread != null) && running) {
      synchronized(txList) {
        txList.add(buffer);
      }
      return true;
    }
    return false;
  }

  public void unPause() {
    paused = false;
    if (myRxThread != null) {
      synchronized(myRxThread) {
        myRxThread.interrupt();
      }
    }
    if (myTxThread != null) {
      synchronized(myTxThread) {
        myTxThread.interrupt();
      }
    }
  }
};
