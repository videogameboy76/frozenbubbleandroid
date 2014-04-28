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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

/**
 * Multicast manager class - implements UDP unicast and multicast
 * datagram sending and receiving.  This implementation currently only
 * supports IPv4 internet addresses.
 * <p>This class instantiates a thread to send and receive WiFi UDP
 * unicast or multicast messages, depending on the constructor utilized.
 * The <code>MulticastSocket</code> class is a descendant of
 * <code>DatagramSocket</code>, the UDP datagram socket class.  Thus a
 * multicast socket can be used to send and receive either UDP unicast
 * datagrams, or UDP multicast datagrams, which makes it convenient to
 * use either peer to peer networking scheme using virtually identical
 * methods.
 * <p>Multicast host addresses must be in the IPv4 class D address
 * range, with the first octet being within the 224 to 239 range.
 * For example, <code>"225.0.0.15"</code> is an actual IPv4 multicast
 * address.
 * <p>Refer to:
 * <a href="url">http://en.wikipedia.org/wiki/Multicast_address</a> for
 * information regarding Multicast address usage and restrictions.
 * <p>A typical UDP multicast implementation looks like this:
 * <pre><code>
 * MulticastManager session =
 *     new MulticastManager(context, addr, port);
 * session.setMulticastListener(this);
 * </code></pre>
 * or alternatively the UDP unicast implementation appears as follows:
 * <pre><code>
 * MulticastManager session =
 *     new MulticastManager(context, hostName, port);
 * session.setMulticastListener(this);
 * </code></pre>
 * <p>The context from which to obtain the application context, the IP
 * address or host name of the UDP peer, and the port of the UDP socket
 * session must be supplied when creating a new
 * <code>MulticastManager</code> instance.
 * <p>The following <code>uses</code> permissions must be added to the
 * Android project manifest to access all the API functionality required
 * to perform UDP unicast or multicast networking as implemented:<br>
 * <code>ACCESS_NETWORK_STATE<br>
 * ACCESS_WIFI_STATE<br>
 * CHANGE_WIFI_MULTICAST_STATE<br>
 * INTERNET</code>
 * @author Eric Fortin, Wednesday, May 8, 2013
 */
public class MulticastManager {
  private static final String LOG_TAG = MulticastManager.class.getSimpleName();

  /*
   * The following value turns of the multicast receive filter.
   */
  public static final byte FILTER_OFF = -1;

  /*
   * Multicast network transport layer event enumeration.
   */
  public static enum eventEnum {
    PACKET_RX,
    RX_FAIL,
    TX_FAIL;
  }

  /*
   * Listener interface for various multicast management events.
   *
   * This interface defines the abstract listener method that needs
   * to be instantiated by the registrar, as well as the various
   * events supported by the interface.
   */
  public interface MulticastListener {
    public abstract void onMulticastEvent(eventEnum event,
                                          byte[] buffer,
                                          int length);
  }

  public void setMulticastListener(MulticastListener ml) {
    mListener = ml;
  }

  /*
   * MulticastManager class member variables.
   */
  private boolean paused;
  private boolean running;
  private byte filter;
  private int mPort;
  private ArrayList<byte[]> txList = null;
  private Context mContext = null;
  private InetAddress mAddress = null;
  private MulticastListener mListener = null;
  private MulticastSocket mSocket = null;
  private Thread mThread = null;
  private WifiManager.MulticastLock mLock = null;

  /**
   * Multicast manager UDP multicast class constructor.
   * <p>When created, this class instantiates a thread to send and
   * receive WiFi UDP multicast messages.
   * <p>Multicast host addresses must be in the IPv4 class D address
   * range, with the first octet being within the 224 to 239 range.
   * For example, <code>"225.0.0.15"</code> is an actual IPv4 multicast
   * address.
   * <p>Refer to:
   * <a href="url">http://en.wikipedia.org/wiki/Multicast_address</a>
   * for information regarding Multicast address usage and restrictions.
   * <p>A typical implementation looks like this:
   * <pre><code>
   * MulticastManager session =
   *     new MulticastManager(context, addr, port);
   * session.setMulticastListener(this);
   * </code></pre>
   * <p>The context from which to obtain the application context, the IP
   * address of the UDP multicast session, and the port of the multicast
   * session must be supplied when creating a new
   * <code>MulticastManager</code> instance.
   * <p>The following <code>uses</code> permissions must be added to the
   * Android project manifest to perform multicast networking:<br>
   * <code>CHANGE_WIFI_MULTICAST_STATE<br>
   * INTERNET</code>
   * @param context - the context from which to obtain the application
   * context for the purpose of obtaining WiFi service access.
   * @param address - the internet address of this multicast session.
   * @param port - the port number to use for the multicast socket.
   * @throws UnknownHostException the address is invalid.
   * @throws IOException socket creation failed.
   */
  public MulticastManager(Context context, byte[]address, int port) throws
      UnknownHostException, IOException {
    mContext = context.getApplicationContext();
    mPort = port;
    filter = FILTER_OFF;
    mListener = null;
    mLock = null;
    mThread = null;
    txList = null;
    try {
      configureMulticast(address, port);
    } catch(UnknownHostException uhe) {
      throw uhe;
    } catch(IOException ioe) {
      throw ioe;
    }
    txList = new ArrayList<byte[]>();
    WifiManager wm =
        (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
    mLock = wm.createMulticastLock("multicastLock");
    mLock.setReferenceCounted(true);
    mLock.acquire();
    mThread = new Thread(new MulticastThread(), "mThread");
    mThread.start();
  }

  /**
   * Multicast manager UDP unicast class constructor.
   * <p>When created, this class instantiates a thread to send and
   * receive WiFi UDP unicast messages.
   * <p>A typical implementation looks like this:
   * <pre><code>
   * MulticastManager session =
   *     new MulticastManager(context, hostName, port);
   * session.setMulticastListener(this);
   * </code></pre>
   * <p>The context from which to obtain the application context, the
   * host name of the UDP unicast peer to connect to, and the port of
   * the UDP unicast session must be supplied when creating a new
   * <code>MulticastManager</code> instance.
   * <p>The following <code>uses</code> permissions must be addeded to
   * the Android project manifest to perform UDP networking:<br>
   * <code>INTERNET</code>
   * @param context - the context from which to obtain the application
   * context for the purpose of obtaining WiFi service access.
   * @param hostName - the host name of the UDP unicast peer.
   * @param port - the port number to use for the UDP socket.
   * @throws UnknownHostException the host name is invalid.
   * @throws IOException socket creation failed.
   */
  public MulticastManager(Context context, String hostName, int port) throws
      UnknownHostException, IOException {
    mContext = context.getApplicationContext();
    mPort = port;
    filter = FILTER_OFF;
    mListener = null;
    mLock = null;
    mThread = null;
    txList = null;
    try {
      configureMulticast(hostName, port);
    } catch(UnknownHostException uhe) {
      throw uhe;
    } catch(IOException ioe) {
      throw ioe;
    }
    txList = new ArrayList<byte[]>();
    mThread = new Thread(new MulticastThread(), "mThread");
    mThread.start();
  }

  /**
   * Clean up the multicast manager by stopping the thread, closing the
   * multicast socket and freeing resources.
   */
  public void cleanUp() {
    mListener = null;
    stopThread();
    if (mSocket != null) {
      mSocket.close();
    }
    mSocket = null;
    if (txList != null) {
      txList.clear();
    }
    txList = null;
    if (mLock != null) {
      mLock.release();
    }
    mLock = null;
  }

  /**
   * Configure the multicast socket settings.
   * <p>This must be called before <code>start()</code>ing the thread.
   * @param address - the internet address of this multicast session.
   * @param port - the port number to use for the multicast socket.
   * @throws UnknownHostException the address is invalid.
   * @throws IOException socket creation failed.
   */
  private void configureMulticast(byte[]address, int port) throws
      UnknownHostException, IOException {
    try {
      mAddress = InetAddress.getByAddress(address);
      mSocket = new MulticastSocket(port);
      mSocket.setSoTimeout(101);
      mSocket.setBroadcast(false);
      mSocket.setLoopbackMode(true);
      mSocket.joinGroup(mAddress);
    } catch (UnknownHostException uhe) {
      mAddress = null;
      mSocket = null;
      uhe.printStackTrace();
      throw uhe;
    } catch (IOException ioe) {
      mAddress = null;
      mSocket = null;
      ioe.printStackTrace();
      throw ioe;
    }
  }

  /**
   * Configure the multicast socket settings.
   * <p>This must be called before <code>start()</code>ing the thread.
   * @param hostName - the host name of the UDP unicast peer.
   * @param port - the port number to use for the UDP socket.
   * @throws UnknownHostException the host name is invalid.
   * @throws IOException socket creation failed.
   */
  private void configureMulticast(String hostName, int port) throws
      UnknownHostException, IOException{
    try {
      mAddress = InetAddress.getByName(hostName);
      mSocket = new MulticastSocket(port);
      mSocket.setSoTimeout(101);
      mSocket.setBroadcast(false);
      mSocket.setLoopbackMode(true);
    } catch (UnknownHostException uhe) {
      mAddress = null;
      mSocket = null;
      uhe.printStackTrace();
      throw uhe;
    } catch (IOException ioe) {
      mAddress = null;
      mSocket = null;
      ioe.printStackTrace();
      throw ioe;
    }
  }

  /**
   * Obtain the local IP address from the <code>WiFiManager</code>.
   * <p>The following <code>uses</code> permission must be addeded to
   * the Android project manifest to obtain the network connection
   * status:<br>
   * <code>ACCESS_WIFI_STATE</code>
   * @return the local WiFi IP address.
   * @see WifiManager
   */
  public String getLocalIpAddress() {
    WifiManager wifiManager =
        (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    return Formatter.formatIpAddress(wifiInfo.getIpAddress());
  }

  /**
   * Check with the <code>ConnectivityManager</code> if the device is
   * connected to the internet.
   * <p>The following <code>uses</code> permission must be addeded to
   * the Android project manifest to obtain the network connection
   * status:<br>
   * <code>ACCESS_NETWORK_STATE</code>
   * @return <code>true</code> if the device is connected to the
   * internet.
   * @see ConnectivityManager
   */
  public boolean hasInternetConnection()
  {
    ConnectivityManager cm =
        (ConnectivityManager) mContext.getSystemService(Context.
                                                        CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    if ((activeNetwork != null) && activeNetwork.isConnected())
    {
      return true;
    }
    return false;
  }

  /**
   * This is the multicast thread declaration.
   * <p>To support being able to send and receive packets in the same
   * thread, a nonzero socket read timeout must be set, because
   * <code>MulticastSocket.receive()</code> blocks until a packet is
   * received or the socket times out.  Thus, if a timeout of zero is
   * set (which is the default, and denotes that the socket will never
   * time out), a datagram will never be sent unless one has just been
   * received.
   * @author Eric Fortin, Wednesday, May 8, 2013
   * @see <code>configureMulticast()</code>
   */
  private class MulticastThread implements Runnable {
    private byte[] rxBuffer = new byte[256];

    /**
     * Receive a multicast datagram.
     * <p>Given a nonzero socket timeout, it is expected behavior for
     * this method to catch an <code>InterruptedIOException</code>.
     * This method posts an <code>EVENT_PACKET_RX</code> event to the
     * registered listener upon datagram receipt.
     */
    private void receiveDatagram() {
      if (!paused && running) try {
        DatagramPacket dpRX =
            new DatagramPacket(rxBuffer, rxBuffer.length, mAddress, mPort);
        mSocket.receive(dpRX);
        byte[] buffer = dpRX.getData();
        int length    = dpRX.getLength();

        if (!paused && running && (length != 0) && (mListener != null)) {
          if ((filter == FILTER_OFF) || (filter == buffer[0])) {
            mListener.onMulticastEvent(eventEnum.PACKET_RX, buffer, length);
            Log.d(LOG_TAG, "received "+length+" bytes");
          }
        }
      } catch (NullPointerException npe) {
        npe.printStackTrace();
        if (mListener != null) {
          mListener.onMulticastEvent(eventEnum.RX_FAIL, null, 0);
        }
      } catch (InterruptedIOException iioe) {
        /*
         * Receive timeout.  This is expected behavior.
         */
      } catch (IOException ioe) {
        ioe.printStackTrace();
        if (mListener != null) {
          mListener.onMulticastEvent(eventEnum.RX_FAIL, null, 0);
        }
      }
    }

    /**
     * This is the thread's <code>run()</code> call.
     * <p>Send multicast UDP messages, and read multicast datagrams from
     * other clients.
     * <p>To support being able to send and receive packets in the same
     * thread, a nonzero socket read timeout must be set, because
     * <code>MulticastSocket.receive()</code> blocks until a packet is
     * received or the socket times out.  Thus, if a timeout of zero is
     * set (which is the default, and denotes that the socket will never
     * time out), a datagram will never be sent unless one has just been
     * received.
     * <p>Thus the maximum time between datagram transmissions is the
     * socket timeout if no datagrams are being recieved.  If messages
     * are being received, available TX throughput will be increased.
     */
    @Override
    public void run() {
      paused  = false;
      running = true;

      while (running) {
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
          receiveDatagram();
        }
      }
    }

    /**
     * Extract the next buffer from the FIFO transmit list and send it
     * as a multicast datagram packet.
     */
    private void sendDatagram() {
      if (!paused && running && txList.size() > 0) try {
        byte[] bytes;
        synchronized(txList) {
          bytes = txList.get(0);
        }
        mSocket.send(new DatagramPacket(bytes, bytes.length, mAddress, mPort));
        Log.d(LOG_TAG, "transmitted "+bytes.length+" bytes");
        synchronized(txList) {
          txList.remove(0);
        }
      } catch (NullPointerException npe) {
        npe.printStackTrace();
        if (mListener != null) {
          mListener.onMulticastEvent(eventEnum.TX_FAIL, null, 0);
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
        if (mListener != null) {
          mListener.onMulticastEvent(eventEnum.TX_FAIL, null, 0);
        }
      }
    }
  }

  public void pause() {
    if (running) {
      paused = true;
      mSocket.disconnect();
      if (mLock != null) {
        try {
          mSocket.leaveGroup(mAddress);
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
        mLock.release();
      }
    }
  }

  /**
   * Set the software datagram receive filter value.  If the filter
   * value is FILTER_OFF, then received messages are all passed to the
   * registered listener(s).  Otherwise, all messages that don't have
   * the same value as the filter in the first byte of their datagram
   * payload will be discarded. 
   * @param newFilter - the new recieve filter value to use.
   */
  public void setFilter(byte newFilter) {
    filter = newFilter;
  }

  /**
   * Stop and <code>join()</code> the multicast thread.
   */
  private void stopThread() {
    paused  = false;
    running = false;
    if (mThread != null) {
      synchronized(mThread) {
        mThread.interrupt();
      }
    }
    /*
     * Close and join() the multicast thread.
     */
    boolean retry = true;
    while (retry && (mThread != null)) {
      try {
        mThread.join();
        retry = false;
      } catch (InterruptedException e) {
        /*
         * Keep trying to close the multicast thread.
         */
      }
    }
  }

  /**
   * Send the desired byte buffer as a multicast datagram packet.
   * @param buffer - the byte buffer to transmit.
   * @return <code>true</code> if the buffer was successfully added to
   * the outgoing datagram transmit list, <code>false</code> if the the
   * buffer was unable to be added to the transmit list.
   */
  public boolean transmit(byte[] buffer) {
    if ((mThread != null) && running) {
      synchronized(txList) {
        txList.add(buffer);
      }
      return true;
    }
    return false;
  }

  public void unPause() {
    paused = false;
    try {
      if (mLock != null) {
        mLock.acquire();
        mSocket.joinGroup(mAddress);
      }
      mSocket.bind   (new InetSocketAddress(mAddress, mPort));
      mSocket.connect(new InetSocketAddress(mAddress, mPort));
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    if (mThread != null) {
      synchronized(mThread) {
        mThread.interrupt();
      }
    }
  }
}
