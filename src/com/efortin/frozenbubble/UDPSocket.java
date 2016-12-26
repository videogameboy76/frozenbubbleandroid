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

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.util.Log;

import com.efortin.frozenbubble.NetworkManager.connectEnum;

/**
 * User Datagram Protocol (UDP) internet socket class for Android -
 * implements UDP unicast, multicast, and broadcast datagram messaging
 * via internet sockets to send and receive data over the internet.
 * This implementation currently only supports IPv4 internet addresses.
 * <p><b>NOTE THAT MANY DEVICES DO NOT SUPPORT OR PROPERLY IMPLEMENT UDP
 * MULTICASTING OR BROADCASTING</b>
 * <p>This class instantiates two sockets serviced by two independent
 * threads to send and receive UDP unicasts, multicasts, or broadcasts
 * via a WiFi internet connection.  Since there are two sockets, one for
 * receiving and one for transmitting, they can be assigned to different
 * ports - note that the remote peer will have to have two sockets as
 * well with the same port assignments, only inverted with respect to
 * which port is bound by the receive versus transmit socket.  The
 * sockets are configured to enable the port reuse option, so multiple
 * sockets can be bound to the same port if desired to simplify the
 * implementation.
 * <p>The <code>MulticastSocket</code> class is a descendant of
 * <code>DatagramSocket</code>, the Java UDP datagram socket class, with
 * additional methods to support multicast address groups.  Thus a
 * <code>MulticastSocket</code> can be used to send and receive
 * messages using any addressing scheme, whether it be unicast,
 * multicast, or broadcast, which makes it convenient to implement every
 * possible peer to peer network addressing scheme using the same
 * wrapper and objects.
 * <p>Multicast host addresses must be in the IPv4 class D address
 * range, with the first octet being within the 224 to 239 range.
 * For example, <code>"225.0.0.15"</code> is an actual IPv4 multicast
 * address.
 * <p>Refer to:
 * <a href="url">http://en.wikipedia.org/wiki/Multicast_address</a> for
 * information regarding Multicast address usage and restrictions.
 * <p>A typical <code>UDPSocket</code> implementation looks like this:
 * <pre><code>
 * UDPSocket session =
 *     new UDPSocket(context, mode, localIpAddress, remoteIpAddress,
 *                   portRx, portTx, broadcast);
 * session.setUDPListener(this);
 * </code></pre>
 * The parameters supplied to the constructor are described as follows:
 * <br><code>localIpAddress</code> - the local IP address.
 * <br><code>remoteIpAddress</code> - the destination IP address.
 * <br><code>portRx</code> - the port number to use for the receive socket.
 * <br><code>portTx</code> - the port number to use for the transmit socket.
 * <br><code>broadcast</code> - <code>true</code> to enable TX broadcast.
 * <p>The following <code>uses</code> permissions must be added to the
 * Android project manifest to access all the Android API functionality
 * required to perform WiFi UDP internet networking as implemented:<br>
 * <code>ACCESS_NETWORK_STATE<br>
 * ACCESS_WIFI_STATE<br>
 * CHANGE_WIFI_MULTICAST_STATE<br>
 * INTERNET</code>
 * @author Eric Fortin, Wednesday, May 8, 2013
 */
public class UDPSocket {
  private static final String LOG_TAG    = UDPSocket.class.getSimpleName();
  public  static final int    RX_TIMEOUT = 101;

  /*
   * Private member variables.
   */
  private boolean paused;
  private boolean running;
  private int     mRxPort;
  private int     mTxPort;
  
  private ArrayList<byte[]>         txList          = null;
  private ArrayList<UDPListener>    listenerList    = null;
  private Context                   mContext        = null;
  private InetAddress               remoteAddressRX = null;
  private InetAddress               remoteAddressTX = null;
  private InetAddress               localAddress    = null;
  private MulticastSocket           mRxSocket       = null;
  private MulticastSocket           mTxSocket       = null;
  private Thread                    mRxThread       = null;
  private Thread                    mTxThread       = null;
  private WifiManager.MulticastLock mLock           = null;

  /*
   * Listener interface for various UDP events.
   *
   * This interface defines the abstract listener method that needs
   * to be instantiated by the event registrar.
   */
  public interface UDPListener {
    public abstract void onUDPEvent(InetAddress address,
                                    byte[]      buffer,
                                    int         length);
  }

  public void setUDPListener(UDPListener listener) {
    listenerList.add(listener);
  }

  /**
   * UDP socket class constructor.
   * <p>When created, this class instantiates threads to send and
   * receive WiFi UDP internet unicasts, multicasts, or broadcasts.
   * <p>A typical <code>UDPSocket</code> implementation looks like this:
   * <pre><code>
   * UDPSocket session =
   *     new UDPSocket(context, mode, localIpAddress, remoteIpAddress,
   *                   portRx, portTx, broadcast);
   * session.setUDPListener(this);
   * </code></pre>
   * The following <code>uses</code> permissions must be added to the
   * Android project manifest to perform WiFi internet networking:<br>
   * <code>CHANGE_WIFI_MULTICAST_STATE<br>
   * INTERNET</code>
   * @param context - the context from which to obtain the application
   * context for the purpose of obtaining WiFi service access.
   * @param mode - the connection mode, either <code>UDP_UNICAST</code>,
   * <code>UDP_MULTICAST</code>, or <code>UDP_BROADCAST</code>.
   * @param localIpAddress - the local IP address.
   * @param remoteIpAddress - the destination IP address.
   * @param portRx - the port number to use for the receive UDP socket.
   * @param portTx - the port number to use for the transmit UDP socket.
   * @param broadcast - <code>true</code> to enable TX broadcast.
   * @throws UnknownHostException the host name is invalid.
   * @throws IOException socket creation failed.
   */
  public UDPSocket(Context     context,
                   connectEnum mode,
                   InetAddress localIpAddress,
                   String      remoteIpAddress,
                   int         portRx,
                   int         portTx,
                   boolean     broadcast) throws UnknownHostException,
                                                 IOException {
    mContext     = context.getApplicationContext();
    mRxPort      = portRx;
    mTxPort      = portTx;
    mLock        = null;
    txList       = new ArrayList<byte[]>();
    txList.clear();
    listenerList = new ArrayList<UDPListener>();
    listenerList.clear();
    localAddress = localIpAddress;
    configureUDPSocket(remoteIpAddress, mode, portRx, portTx, broadcast);

    if (mode == connectEnum.UDP_MULTICAST) {
      WifiManager wm =
          (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
      mLock = wm.createMulticastLock("MulticastLock");
      mLock.setReferenceCounted(true);
      mLock.acquire();
    }

    paused    = false;
    running   = true;
    mRxThread = new Thread(new RxThread(), "mRxThread");
    mRxThread.start();
    mTxThread = new Thread(new TxThread(), "mTxThread");
    mTxThread.start();
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

    joinThreads();
    cleanUpSocket(mRxSocket);
    cleanUpSocket(mTxSocket);

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

  private void cleanUpSocket(MulticastSocket socket) {
    if (socket != null) {
      if (socket.isConnected()) {
        if (mLock != null) {
          try {
            socket.leaveGroup(remoteAddressTX);
          } catch (IOException ioe) {
            // TODO Auto-generated catch block
            ioe.printStackTrace();
          }
        }
        else {
          socket.disconnect();
        }
      }
      socket.close();
    }
    socket = null;
  }

  /**
   * Configure the UDP socket settings.
   * <p>This must be called before <code>start()</code>ing the threads.
   * @param remoteAddress - the IP address or host name of the remote
   * peer or multicast group.  If using <code>UDP_BROADCAST</code> mode,
   * then the first two octets of this IPv4 address are used to
   * construct the broadcast address.
   * @param portRx - the receive port number to use for the UDP socket.
   * @param portTx - the transmit port number to use for the UDP socket.
   * @throws UnknownHostException the host name is invalid.
   * @throws IOException socket creation failed.
   */
  private void configureUDPSocket(String      remoteAddress,
                                  connectEnum mode,
                                  int         portRx,
                                  int         portTx,
                                  boolean     broadcast) throws
      UnknownHostException, IOException {
    mRxSocket = null;
    mTxSocket = null;

    if (mode == connectEnum.UDP_BROADCAST) {
      remoteAddressTX = getBroadcastAddress(mContext, remoteAddress, 65535);
    }
    else {
      remoteAddressTX = InetAddress.getByName(remoteAddress);
    }

    mRxSocket = new MulticastSocket(portRx);
    mRxSocket.setSoTimeout   (RX_TIMEOUT);
    mRxSocket.setBroadcast   (broadcast);
    mRxSocket.setLoopbackMode(true);
    mRxSocket.setReuseAddress(true);
    mTxSocket = new MulticastSocket(portTx);
    mTxSocket.setSoTimeout   (RX_TIMEOUT);
    mTxSocket.setBroadcast   (broadcast);
    mTxSocket.setLoopbackMode(true);
    mTxSocket.setReuseAddress(true);

    if (mLock != null) {
      mRxSocket.joinGroup(remoteAddressTX);
      mTxSocket.joinGroup(remoteAddressTX);
    }
  }

  private void disconnectSocket(MulticastSocket socket) {
    if (socket != null) {
      if (socket.isConnected()) {
        if (mLock != null) {
          try {
            socket.leaveGroup(remoteAddressTX);
          } catch (IOException ioe) {
            // TODO Auto-generated catch block
            ioe.printStackTrace();
          }
        }
        else {
          socket.disconnect();
        }
      }
    }
  }

  /**
   * Get the broadcast address from the DHCP configuration info.
   * @param context - the application context.
   * @param address - the address to get the broadcast address from.
   * @param subnetMask - mask the DHCP subnet mask with this mask.
   * @return the broadcast address to transmit to.
   * @throws IOException
   */
  private InetAddress getBroadcastAddress(Context context,
                                          String  address,
                                          int     subnetMask) throws
      IOException {
    WifiManager wifi =
        (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    DhcpInfo    dhcp = wifi.getDhcpInfo();

    if (dhcp != null) {
      int subnet = dhcp.netmask & subnetMask;
      int broadcast = (dhcp.ipAddress & subnet) | ~subnet;
      byte[] quads = new byte[4];
      for (int k = 0; k < 4; k++) {
        quads[k] = (byte) (broadcast >> k * 8);
      }
      return InetAddress.getByAddress(quads);
    }
    else {
      return InetAddress.getByName(address);
    }
  }

  /**
   * <code>join()</code> a thread.
   * @param thread - the <code>Thread</code> object to
   * <code>join()</code>.
   */
  private void joinThread(Thread thread) {
    /*
     * Wake the thread.
     */
    if (thread != null) {
      synchronized(thread) {
        thread.interrupt();
      }
    }
    /*
     * Close and join() the thread.
     */
    boolean retry = true;

    while (retry && (thread != null)) {
      try {
        thread.join();
        retry = false;
      } catch (InterruptedException e) {
        /*
         * Keep trying to close the thread.
         */
      }
    }
  }

  /**
   * Stop and <code>join()</code> the UDP threads.
   */
  private void joinThreads() {
    paused  = false;
    running = false;
    joinThread(mRxThread);
    joinThread(mTxThread);
  }

  /**
   * This is the UDP socket receive thread declaration.
   * @author Eric Fortin, Wednesday, May 8, 2013
   * @see <code>configureUDPSocket()</code>
   */
  private class RxThread implements Runnable {
    private byte[] rxBuffer = new byte[256];

    /**
     * Receive a UDP datagram.
     * <p>Given a nonzero socket timeout, it is expected behavior for
     * this method to catch an <code>InterruptedIOException</code>.
     */
    private void receiveDatagram() {
      if (!paused && running) try {
        DatagramPacket dpRX = new DatagramPacket(rxBuffer,
                                                 rxBuffer.length,
                                                 remoteAddressTX,
                                                 mRxPort);

        if (mRxSocket != null) {
          mRxSocket.receive(dpRX);
        }

        byte[]      buffer  = dpRX.getData();
        int         length  = dpRX.getLength();
        InetAddress address = dpRX.getAddress();

        /*
         * If the received datagram is nonzero in length, message
         * listeners are registered with the UDP socket, and the address
         * received is not the local host indicating that this is not
         * message loopback, attempt to process the message.
         */
        if (!paused && running && (length != 0) &&
            (listenerList != null) && (!address.equals(localAddress))) {
          remoteAddressRX  = address;
          String addressRX = remoteAddressRX.getHostAddress();
          int    size      = listenerList   .size();

          while (--size >= 0) {
            listenerList.get(size).onUDPEvent(address, buffer, length);
          }

          Log.d(LOG_TAG, "received "+length+" bytes from "+ addressRX +
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
     * <p>Receive UDP datagrams.
     * <p><code>MulticastSocket.receive()</code> blocks until a packet
     * is received or the socket times out.  Thus, if a timeout of zero
     * is set (which is the default, and denotes that the socket will
     * never time out), the socket will block forever if there is no
     * remote peer communicating on the same port.
     */
    @Override
    public void run() {
      Looper.prepare();

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
          receiveDatagram();
        }
      }
    }
  }

  /**
   * This is the UDP socket transmit thread declaration.
   * @author Eric Fortin, Wednesday, May 8, 2013
   * @see <code>configureUDPSocket()</code>
   */
  private class TxThread implements Runnable {
    /**
     * This is the thread's <code>run()</code> call.
     * <p>Send UDP datagrams.
     */
    @Override
    public void run() {
      Looper.prepare();

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

        if (mTxSocket != null) {
          mTxSocket.send(new DatagramPacket(bytes,
                                            bytes.length,
                                            remoteAddressTX,
                                            mTxPort));
        }

        Log.d(LOG_TAG, "transmitted "+bytes.length+" bytes to "+
            remoteAddressTX.getHostAddress() + ": 0x" +
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
      disconnectSocket(mRxSocket);
      disconnectSocket(mTxSocket);
    }

    if (mLock != null) {
      if (mLock.isHeld()) {
        mLock.release();
      }
    }
  }

  private void reconnectSocket(MulticastSocket socket, int port) {
    if (socket != null) try {
      if (mLock != null) try {
        socket.joinGroup(remoteAddressTX);
      } catch (SocketException se) {
        /*
         * Rejoining the multicast group will result in a
         * SocketException being thrown.  This occurs even if you
         * left the group before rejoining it.  This is expected
         * behavior.
         */
      }
      else try {
      /*
       * If the bind attempt throws a BindException, skip connecting
       * the socket as it is already bound.
       */
        socket.bind   (new InetSocketAddress(remoteAddressTX, port));
        socket.connect(new InetSocketAddress(remoteAddressTX, port));
      } catch(BindException be) {
        /*
         * If the socket was already bound to a port, it will throw a
         * BindException.  This is expected behavior.
         */
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void setLocalIPaddress(InetAddress address) {
    localAddress = address;
  }

  /**
   * Send the desired byte buffer as a UDP datagram packet.
   * @param buffer - the byte buffer to transmit.
   * @return <code>true</code> if the buffer was successfully added to
   * the outgoing datagram transmit list, <code>false</code> if the the
   * buffer was unable to be added to the transmit list.
   */
  public boolean transmit(byte[] buffer) {
    if ((mTxThread != null) && running) {
      synchronized(txList) {
        txList.add(buffer);
      }
      return true;
    }
    return false;
  }

  public void unPause() {
    paused = false;

    if (mLock != null) {
      if (!mLock.isHeld()) {
        mLock.acquire();
      }
    }

    reconnectSocket(mRxSocket, mRxPort);
    reconnectSocket(mTxSocket, mTxPort);

    if (mRxThread != null) {
      synchronized(mRxThread) {
        mRxThread.interrupt();
      }
    }

    if (mTxThread != null) {
      synchronized(mTxThread) {
        mTxThread.interrupt();
      }
    }
  }
};
