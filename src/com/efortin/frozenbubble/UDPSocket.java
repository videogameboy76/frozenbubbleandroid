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
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.efortin.frozenbubble.NetworkManager.connectEnum;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * UDP socket class - implements UDP unicast and multicast datagram
 * sending and receiving.  This implementation currently only supports
 * IPv4 internet addresses.
 * <p>This class instantiates a thread to send and receive WiFi UDP
 * unicast or multicast messages.  The <code>MulticastSocket</code>
 * class is a descendant of <code>DatagramSocket</code>, the UDP
 * datagram socket class.  Thus a UDP socket can be used to send and
 * receive either UDP unicast datagrams, or UDP multicast datagrams,
 * which makes it convenient to use either peer to peer networking
 * scheme using virtually identical methods.
 * <p>Multicast host addresses must be in the IPv4 class D address
 * range, with the first octet being within the 224 to 239 range.
 * For example, <code>"225.0.0.15"</code> is an actual IPv4 multicast
 * address.
 * <p>Refer to:
 * <a href="url">http://en.wikipedia.org/wiki/Multicast_address</a> for
 * information regarding Multicast address usage and restrictions.
 * <p>A typical UDP implementation looks like this:
 * <pre><code>
 * UDPSocket session =
 *     new UDPSocket(context, mode, hostName, port);
 * session.setUDPListener(this);
 * </code></pre>
 * <p>The context from which to obtain the application context, the
 * connection mode, the IP address or host name of the UDP peer, and the
 * port of the UDP socket session must be supplied when creating a new
 * <code>UDPSocket</code> instance.
 * <p>The following <code>uses</code> permissions must be added to the
 * Android project manifest to access all the API functionality required
 * to perform UDP unicast or multicast networking as implemented:<br>
 * <code>ACCESS_NETWORK_STATE<br>
 * ACCESS_WIFI_STATE<br>
 * CHANGE_WIFI_MULTICAST_STATE<br>
 * INTERNET</code>
 * @author Eric Fortin, Wednesday, May 8, 2013
 */
public class UDPSocket {
  private static final String LOG_TAG = UDPSocket.class.getSimpleName();

  /*
   * MulticastManager class member variables.
   */
  private boolean     paused;
  private boolean     running;
  private int         mPort;
  private connectEnum mType;
  private ArrayList<byte[]>         txList        = null;
  private ArrayList<UDPListener>    listenerList  = null;
  private Context                   mContext      = null;
  private InetAddress               remoteAddress = null;
  private InetAddress               localAddress  = null;
  private MulticastSocket           mSocket       = null;
  private Thread                    mThread       = null;
  private WifiManager.MulticastLock mLock         = null;

  /*
   * Listener interface for various UDP socket events.
   *
   * This interface defines the abstract listener method that needs
   * to be instantiated by the registrar, as well as the various
   * events supported by the interface.
   */
  public interface UDPListener {
    public abstract void onUDPEvent(InetAddress address,
                                    byte[] buffer,
                                    int length);
  }

  public void setUDPListener(UDPListener listener) {
    listenerList.add(listener);
  }

  /**
   * UDP socket class constructor.
   * <p>When created, this class instantiates a thread to send and
   * receive WiFi UDP unicast or multicast messages.
   * <p>A typical implementation looks like this:
   * <pre><code>
   * UDPSocket session =
   *     new UDPSocket(context, mode, hostName, port);
   * session.setUDPListener(this);
   * </code></pre>
   * <p>The context from which to obtain the application context, the
   * connection mode, the host name of the UDP unicast peer or multicast
   * group to connect to, and the port of the UDP socket session must be
   * supplied when creating a new <code>UDPSocket</code> instance.
   * <p>The following <code>uses</code> permissions must be addeded to
   * the Android project manifest to perform UDP unicast networking:<br>
   * <code>INTERNET</code>
   * <p>Multicast host addresses must be in the IPv4 class D address
   * range, with the first octet being within the 224 to 239 range.
   * For example, <code>"225.0.0.15"</code> is an actual IPv4 multicast
   * address.
   * <p>Refer to:
   * <a href="url">http://en.wikipedia.org/wiki/Multicast_address</a>
   * for information regarding Multicast address usage and restrictions.
   * <p>A typical implementation looks like this:
   * <pre><code>
   * UDPSocket session =
   *     new UDPSocket(context, mode, hostName, port);
   * session.setUDPListener(this);
   * </code></pre>
   * <p>The context from which to obtain the application context, the
   * connection mode, the IP address of the UDP multicast session, and
   * the port of the UDP socket must be supplied when creating a new
   * <code>UDPSocket</code> instance.
   * <p>The following <code>uses</code> permissions must be added to the
   * Android project manifest to perform UDP multicast networking:<br>
   * <code>CHANGE_WIFI_MULTICAST_STATE<br>
   * INTERNET</code>
   * @param context - the context from which to obtain the application
   * context for the purpose of obtaining WiFi service access.
   * @param mode - the connection mode, either UNICAST or MULTICAST.
   * @param hostName - the host name of the UDP unicast peer or
   * multicast group.
   * @param port - the port number to use for the UDP socket.
   * @throws UnknownHostException the host name is invalid.
   * @throws IOException socket creation failed.
   */
  public UDPSocket(Context context, connectEnum mode, String hostName,
                   int port) throws UnknownHostException, IOException {
    mContext = context.getApplicationContext();
    mPort    = port;
    mLock    = null;
    mThread  = null;
    txList   = null;
    mType    = mode;
    configureUDPSocket(hostName, port);
    txList       = new ArrayList<byte[]>();
    listenerList = new ArrayList<UDPListener>();
    if (mode == connectEnum.UDP_MULTICAST) {
      WifiManager wm =
          (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
      mLock = wm.createMulticastLock("UDPLock");
      mLock.setReferenceCounted(true);
      mLock.acquire();
    }
    mThread = new Thread(new UDPThread(), "mThread");
    mThread.start();
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
    if (mSocket != null) {
      if (mSocket.isConnected()) {
        if (mType == connectEnum.UDP_MULTICAST) {
          try {
            mSocket.leaveGroup(remoteAddress);
          } catch (IOException ioe) {
            /*
             * Auto-generated catch block.
             */
            ioe.printStackTrace();
          }
        }
        else {
          mSocket.disconnect();
        }
      }
      mSocket.close();
    }
    mSocket = null;
    if (mLock != null) {
      if (mLock.isHeld()) {
        mLock.release();
      }
    }
    mLock = null;
    if (txList != null) {
      txList.clear();
    }
    txList = null;
  }

  /**
   * Configure the UDP socket settings.
   * <p>This must be called before <code>start()</code>ing the thread.
   * @param hostName - the IP address or host name of the UDP unicast
   * peer or multicast group.
   * @param port - the port number to use for the UDP socket.
   * @throws UnknownHostException the host name is invalid.
   * @throws IOException socket creation failed.
   */
  private void configureUDPSocket(String hostName, int port) throws
      UnknownHostException, IOException{
    remoteAddress = null;
    mSocket       = null;
    remoteAddress = InetAddress.getByName(hostName);
    mSocket       = new MulticastSocket(port);
    mSocket.setSoTimeout(101);
    mSocket.setBroadcast(false);
    mSocket.setLoopbackMode(true);
    mSocket.setReuseAddress(true);
    if (mType == connectEnum.UDP_MULTICAST) {
      mSocket.joinGroup(remoteAddress);
    }
  }

  /**
   * This is the UDP socket receive and transmit thread declaration.
   * <p>To support being able to send and receive packets in the same
   * thread, a nonzero socket read timeout must be set, because
   * <code>MulticastSocket.receive()</code> blocks until a packet is
   * received or the socket times out.  Thus, if a timeout of zero is
   * set (which is the default, and denotes that the socket will never
   * time out), a datagram will never be sent unless one has just been
   * received.
   * @author Eric Fortin, Wednesday, May 8, 2013
   * @see <code>configureUDPSocket()</code>
   */
  private class UDPThread implements Runnable {
    private byte[] rxBuffer = new byte[256];

    /**
     * Receive a UDP datagram.
     * <p>Given a nonzero socket timeout, it is expected behavior for
     * this method to catch an <code>InterruptedIOException</code>.
     * This method posts an <code>EVENT_PACKET_RX</code> event to the
     * registered listener upon datagram receipt.
     */
    private void receiveDatagram() {
      if (!paused && running) try {
        DatagramPacket dpRX = new DatagramPacket(rxBuffer, rxBuffer.length,
                                                 remoteAddress, mPort);
        mSocket.receive(dpRX);
        byte[]      buffer  = dpRX.getData();
        int         length  = dpRX.getLength();
        InetAddress address = dpRX.getAddress();

        if (!paused && running && (length != 0) && (address != localAddress) &&
            (listenerList != null)) {
          int size = listenerList.size();
          while (--size >= 0) {
            listenerList.get(size).onUDPEvent(address, buffer, length);
          }
          Log.d(LOG_TAG, "received "+length+" bytes from "+ remoteAddress +
              ": 0x" + bytesToHex(buffer, length));
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
     * <p>Send and receive UDP datagrams.
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
     * as a UDP datagram packet.
     */
    private void sendDatagram() {
      if (!paused && running && txList.size() > 0) try {
        byte[] bytes;
        synchronized(txList) {
          bytes = txList.get(0);
        }
        mSocket.send(
            new DatagramPacket(bytes, bytes.length, remoteAddress, mPort));
        Log.d(LOG_TAG, "transmitted "+bytes.length+" bytes to "+
            remoteAddress.getHostAddress() + ": 0x" +
            bytesToHex(bytes, bytes.length));
        synchronized(txList) {
          txList.remove(0);
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
      if (mSocket.isConnected()) {
        if (mType == connectEnum.UDP_MULTICAST) {
          try {
            mSocket.leaveGroup(remoteAddress);
          } catch (IOException ioe) {
            /*
             * Auto-generated catch block.
             */
            ioe.printStackTrace();
          }
        }
        else {
          mSocket.disconnect();
        }
        if (mLock != null) {
          if (mLock.isHeld()) {
            mLock.release();
          }
        }
      }
    }
  }

  public void setLocalIPaddress(InetAddress address) {
    localAddress = address;
  }

  /**
   * Stop and <code>join()</code> the UDP thread.
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
     * Close and join() the UDP thread.
     */
    boolean retry = true;
    while (retry && (mThread != null)) {
      try {
        mThread.join();
        retry = false;
      } catch (InterruptedException e) {
        /*
         * Keep trying to close the UDP thread.
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
        if (!mLock.isHeld()) {
          mLock.acquire();
        }
      }
      if (mType == connectEnum.UDP_MULTICAST) {
        try {
          mSocket.joinGroup(remoteAddress);
        } catch (SocketException se) {
          /*
           * Rejoining the multicast group will result in a
           * SocketException being thrown.  This occurs even if you
           * left the group before rejoining it.  This is expected
           * behavior.
           */
        }
      }
      else {
        /*
         * If the bind attempt throws a BindException, skip connecting
         * the socket as it is already bound.
         */
        try {
          mSocket.bind   (new InetSocketAddress(remoteAddress, mPort));
          mSocket.connect(new InetSocketAddress(remoteAddress, mPort));
        } catch(BindException be) {
          /*
           * If the socket was already bound to a port, it will throw a
           * BindException.  This is expected behavior.
           */
        }
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    if (mThread != null) {
      synchronized(mThread) {
        mThread.interrupt();
      }
    }
  }
};
