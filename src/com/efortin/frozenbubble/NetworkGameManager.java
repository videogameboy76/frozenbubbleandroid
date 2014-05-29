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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.jfedor.frozenbubble.BubbleSprite;
import org.jfedor.frozenbubble.FrozenBubble;
import org.jfedor.frozenbubble.FrozenGame;
import org.jfedor.frozenbubble.GameView.NetGameInterface;
import org.jfedor.frozenbubble.LevelManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.efortin.frozenbubble.MulticastManager.MulticastListener;
import com.efortin.frozenbubble.MulticastManager.eventEnum;

/**
 * This class manages the actions in a network multiplayer game by
 * sending the local actions to the remote player, and queueing the
 * incoming remote player actions for enactment on the local machine.
 * <p>The thread created by this class will not <code>run()</code> until
 * <code>newGame()</code> is called.
 * <p>Attach <code>VirtualInput</code> objects to this manager for each
 * player in the network game.
 * @author Eric Fortin
 *
 */
public class NetworkGameManager extends Thread
  implements MulticastListener, NetGameInterface {
  private static final String MCAST_HOST_NAME = "225.0.0.15";
  private static final byte[] MCAST_BYTE_ADDR = { (byte) 225, 0, 0, 15 };
  private static final int    PORT            = 5500;

  /*
   * Message identifier definitions.
   */
  public static final byte MSG_ID_STATUS = 1;
  public static final byte MSG_ID_PREFS  = 2;
  public static final byte MSG_ID_ACTION = 3;
  public static final byte MSG_ID_FIELD  = 4;

  /*
   * Datagram size definitions.
   */
  public static final int  ACTION_BYTES = 37;
  public static final int  FIELD_BYTES  = 112;
  public static final int  PREFS_BYTES  = Preferences.PREFS_BYTES;
  public static final int  STATUS_BYTES = 14;

  /*
   * Network game management definitions.
   */
  private static final long ACTION_TIMEOUT     = 521L;
  private static final long GAME_START_TIMEOUT = 509L;
  private static final long STATUS_TIMEOUT     = 503L;
  private static final byte PROTOCOL_VERSION   = 1;
  private static final byte GAME_ID_MAX        = 100;

  /*
   * UDP unicast and multicast connection type enumeration. 
   */
  public static enum connectEnum {
    UDP_UNICAST,
    UDP_MULTICAST;
  }

  private byte             myGameID;
  private boolean          anyStatusRx;
  private boolean          gotFieldData;
  private boolean          gotPrefsData;
  private boolean[]        gamesInProgress;
  private boolean          missedAction;
  private boolean          paused;
  private boolean          running;
  private long             actionTxTime;
  private long             gameStartTime;
  private long             statusTxTime;
  private connectEnum      connectType;
  private String           localIpAddress = null;
  private String           remoteIpAddress = null;
  private Context          myContext = null;
  private PlayerStatus     localStatus = null;
  private PlayerStatus     remoteStatus = null;
  private Preferences      localPrefs = null;
  private Preferences      remotePrefs = null;
  private VirtualInput     localPlayer = null;
  private VirtualInput     remotePlayer = null;
  private GameFieldData    remoteGameFieldData = null;
  private PlayerAction     remotePlayerAction = null;
  private RemoteInterface  remoteInterface = null;
  private MulticastManager session = null;

  /*
   * Keep action lists for action retransmission requests and game
   * access.
   */
  private ArrayList<PlayerAction> localActionList = null;
  private ArrayList<PlayerAction> remoteActionList = null;

  /**
   * Class constructor.
   * @param myContext - the context from which to obtain the application
   * context to pass to the transport layer.
   * @param connectType - the transport layer connect type.
   * @param localPlayer - reference to the local player input object.
   * @param remotePlayer - reference to the remote player input object.
   * transport layer to create a socket connection.
   */
  public NetworkGameManager(Context myContext,
                            connectEnum connectType,
                            VirtualInput localPlayer,
                            VirtualInput remotePlayer) {
    this.myContext = myContext.getApplicationContext();
    this.connectType = connectType;
    this.localPlayer = localPlayer;
    this.remotePlayer = remotePlayer;
    /*
     * The game ID is used as the transport layer receive filter.  Do
     * not filter messages until we have obtained a game ID.
     */
    myGameID = MulticastManager.FILTER_OFF;
    anyStatusRx = false;
    gotFieldData = false;
    gotPrefsData = false;
    gamesInProgress = new boolean[GAME_ID_MAX];
    missedAction = false;
    localIpAddress = null;
    remoteIpAddress = null;
    localPrefs = new Preferences();
    remotePrefs = new Preferences();
    localStatus = null;
    remoteStatus = null;
    remoteGameFieldData = new GameFieldData(null);
    remotePlayerAction = new PlayerAction(null);
    remoteInterface = new RemoteInterface(remotePlayerAction,
                                          remoteGameFieldData);
    session = null;
    SharedPreferences sp =
        myContext.getSharedPreferences(FrozenBubble.PREFS_NAME,
                                       Context.MODE_PRIVATE);
    PreferencesActivity.getFrozenBubblePrefs(localPrefs, sp);
    /*
     * Create the player action arrays.  The actions are inserted
     * chronologically based on message receipt order, but are extracted
     * based on consecutive action ID.
     */
    localActionList  = new ArrayList<PlayerAction>();
    remoteActionList = new ArrayList<PlayerAction>();
    /*
     * Set the preference request flag to request the game option data
     * from the remote player.  If this player is player 1, then don't
     * request the preference data, since player 1's preferences are
     * used as the game preferences for all players.
     */
    boolean requestPrefs;
    if (localPlayer.playerID == VirtualInput.PLAYER1) {
      requestPrefs = false;
    }
    else {
      requestPrefs = true;
    }
    /*
     * Initialize the local status local action ID to zero, as it is
     * pre-incremented for every action transmitted to the remote
     * player.
     *
     * Initialize the local status remote action ID to 1, as it must be
     * the first action ID received from the remote player.
     */
    localStatus = new PlayerStatus((byte) localPlayer.playerID,
                                   (short) 0, (short) 1,
                                   false, false, false, requestPrefs,
                                   (short) 0, (short) 0);
  }

  /**
   * This class represents the current state of an individual player
   * game field.  The game field consists of the launcher bubbles, the
   * bubbles fixed to the game field, and the the attack bar.
   * @author Eric Fortin
   *
   */
  public class GameFieldData {
    public byte  playerID          = 0;
    public short localActionID     = 0;
    public byte  compressorSteps   = 0;
    public byte  launchBubbleColor = -1;
    public byte  nextBubbleColor   = -1;
    public short attackBarBubbles  = 0;
    /*
     * The game field is represented by a 2-dimensional array, with 8
     * rows and 13 columns.  This is displayed on the screen as 13 rows
     * with 8 columns.
     */
    public byte[][] gameField =
      {{ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
       { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
       { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
       { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
       { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
       { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
       { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
       { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 }};

    /**
     * Class constructor.
     * @param action - GameFieldData object to copy to this instance.
     */
    public GameFieldData(GameFieldData fieldData) {
      copyFromFieldData(fieldData);
    }

    /**
     * Class constructor.
     * @param buffer - buffer contents to copy to this instance.
     */
    public GameFieldData(byte[] buffer, int startIndex) {
      copyFromBuffer(buffer, startIndex);
    }

    /**
     * Copy the contents of the supplied field data to this field data.
     * @param action - the action to copy
     */
    public void copyFromFieldData(GameFieldData fieldData) {
      if (fieldData != null) {
        this.playerID          = fieldData.playerID;
        this.localActionID     = fieldData.localActionID;
        this.compressorSteps   = fieldData.compressorSteps;
        this.launchBubbleColor = fieldData.launchBubbleColor;
        this.nextBubbleColor   = fieldData.nextBubbleColor;
        this.attackBarBubbles  = fieldData.attackBarBubbles;

        for (int x = 0; x < LevelManager.NUM_COLS; x++) {
          for (int y = 0; y < LevelManager.NUM_ROWS; y++) {
            this.gameField[x][y] = fieldData.gameField[x][y];
          }
        }
      }
    }

    /**
     * Copy the contents of the buffer to this field data.
     * @param buffer - the buffer to convert and copy
     * @param startIndex - the start of the data to convert
     */
    public void copyFromBuffer(byte[] buffer, int startIndex) {
      byte[] shortBytes  = new byte[2];

      if (buffer != null) {
        this.playerID          = buffer[startIndex++];
        shortBytes[0]          = buffer[startIndex++];
        shortBytes[1]          = buffer[startIndex++];
        this.localActionID     = toShort(shortBytes);
        this.compressorSteps   = buffer[startIndex++];
        this.launchBubbleColor = buffer[startIndex++];
        this.nextBubbleColor   = buffer[startIndex++];
        shortBytes[0]          = buffer[startIndex++];
        shortBytes[1]          = buffer[startIndex++];
        this.attackBarBubbles  = toShort(shortBytes);

        for (int x = 0; x < LevelManager.NUM_COLS; x++) {
          for (int y = 0; y < LevelManager.NUM_ROWS; y++) {
            this.gameField[x][y] = buffer[startIndex++];
          }
        }
      }
    }

    /**
     * Copy the contents of this field data to the buffer.
     * @param buffer - the buffer to copy to
     * @param startIndex - the start location to copy to
     */
    public void copyToBuffer(byte[] buffer, int startIndex) {
      byte[] shortBytes  = new byte[2];

      if (buffer != null) {
        buffer[startIndex++] = this.playerID;
        toByteArray(this.localActionID, shortBytes);
        buffer[startIndex++] = shortBytes[0];
        buffer[startIndex++] = shortBytes[1];
        buffer[startIndex++] = this.compressorSteps;
        buffer[startIndex++] = this.launchBubbleColor;
        buffer[startIndex++] = this.nextBubbleColor;
        toByteArray(this.attackBarBubbles, shortBytes);
        buffer[startIndex++] = shortBytes[0];
        buffer[startIndex++] = shortBytes[1];

        for (int x = 0; x < LevelManager.NUM_COLS; x++) {
          for (int y = 0; y < LevelManager.NUM_ROWS; y++) {
            buffer[startIndex++] = this.gameField[x][y];
          }
        }
      }
    }
  };

  /**
   * This class encapsulates variables used to identify all possible
   * player actions.
   * @author Eric Fortin
   *
   */
  public class PlayerAction {
    public byte  playerID;        // player ID associated with this action
    public short localActionID;   // ID of this particular action
    public short remoteActionID;  // ID of expected remote player action
    /*
     * The following three booleans are flags associated with player
     * actions.
     *
     * compress -
     *   This flag indicates whether to lower the game field compressor.
     *
     * launchBubble -
     *   This flag indicates that the player desires a bubble launch to
     *   occur.  This flag must be set with a valid aimPosition value,
     *   as well as valid values for launchBubbleColor and
     *   nextBubbleColor.
     *
     * swapBubble -
     *   This flag indicates that the player desires that the current
     *   launch bubble be swapped with the next launch bubble.  This
     *   flag must be set with a valid aimPosition value, as well as
     *   valid values for launchBubbleColor and nextBubbleColor.
     */
    public boolean compress;
    public boolean launchBubble;
    public boolean swapBubble;
    public byte    keyCode;
    public byte    launchBubbleColor;
    public byte    nextBubbleColor;
    public byte    newNextBubbleColor;
    public short   attackBarBubbles;
    public byte    attackBubbles[] = { -1, -1, -1, -1, -1,
                                       -1, -1, -1, -1, -1,
                                       -1, -1, -1, -1, -1 };
    public double  aimPosition;

    /**
     * Class constructor.
     * @param action - PlayerAction object to copy to this instance.
     */
    public PlayerAction(PlayerAction action) {
      copyFromAction(action);
    }

    /**
     * Class constructor.
     * @param buffer - buffer contents to copy to this instance.
     */
    public PlayerAction(byte[] buffer, int startIndex) {
      copyFromBuffer(buffer, startIndex);
    }

    /**
     * Copy the contents of the supplied action to this action.
     * @param action - the action to copy.
     */
    public void copyFromAction(PlayerAction action) {
      if (action != null) {
        this.playerID           = action.playerID;
        this.localActionID      = action.localActionID;
        this.remoteActionID     = action.remoteActionID;
        this.compress           = action.compress;
        this.launchBubble       = action.launchBubble;
        this.swapBubble         = action.swapBubble;
        this.keyCode            = action.keyCode;
        this.launchBubbleColor  = action.launchBubbleColor;
        this.nextBubbleColor    = action.nextBubbleColor;
        this.newNextBubbleColor = action.newNextBubbleColor;
        this.attackBarBubbles   = action.attackBarBubbles;

        for (int index = 0; index < 15; index++) {
          this.attackBubbles[index] = action.attackBubbles[index];
        }

        this.aimPosition = action.aimPosition;
      }
    }

    /**
     * Copy the contents of the buffer to this action.
     * @param buffer - the buffer to convert and copy.
     * @param startIndex - the start of the data to convert.
     */
    public void copyFromBuffer(byte[] buffer, int startIndex) {
      byte[] shortBytes  = new byte[2];
      byte[] doubleBytes = new byte[8];

      if (buffer != null) {
        this.playerID           = buffer[startIndex++];
        shortBytes[0]           = buffer[startIndex++];
        shortBytes[1]           = buffer[startIndex++];
        this.localActionID      = toShort(shortBytes);
        shortBytes[0]           = buffer[startIndex++];
        shortBytes[1]           = buffer[startIndex++];
        this.remoteActionID     = toShort(shortBytes);
        this.compress           = buffer[startIndex++] == 1;
        this.launchBubble       = buffer[startIndex++] == 1;
        this.swapBubble         = buffer[startIndex++] == 1;
        this.keyCode            = buffer[startIndex++];
        this.launchBubbleColor  = buffer[startIndex++];
        this.nextBubbleColor    = buffer[startIndex++];
        this.newNextBubbleColor = buffer[startIndex++];
        shortBytes[0]           = buffer[startIndex++];
        shortBytes[1]           = buffer[startIndex++];
        this.attackBarBubbles   = toShort(shortBytes);

        for (int index = 0; index < 15; index++) {
          this.attackBubbles[index] = buffer[startIndex++];
        }

        for (int index = 0; index < 8; index++) {
          doubleBytes[index] = buffer[startIndex++];
        }

        this.aimPosition = toDouble(doubleBytes);
      }
    }

    /**
     * Copy the contents of this action to the buffer.
     * @param buffer - the buffer to copy to.
     * @param startIndex - the start location to copy to.
     */
    public void copyToBuffer(byte[] buffer, int startIndex) {
      byte[] shortBytes  = new byte[2];
      byte[] doubleBytes = new byte[8];

      if (buffer != null) {
        buffer[startIndex++] = this.playerID;
        toByteArray(this.localActionID, shortBytes);
        buffer[startIndex++] = shortBytes[0];
        buffer[startIndex++] = shortBytes[1];
        toByteArray(this.remoteActionID, shortBytes);
        buffer[startIndex++] = shortBytes[0];
        buffer[startIndex++] = shortBytes[1];
        buffer[startIndex++] = (byte) ((this.compress == true)?1:0);
        buffer[startIndex++] = (byte) ((this.launchBubble == true)?1:0);
        buffer[startIndex++] = (byte) ((this.swapBubble == true)?1:0);
        buffer[startIndex++] = this.keyCode;
        buffer[startIndex++] = this.launchBubbleColor;
        buffer[startIndex++] = this.nextBubbleColor;
        buffer[startIndex++] = this.newNextBubbleColor;
        toByteArray(this.attackBarBubbles, shortBytes);
        buffer[startIndex++] = shortBytes[0];
        buffer[startIndex++] = shortBytes[1];

        for (int index = 0; index < 15; index++) {
          buffer[startIndex++] = this.attackBubbles[index];
        }

        toByteArray(this.aimPosition, doubleBytes);

        for (int index = 0; index < 8; index++) {
          buffer[startIndex++] = doubleBytes[index];
        }
      }
    }
  };

  /**
   * This class encapsulates variables used to indicate the local game
   * and player status, and is used to synchronize the exchange of
   * information over the network.
   * <p>This data is intended to be send periodically to the remote
   * player(s) to keep all the players synchronized and informed of
   * potential network issues with lost datagrams.  This is especially
   * common with multicasting, which is implemented via the User
   * Datagram Protocol (UDP), which is unreliable.<br>
   * Refer to:
   * <a href="url">http://en.wikipedia.org/wiki/User_Datagram_Protocol</a>
   * @author Eric Fortin
   *
   */
  public class PlayerStatus {
    /*
     * The following ID is the player associated with this status.
     */
    public byte playerID;
    public byte protocolVersion;
    /*
     * The following action IDs represent the associated player's
     * current game state.  localActionID will refer to that player's
     * last transmitted action identifer, and remoteActionID will refer
     * to that player's pending action identifier (the action it is 
     * expecting to receive next).
     *
     * This is useful for noting if a player has missed player action
     * datagrams from another player, because its remoteActionID will be
     * less than or equal to the localActionID of the other player if it
     * has not received all the action transmissions from the other
     * player(s). 
     */
    public short localActionID;
    public short remoteActionID;
    /*
     * The following flags are used to manage game synchronization.
     */
    public boolean readyToPlay;
    public boolean gameWonLost;
    /*
     * The following flags are used to request data from the remote
     * player(s) - either their game preferences, or game field data.
     * When one or either of these flags is true, then the other
     * player(s) shall transmit the appropriate information.
     */
    private boolean fieldRequest;
    private boolean prefsRequest;
    /*
     * The following values are the bubble grid CRC16 values for the
     * local and remote game fields.  When the CRC16 value is zero, the
     * CRC16 value has not been calculated (or improbably, is zero).
     */
    public short localChecksum;
    public short remoteChecksum;

    /**
     * Class constructor.
     * @param id - the player ID associated with this status
     * @param localId - the local last transmitted action ID.
     * @param remoteId - the remote current pending action ID.
     * @param ready - player is ready to play flag.
     * @param wonLost - player won or lost the game.
     * @param field - request field data.
     * @param prefs - request preference data.
     * @param localCRC - the local player bubble grid CRC16 checksum.
     * @param remoteCRC - the remote player bubble grid CRC16 checksum.
     */
    public PlayerStatus(byte id,
                        short localId,
                        short remoteId,
                        boolean ready,
                        boolean wonLost,
                        boolean field,
                        boolean prefs,
                        short localCRC,
                        short remoteCRC) {
      init(id, localId, remoteId, ready, wonLost, field, prefs,
           localCRC, remoteCRC);
    }

    /**
     * Class constructor.
     * @param action - PlayerAction object to copy to this instance.
     */
    public PlayerStatus(PlayerStatus status) {
      copyFromStatus(status);
    }

    /**
     * Class constructor.
     * @param buffer - buffer contents to copy to this instance.
     */
    public PlayerStatus(byte[] buffer, int startIndex) {
      copyFromBuffer(buffer, startIndex);
    }

    /**
     * Copy the contents of the supplied action to this action.
     * @param action - the action to copy.
     */
    public void copyFromStatus(PlayerStatus status) {
      if (status != null) {
        this.playerID        = status.playerID;
        this.protocolVersion = PROTOCOL_VERSION;
        this.localActionID   = status.localActionID;
        this.remoteActionID  = status.remoteActionID;
        this.readyToPlay     = status.readyToPlay;
        this.gameWonLost     = status.gameWonLost;
        this.fieldRequest    = status.fieldRequest;
        this.prefsRequest    = status.prefsRequest;
        this.localChecksum   = status.localChecksum;
        this.remoteChecksum  = status.remoteChecksum;
      }
    }

    /**
     * Copy the contents of the buffer to this status.
     * @param buffer - the buffer to convert and copy.
     * @param startIndex - the start of the data to convert.
     */
    public void copyFromBuffer(byte[] buffer, int startIndex) {
      byte[] shortBytes  = new byte[2];

      if (buffer != null) {
        this.playerID        = buffer[startIndex++];
        this.protocolVersion = buffer[startIndex++];
        shortBytes[0]        = buffer[startIndex++];
        shortBytes[1]        = buffer[startIndex++];
        this.localActionID   = toShort(shortBytes);
        shortBytes[0]        = buffer[startIndex++];
        shortBytes[1]        = buffer[startIndex++];
        this.remoteActionID  = toShort(shortBytes);
        this.readyToPlay     = buffer[startIndex++] == 1;
        this.gameWonLost     = buffer[startIndex++] == 1;
        this.fieldRequest    = buffer[startIndex++] == 1;
        this.prefsRequest    = buffer[startIndex++] == 1;
        shortBytes[0]        = buffer[startIndex++];
        shortBytes[1]        = buffer[startIndex++];
        this.localChecksum   = toShort(shortBytes);
        shortBytes[0]        = buffer[startIndex++];
        shortBytes[1]        = buffer[startIndex++];
        this.remoteChecksum  = toShort(shortBytes);
      }
    }

    /**
     * Copy the contents of this status to the buffer.
     * @param buffer - the buffer to copy to.
     * @param startIndex - the start location to copy to.
     */
    public void copyToBuffer(byte[] buffer, int startIndex) {
      byte[] shortBytes  = new byte[2];

      if (buffer != null) {
        buffer[startIndex++] = this.playerID;
        buffer[startIndex++] = this.protocolVersion;
        toByteArray(this.localActionID, shortBytes);
        buffer[startIndex++] = shortBytes[0];
        buffer[startIndex++] = shortBytes[1];
        toByteArray(this.remoteActionID, shortBytes);
        buffer[startIndex++] = shortBytes[0];
        buffer[startIndex++] = shortBytes[1];
        buffer[startIndex++] = (byte) ((this.readyToPlay == true)?1:0);
        buffer[startIndex++] = (byte) ((this.gameWonLost == true)?1:0);
        buffer[startIndex++] = (byte) ((this.fieldRequest == true)?1:0);
        buffer[startIndex++] = (byte) ((this.prefsRequest == true)?1:0);
        toByteArray(this.localChecksum, shortBytes);
        buffer[startIndex++] = shortBytes[0];
        buffer[startIndex++] = shortBytes[1];
        toByteArray(this.remoteChecksum, shortBytes);
        buffer[startIndex++] = shortBytes[0];
        buffer[startIndex++] = shortBytes[1];
      }
    }

    /**
     * Initialize this object with the provided data.
     * @param id - the player ID associated with this status
     * @param localId - the local last transmitted action ID.
     * @param remoteId - the remote current pending action ID.
     * @param ready - player is ready to play.
     * @param wonLost - player won or lost the game.
     * @param field - request field data
     * @param prefs - request preference data
     * @param localCRC - the local player bubble grid CRC16 checksum.
     * @param remoteCRC - the remote player bubble grid CRC16 checksum.
     */
    public void init(byte id,
                     short localId,
                     short remoteId,
                     boolean ready,
                     boolean wonLost,
                     boolean field,
                     boolean prefs,
                     short localCRC,
                     short remoteCRC) {
      this.playerID        = id;
      this.protocolVersion = PROTOCOL_VERSION;
      this.localActionID   = localId;
      this.remoteActionID  = remoteId;
      this.readyToPlay     = ready;
      this.gameWonLost     = wonLost;
      this.fieldRequest    = field;
      this.prefsRequest    = prefs;
      this.localChecksum   = localCRC;
      this.remoteChecksum  = remoteCRC;
    }
  };

  private boolean actionTimerExpired() {
    return System.currentTimeMillis() >= actionTxTime;
  }

  /**
   * Add a player action to the appropriate action list.  Do not allow
   * duplicate actions to populate the lists.
   * @param newAction - the action to add to the appropriate list.
   */
  private void addAction(PlayerAction newAction) {
    if ((localPlayer != null) && (remotePlayer != null)) {
      /*
       * If an action is a local player action, add it to the action
       * list if it is not already in the list.
       *
       * If it is a remote player action, add it to the action list if
       * it is not already in the list.
       */
      if (newAction.playerID == localPlayer.playerID) {
        synchronized(localActionList) {
          int listSize = localActionList.size();
  
          for (int index = 0; index < listSize; index++) {
            /*
             * If a match is found, return from this function without
             * adding the action to the list since it is a duplicate.
             */
            if (localActionList.get(index).localActionID ==
                newAction.localActionID) {
              return;
            }
          }
          localActionList.add(newAction);
        }
      }
      else if (newAction.playerID == remotePlayer.playerID) {
        synchronized(remoteActionList) {
          int listSize = remoteActionList.size();
          /*
           * Update the remote player remote action ID to the ID of this
           * action if it is exactly 1 greater than the local player
           * local action ID.  This signifies that the remote player has
           * received all the local player action messages, since they
           * are expecting an action datagram that has not yet been sent
           * by the local player because it hasn't occurred.
           */
          if (newAction.remoteActionID ==
              localStatus.localActionID + 1) {
            remoteStatus.remoteActionID = newAction.remoteActionID;
          }
  
          for (int index = 0; index < listSize; index++) {
            /*
             * If a match is found, return from this function without
             * adding the action to the list since it is a duplicate.
             */
            if (remoteActionList.get(index).localActionID ==
                newAction.localActionID) {
              return;
            }
          }
          /*
           * Clear the list when the first action is received to remove
           * spurious entries.
           */
          if (newAction.localActionID == 1) {
            remoteActionList.clear();
          }
          remoteActionList.add(newAction);
        }
      }
    }
  }

  public void checkRemoteChecksum() {
    if ((localStatus != null) && (remoteStatus != null)) {
      if ((localStatus.remoteActionID == (remoteStatus.localActionID + 1)) &&
          (localStatus.remoteChecksum != 0) &&
          (remoteStatus.localChecksum != 0) &&
          (localStatus.remoteChecksum != remoteStatus.localChecksum)) {
        localStatus.fieldRequest = true;
      }
    }
  }

  /**
   * Check the local action list for actions that have been received
   * by the remote player.  When this is the case, the local action
   * list entries will have action IDs lower than the remote player
   * remote action ID.
   * <p>Each call of this function will remove one entry.
   * @return <code>true</code> if an entry was removed.
   */
  private boolean cleanLocalActionList() {
    boolean removed = false;
    synchronized(localActionList) {
      int listSize = localActionList.size();
  
      for (int index = 0; index < listSize; index++) {
        /*
         * If the local action ID in the list is less than the remote
         * player remote action ID, remove it from the list.  Only one
         * entry is removed per function call.
         */
        if (localActionList.get(index).localActionID <
            remoteStatus.remoteActionID) {
          localActionList.remove(index);
          removed = true;
          break;
        }
      }
    }
    return removed;
  }

  public void cleanUp() {
    stopThread();

    if (session != null)
      session.cleanUp();
    session = null;

    /*
     * Restore the local game preferences in the event that they were
     * overwritten by the remote player's preferences.
     */
    if (localPrefs != null) {
      SharedPreferences sp =
          myContext.getSharedPreferences(FrozenBubble.PREFS_NAME,
                                         Context.MODE_PRIVATE);
      PreferencesActivity.setFrozenBubblePrefs(localPrefs, sp);
    }

    localPrefs = null;
    remotePrefs = null;
    localPlayer = null;
    remotePlayer = null;
    remoteGameFieldData = null;
    remotePlayerAction = null;

    if (remoteInterface != null)
      remoteInterface.cleanUp();
    remoteInterface = null;

    if (localActionList != null)
      localActionList.clear();
    localActionList = null;

    if (remoteActionList != null)
      remoteActionList.clear();
    remoteActionList = null;
  }

  /**
   * Copy the contents of the buffer to the designated preferences.
   * @param buffer - the buffer to convert and copy.
   * @param startIndex - the start of the data to convert.
   */
  private void copyPrefsFromBuffer(Preferences prefs,
                                   byte[] buffer,
                                   int startIndex) {
    byte[] intBytes = new byte[4];

    if (buffer != null) {
      intBytes[0]      = buffer[startIndex++];
      intBytes[1]      = buffer[startIndex++];
      intBytes[2]      = buffer[startIndex++];
      intBytes[3]      = buffer[startIndex++];
      prefs.collision  = toInt(intBytes);
      prefs.compressor = buffer[startIndex++] == 1;
      intBytes[0]      = buffer[startIndex++];
      intBytes[1]      = buffer[startIndex++];
      intBytes[2]      = buffer[startIndex++];
      intBytes[3]      = buffer[startIndex++];
      prefs.difficulty = toInt(intBytes);
      prefs.dontRushMe = buffer[startIndex++] == 1;
      prefs.fullscreen = buffer[startIndex++] == 1;
      intBytes[0]      = buffer[startIndex++];
      intBytes[1]      = buffer[startIndex++];
      intBytes[2]      = buffer[startIndex++];
      intBytes[3]      = buffer[startIndex++];
      prefs.gameMode   = toInt(intBytes);
      prefs.musicOn    = buffer[startIndex++] == 1;
      prefs.soundOn    = buffer[startIndex++] == 1;
      intBytes[0]      = buffer[startIndex++];
      intBytes[1]      = buffer[startIndex++];
      intBytes[2]      = buffer[startIndex++];
      intBytes[3]      = buffer[startIndex++];
      prefs.targetMode = toInt(intBytes);
    }
  }

  /**
   * Copy the contents of this preferences object to the buffer.
   * @param buffer - the buffer to copy to.
   * @param startIndex - the start location to copy to.
   */
  private void copyPrefsToBuffer(Preferences prefs,
                                 byte[] buffer,
                                 int startIndex) {
    byte[] intBytes = new byte[4];

    if (buffer != null) {
      toByteArray(prefs.collision, intBytes);
      buffer[startIndex++] = intBytes[0];
      buffer[startIndex++] = intBytes[1];
      buffer[startIndex++] = intBytes[2];
      buffer[startIndex++] = intBytes[3];
      buffer[startIndex++] = (byte) ((prefs.compressor == true)?1:0);
      toByteArray(prefs.difficulty, intBytes);
      buffer[startIndex++] = intBytes[0];
      buffer[startIndex++] = intBytes[1];
      buffer[startIndex++] = intBytes[2];
      buffer[startIndex++] = intBytes[3];
      buffer[startIndex++] = (byte) ((prefs.dontRushMe == true)?1:0);
      buffer[startIndex++] = (byte) ((prefs.fullscreen == true)?1:0);
      toByteArray(prefs.gameMode, intBytes);
      buffer[startIndex++] = intBytes[0];
      buffer[startIndex++] = intBytes[1];
      buffer[startIndex++] = intBytes[2];
      buffer[startIndex++] = intBytes[3];
      buffer[startIndex++] = (byte) ((prefs.musicOn == true)?1:0);
      buffer[startIndex++] = (byte) ((prefs.soundOn == true)?1:0);
      toByteArray(prefs.targetMode, intBytes);
      buffer[startIndex++] = intBytes[0];
      buffer[startIndex++] = intBytes[1];
      buffer[startIndex++] = intBytes[2];
      buffer[startIndex++] = intBytes[3];
    }
  }

  /**
   * Check if the network game is finished.  The game is finished when
   * each player has either won or lost the game.
   */
  public boolean getGameIsFinished() {
    if (remoteStatus == null)
      return false;
    else
      return localStatus.gameWonLost && remoteStatus.gameWonLost;
  }

  /**
   * Check if the network game is ready for action.  The game is ready
   * to begin play when all data synchronization tasks are completed, at
   * which point every respective player's readyToPlay flag will be set.
   * @return <code>true</code> if game synchronization is complete.
   */
  public boolean gameIsReadyForAction() {
    if (remoteStatus == null)
      return false;
    else
      return localStatus.readyToPlay && remoteStatus.readyToPlay;
  }

  private boolean gameStartTimerExpired() {
    return System.currentTimeMillis() >= gameStartTime;
  }

  private void getGameFieldData(GameFieldData gameData) {
    FrozenGame gameRef = localPlayer.mGameRef;

    gameData.playerID           = (byte)  localPlayer.playerID;
    gameData.localActionID      =         localStatus.localActionID;
    gameData.compressorSteps    = (byte)  gameRef.getCompressorSteps();
    gameData.launchBubbleColor  = (byte)  gameRef.getCurrentColor();
    gameData.nextBubbleColor    = (byte)  gameRef.getNextColor();
    gameData.attackBarBubbles   = (short) gameRef.getAttackBarBubbles();

    BubbleSprite[][] bubbleGrid = gameRef.getGrid();
    for (int i = 0; i < LevelManager.NUM_COLS; i++) {
      for (int j = 0; j < LevelManager.NUM_ROWS; j++) {
        if (bubbleGrid[i][j] != null) {
          gameData.gameField[i][j] = (byte) bubbleGrid[i][j].getColor();
        }
        else {
          gameData.gameField[i][j] = -1;
        }
      }
    }
  }

  public short getLatestRemoteActionId() {
    if (remoteStatus != null) {
      return remoteStatus.localActionID;
    }
    else {
      return -1;
    }
  }

  /**
   * Peek into the remote action list to see if we have obtained the
   * current expected remote action.
   * @return The reference to the current remote action if it exists,
   * and <code>null</code> if we haven't received it yet.
   */
  public PlayerAction getRemoteActionPreview() {
    PlayerAction tempAction = null;

    synchronized(remoteActionList) {
      int listSize = remoteActionList.size();
  
      for (int index = 0; index < listSize; index++) {
        /*
         * When a match is found, return a reference to it.
         */
        if (remoteActionList.get(index).localActionID ==
            localStatus.remoteActionID) {
          tempAction = new PlayerAction(remoteActionList.get(index));
          break;
        }
      }
    }

    return tempAction;
  }

  /**
   * This function obtains the expected remote player action (based on
   * action ID) and places it into the remote player interface.
   * <p>This function must be called periodically as it is assumed
   * that the actions will be performed at the most appropriate time as
   * determined by caller.
   * @return <code>true</code> if the appropriate remote player action
   * was retrieved from the remote action list.
   */
  public boolean getRemoteAction() {
    remoteInterface.gotAction = false;
    synchronized(remoteActionList) {
      int listSize = remoteActionList.size();
  
      for (int index = 0; index < listSize; index++) {
        /*
         * When a match is found, copy the necessary element from the
         * list, remove it, and exit the loop.
         */
        if (remoteActionList.get(index).localActionID ==
            localStatus.remoteActionID) {
          remoteInterface.playerAction.copyFromAction(remoteActionList.get(index));
          try {
              remoteActionList.remove(index);
          } catch (IndexOutOfBoundsException ioobe) {
            ioobe.printStackTrace();
          }
          remoteInterface.gotAction = true;
          localStatus.remoteActionID++;
          /*
           * The local player status was updated.  Set the status
           * timeout to expire immediately and wake up the network
           * manager thread.
           */
          setStatusTimeout(0L);
          synchronized(this) {
            notify();
          }
          break;
        }
      }
    }

    return remoteInterface.gotAction;
  }

  private String getRemoteAddress() {
    if (connectType == connectEnum.UDP_UNICAST) {
      SharedPreferences dsp = 
          PreferenceManager.getDefaultSharedPreferences(myContext);
      remoteIpAddress = dsp.getString("opponent_ip_address", null);
    }
    else
      remoteIpAddress = MCAST_HOST_NAME;

    return remoteIpAddress;
  }

  /**
   * This function obtains the remote player interface and returns a
   * reference to it to the caller.
   * @return A reference to the remote player network game interface
   * which provides all necessary remote player data.
   */
  public RemoteInterface getRemoteInterface() {
    return remoteInterface;
  }

  /**
   * This function is called from manager thread's <code>run()</code>
   * method.  This performs the network handshaking amongst peers to
   * ensure proper game synchronization and operation.
   */
  private void manageNetworkGame() {
    if (localStatus == null) {
      return;
    }
    /*
     * If the game ID has not been reserved, check the current games in
     * progress for an available game ID to reserve.
     */
    if (myGameID == MulticastManager.FILTER_OFF) {
      if (gameStartTimerExpired()) {
        /*
         * Don't reserve a game ID if we've never received a status
         * message from another player.  Either we are having network
         * issues and may inadvertently reserve an already reserved ID,
         * or there isn't even any one else to play with on the network.
         */
        if (anyStatusRx) {
          reserveGameID();
        }
        else {
          setGameStartTimeout(GAME_START_TIMEOUT);
        }
      }
    }

    if (remoteStatus != null) {
      /*
       * On a new game, wait for the remote player to start a new game
       * before requesting field data from the remote player.
       */
      if (!gotFieldData &&
          !localStatus.fieldRequest &&
          !remoteStatus.readyToPlay) {
        localStatus.fieldRequest = true;
      }
      /*
       * If the last action transmitted by the local player has not yet
       * been received by the remote player, the remote player remote
       * action ID will match or be less than the local player local
       * action ID.  If this is the case, transmit the action ID
       * expected by the remote player.
       */
      if (localStatus.localActionID >= remoteStatus.remoteActionID) {
        if (!missedAction) {
          missedAction = true;
          setActionTimeout(ACTION_TIMEOUT);
        }
        else if (actionTimerExpired()) {
          sendLocalPlayerAction(remoteStatus.remoteActionID);
          setActionTimeout(ACTION_TIMEOUT);
        }
      }
      else {
        missedAction = false;
      }

      cleanLocalActionList();
    }

    /*
     * Check whether various datagrams require transmission, such as
     * player status, game field data, or preferences.
     */
    if (statusTimerExpired()) {
      if (remoteStatus != null) {
        if (remoteStatus.prefsRequest) {
          transmitPrefs();
          /*
           * Clear the remote request flag to potentially reduce network
           * overhead.  If the remote player does not receive the data,
           * the next remote status message will set the flag again.
           */
          remoteStatus.prefsRequest = false;
        }

        /*
         * Only transmit the local game field if the local player local
         * action ID is one less than the remote player remote action
         * ID.  This signifies that the distributed game is synchronized
         * with respect to the local player.  Actions must be executed
         * synchronously with respect to the corresponding game field in
         * order for performance to be identical on each distributed
         * device.
         */
        if (remoteStatus.fieldRequest &&
            ((localStatus.localActionID + 1) == remoteStatus.remoteActionID)) {
          GameFieldData tempField = new GameFieldData(null);
          getGameFieldData(tempField);
          transmitGameField(tempField);
          /*
           * Clear the remote request flag to potentially reduce network
           * overhead.  If the remote player does not receive the data,
           * the next remote status message will set the flag again.
           */
          remoteStatus.fieldRequest = false;
        }
      }

      transmitStatus(localStatus);
      setStatusTimeout(STATUS_TIMEOUT);
    }
  }

  public void newGame() {
    gotFieldData = false;
    if (localStatus != null) {
      localStatus.readyToPlay    = false;
      localStatus.localActionID  = 0;
      localStatus.remoteActionID = 1;
      localStatus.localChecksum  = 0;
      localStatus.remoteChecksum = 0;
    }
    if (localActionList != null) {
      synchronized(localActionList) {
        localActionList.clear();
      }
    }
    if (remoteActionList != null) {
      synchronized(remoteActionList) {
        remoteActionList.clear();
      }
    }
    /*
     * Initialize the various timers.
     */
    setActionTimeout(0L);
    setGameStartTimeout(GAME_START_TIMEOUT);
    setStatusTimeout(0L);
    /*
     * If a UDP session has not yet been created, create a new one and
     * start the <code>NetworkGameManager</code> thread.
     */
    if (session == null) {
      try {
        if (connectType == connectEnum.UDP_UNICAST) {
          session = new MulticastManager(myContext, getRemoteAddress(), PORT);
        }
        else {
          session = new MulticastManager(myContext, MCAST_BYTE_ADDR, PORT);
        }
        session.setMulticastListener(this);
      } catch (UnknownHostException uhe) {
        if (session != null) {
          session.cleanUp();
        }
        session = null;
      } catch (IOException ioe) {
        if (session != null) {
          session.cleanUp();
        }
        session = null;
      }
      /*
       * Start the network manager thread.
       */
      try {
        start();
      } catch (IllegalThreadStateException itse) {
        /*
         * The thread was already started.
         */
        itse.printStackTrace();
      }
    }
    else {
      /*
       * Wake up the thread.
       */
      synchronized(this) {
        notify();
      }
    }
  }

  @Override
  public void onMulticastEvent(eventEnum event, byte[] buffer, int length) {
    /*
     * Process the multicast message if it is a successfully received
     * datagram that possesses a payload.
     */
    if ((event == eventEnum.PACKET_RX) && (buffer != null)) {
      /*
       * The first three bytes of every message must contain the same
       * three fields - the game ID, the message ID, and the player ID.
       * The game ID and message ID are prefixed prior to each datagram
       * transmission, as they are used by the network layer and are of
       * no significance to any other module.  The player ID must be the
       * first byte of every datagram class implemented by the Frozen
       * Bubble network game protocol.
       *
       * The game ID is used to filter messages so that only players
       * with the same game ID will process each others' messages - all
       * other messages are discarded.  The exception is when the game
       * ID is -1, which means that the player is attempting to join a
       * game, so all incoming messages are processed until an
       * unreserved game ID is found.
       *
       * The message ID is used to identify the message type - either a
       * player status datagram, game field datagram, player preferences
       * datagram, or a player action datagram.
       *
       * The player ID is used to identify who originated the message.
       * This must be unique amongst all the players using the same game
       * ID.
       */
      byte gameId   = buffer[0];
      byte msgId    = buffer[1];
      byte playerId = buffer[2];

      /*
       * If the message contains the remote player status, copy it to
       * the remote player status object.  The remote player status
       * object will be null until the first remote status datagram is
       * received.
       *
       * If the game ID has not yet been set, then player status
       * messages are the only messages that will be processed.
       */
      if ((msgId == MSG_ID_STATUS) && (length == (STATUS_BYTES + 2))) {
        anyStatusRx = true;
        /*
         * Perform game ID checking; otherwise process the remote player
         * player status.
         */
        if (myGameID == MulticastManager.FILTER_OFF) {
          PlayerStatus tempStatus = new PlayerStatus(buffer, 2);
          /*
           * If we receive a status from a game already in progress,
           * mark it and bump the game start timer.
           *
           * If we receive a status with the filter mask off and we have
           * the same player ID as the player ID in that status, mark
           * that game as already in progress and bump game start timer.
           */
          if (tempStatus.readyToPlay ||
              ((gameId != MulticastManager.FILTER_OFF) &&
               (playerId == localPlayer.playerID))) {
            if ((gameId >= 0) && (gameId < GAME_ID_MAX)) {
              if (gamesInProgress[gameId] == false) {
                gamesInProgress[gameId] = true;
                setGameStartTimeout(GAME_START_TIMEOUT);
              }
            }
          }
        }
        else if ((gameId != MulticastManager.FILTER_OFF) &&
                 (playerId == remotePlayer.playerID)) {
          if (remoteStatus == null) {
            remoteStatus = new PlayerStatus(buffer, 2);
          }
          else {
            remoteStatus.copyFromBuffer(buffer, 2);
          }
        }
      }

      if (myGameID != MulticastManager.FILTER_OFF) {
        /*
         * If the message contains game preferences from player 1, then
         * update the game preferences.  The game preferences for all
         * players are set per player 1.
         */
        if ((msgId == MSG_ID_PREFS) && (length == (PREFS_BYTES + 3))) {
          if ((playerId == VirtualInput.PLAYER1) && localStatus.prefsRequest) {
            copyPrefsFromBuffer(remotePrefs, buffer, 3);
            /*
             * In a network game, do not override any of the local game
             * options that can be configuring during game play.  Only 
             * set the bubble collision sensitivity and the compressor
             * on/off option as specified by player 1, as these options
             * are inaccessible during game play, and are absolutely
             * necessary for distributed game behavior synchronization.
             * All the other options are purely cosmetic, or may cause
             * confusion if they are changed without notification.
             */
            FrozenBubble.setCollision(remotePrefs.collision);
            FrozenBubble.setCompressor(remotePrefs.compressor);
            /*
             * If all new game data synchronization requests have been
             * fulfilled, then the network game is ready to begin.
             */
            gotPrefsData = true;
            localStatus.prefsRequest = false;
            if (gotFieldData              &&
                !localStatus.fieldRequest &&
                !localStatus.readyToPlay) {
              localStatus.readyToPlay = true;
              localStatus.gameWonLost = false;
            }
            /*
             * The local player status was updated.  Set the status
             * timeout to expire immediately and wake up the network
             * manager thread.
             */
            setStatusTimeout(0L);
            synchronized(this) {
              notify();
            }
          }
        }

        /*
         * If the message contains a remote player game action, add it
         * to the remote player action list if we are ready to play.
         */
        if ((msgId == MSG_ID_ACTION) && (length == (ACTION_BYTES + 2))) {
          if (localStatus.readyToPlay && (playerId == remotePlayer.playerID)) {
            addAction(new PlayerAction(buffer, 2));
          }
        }

        /*
         * If the message contains the remote player game field, update
         * the remote player interface game field object.
         */
        if ((msgId == MSG_ID_FIELD) && (length == (FIELD_BYTES + 2))) {
          if ((playerId == remotePlayer.playerID) &&
              localStatus.fieldRequest) {
            remoteInterface.gameFieldData.copyFromBuffer(buffer, 2);
            remoteInterface.gotFieldData = true;
            /*
             * If all new game data synchronization requests have been
             * fulfilled, then the network game is ready to begin.
             */
            gotFieldData = true;
            localStatus.fieldRequest = false;
            if (!localStatus.prefsRequest && !localStatus.readyToPlay) {
              localStatus.readyToPlay = true;
              localStatus.gameWonLost = false;
            }
            /*
             * The local player status was updated.  Set the status
             * timeout to expire immediately and wake up the network
             * manager thread.
             */
            setStatusTimeout(0L);
            synchronized(this) {
              notify();
            }
          }
        }
      }
    }
  }

  public void pause() {
    if (running) {
      if (session != null) {
        session.pause();
      }
      paused = true;
    }
  }

  /**
   * Reserve the first available game ID, and update the transport layer
   * receive message filter to ignore all messages that don't have this
   * game ID.
   */
  public void reserveGameID() {
    for (byte index = 0;index < GAME_ID_MAX;index++) {
      if (!gamesInProgress[index]) {
        myGameID = index;
        if (session != null) {
          session.setFilter(myGameID);
        }
        /*
         * The transport layer message format has been updated, so send
         * an immediate status message to notify peers that we have
         * reserved a game ID.  This function is called directly from
         * the network manager thread, so there is no need to notify the
         * thread to wake up.
         */
        setStatusTimeout(0L);
        break;
      }
    }
  }

  /**
   * This is the network game manager thread's <code>run()</code> call.
   */
  @Override
  public void run() {
    paused  = false;
    running = true;

    while (running)
    {
      if (paused) try {
        synchronized(this) {
          wait();
        }
      } catch (InterruptedException ie) {
        /*
         * Interrupted.  This is expected behavior.
         */
      }

      if (!paused && running) try {
        synchronized(this) {
          wait(100);
        }
      } catch (InterruptedException ie) {
        /*
         * Timed out.  This is expected behavior.
         */
      }

      if (!paused && running) {
        manageNetworkGame();
      }
    }
  }

  /**
   * Send the specified local player action from the local action list.
   * @param actionId - the ID of the action to transmit.
   */
  private void sendLocalPlayerAction(short actionId) {
    synchronized(localActionList) {
      int listSize = localActionList.size();
  
      for (int index = 0; index < listSize; index++) {
        /*
         * If a match is found, transmit the action.
         */
        if (localActionList.get(index).localActionID == actionId) {
          transmitAction(localActionList.get(index));
        }
      }
    }
  }

  /**
   * Transmit the local player action to the remote player.  The action
   * counter identifier is incremented automatically.
   * @param playerId - the local player ID.
   * @param compress - set <code>true</code> to lower the compressor.
   * @param launch - set <code>true</code> to launch a bubble.
   * @param swap - set <code>true</code> to swap the launch bubble with
   * the next bubble.
   * @param keyCode - set to the key code value of the player key press.
   * @param launchColor - the launch bubble color.
   * @param nextColor - the next bubble color.
   * @param newNextColor - when a bubble is launched, this is the new
   * next bubble color.  The prior next color is promoted to the
   * launch bubble color.
   * @param attackBarBubbles - the number of attack bubbles stored on
   * the attack bar.  If there are attack bubbles being launched, this
   * should be the value prior to launch.
   * @param attackBubbles - the array of attack bubble colors.  A value
   * of -1 denotes no color, and thus no attack bubble at that column.
   * @param aimPosition - the launcher aim position.
   */
  public void sendLocalPlayerAction(int playerId,
                                    boolean compress,
                                    boolean launch,
                                    boolean swap,
                                    int keyCode,
                                    int launchColor,
                                    int nextColor,
                                    int newNextColor,
                                    int attackBarBubbles,
                                    byte attackBubbles[],
                                    double aimPosition) {
    PlayerAction tempAction = new PlayerAction(null);
    tempAction.playerID = (byte) playerId;
    tempAction.localActionID = ++localStatus.localActionID;
    tempAction.remoteActionID = localStatus.remoteActionID;
    tempAction.compress = compress;
    tempAction.launchBubble = launch;
    tempAction.swapBubble = swap;
    tempAction.keyCode = (byte) keyCode;
    tempAction.launchBubbleColor = (byte) launchColor;
    tempAction.nextBubbleColor = (byte) nextColor;
    tempAction.newNextBubbleColor = (byte) newNextColor;
    tempAction.attackBarBubbles = (short) attackBarBubbles;
    if (attackBubbles != null)
      for (int index = 0;index < 15; index++)
        tempAction.attackBubbles[index] = attackBubbles[index];
    tempAction.aimPosition = aimPosition;
    addAction(tempAction);
    transmitAction(tempAction);
    /*
     * The most current action IDs are being transmitted with this
     * action.  Postpone the player status message as it will be mostly
     * redundant with the information in this message, albeit the
     * delayed game field checksums may potentially lead to a more
     * significant game synchronization discrepancy if one is currently
     * present.
     */
    setStatusTimeout(STATUS_TIMEOUT);
  }

  /**
   * Set the action message timeout.
   * @param timeout - the timeout expiration interval.
   */
  public void setActionTimeout(long timeout) {
    actionTxTime = System.currentTimeMillis() + timeout;
  }

  /**
   * Set the flag to indicate that the local player won or lost the
   * network game.  Only set it once, otherwise calling this function
   * multiple times in succession would flood the network with player
   * status messages due to how an immediate local player status message
   * transmission is initiated whenever the player status changes.
   */
  public void setGameIsFinished() {
    if (!localStatus.gameWonLost) {
      localStatus.gameWonLost = true;
      /*
       * The local player status was updated.  Set the status timeout to
       * expire immediately and wake up the network manager thread.
       */
      setStatusTimeout(0L);
      synchronized(this) {
        notify();
      }
    }
  }

  /**
   * Set the game start timeout.
   * @param timeout - the timeout expiration interval.
   */
  public void setGameStartTimeout(long timeout) {
    gameStartTime = System.currentTimeMillis() + timeout;
  }

  /**
   * Set the local player local game field checksum.  The checksum is
   * set to zero immediately after every local player action, and must
   * be set as soon as the game field has become static and a new
   * checksum has been calculated.
   * @param checksum - the new game field checksum.
   */
  public void setLocalChecksum(short checksum) {
    localStatus.localChecksum = checksum;
  }

  /**
   * Set the local player remote game field checksum.  The checksum is
   * set to zero immediately after every remote player action, and must
   * be set as soon as the game field has become static and a new
   * checksum has been calculated.
   * @param checksum - the new game field checksum.
   */
  public void setRemoteChecksum(short checksum) {
    localStatus.remoteChecksum = checksum;
  }

  /**
   * Set the status message timeout.
   * @param timeout - the timeout expiration interval.
   */
  public void setStatusTimeout(long timeout) {
    statusTxTime = System.currentTimeMillis() + timeout;
  }

  private boolean statusTimerExpired() {
    return System.currentTimeMillis() >= statusTxTime;
  }

  /**
   * Stop and <code>join()</code> the network game manager thread.
   */
  private void stopThread() {
    paused  = false;
    running = false;
    synchronized(this) {
      interrupt();
    }
    /*
     * Close and join() the multicast thread.
     */
    boolean retry = true;
    while (retry) {
      try {
        join();
        retry = false;
      } catch (InterruptedException e) {
        /*
         * Keep trying to close the multicast thread.
         */
      }
    }
  }

  /**
   * Populate a byte array with the byte representation of a short.
   * The byte array must consist of at least 2 bytes.
   * @param value - the short to convert to a byte array.
   * @param array - the byte array where the converted short is placed.
   */
  public static void toByteArray(short value, byte[] array) {
    ByteBuffer.wrap(array).putShort(value);
  }

  /**
   * Populate a byte array with the byte representation of an integer.
   * The byte array must consist of at least 4 bytes.
   * @param value - the integer to convert to a byte array.
   * @param array - the byte array where the converted int is placed.
   */
  public static void toByteArray(int value, byte[] array) {
    ByteBuffer.wrap(array).putInt(value);
  }

  /**
   * Populate a byte array with the byte representation of a double.
   * The byte array must consist of at least 8 bytes.
   * @param value - the double to convert to a byte array.
   * @param array - the byte array where the converted double is placed.
   */
  public static void toByteArray(double value, byte[] array) {
    ByteBuffer.wrap(array).putDouble(value);
  }

  /**
   * Convert a byte array into a double value.
   * @param bytes - the byte array to convert into a double.
   * @return The double representation of the supplied byte array.
   */
  public static double toDouble(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getDouble();
  }

  /**
   * Convert a byte array into an integer value.
   * @param bytes - the byte array to convert into an integer.
   * @return The double representation of the supplied byte array.
   */
  public static int toInt(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getInt();
  }

  /**
   * Convert a byte array into a short value.
   * @param bytes - the byte array to convert into a short.
   * @return The short representation of the supplied byte array.
   */
  public static short toShort(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getShort();
  }

  /**
   * Transmit the local player action to the remote player via the
   * network interface.
   * @param action - the player action to transmit.
   * @return <code>true</code> if the transmission was successful.
   */
  private boolean transmitAction(PlayerAction action) {
    byte[] buffer = new byte[ACTION_BYTES + 2];
    buffer[0] = myGameID;
    buffer[1] = MSG_ID_ACTION;
    action.copyToBuffer(buffer, 2);
    /*
     * Send the datagram via the multicast manager.
     */
    if (session != null) {
      return session.transmit(buffer);
    }
    else {
      return false;
    }
  }

  /**
   * Transmit the local player game field to the remote player via the
   * network interface.
   * @param gameField - the player game field data to transmit.
   * @return <code>true</code> if the transmission was successful.
   */
  private boolean transmitGameField(GameFieldData gameField) {
    byte[] buffer = new byte[FIELD_BYTES + 2];
    buffer[0] = myGameID;
    buffer[1] = MSG_ID_FIELD;
    gameField.copyToBuffer(buffer, 2);
    /*
     * Send the datagram via the multicast manager.
     */
    if (session != null) {
      return session.transmit(buffer);
    }
    else {
      return false;
    }
  }

  /**
   * Transmit the player status message.
   * @return <code>true</code> if the transmission was successful.
   */
  private boolean transmitStatus(PlayerStatus status) {
    byte[] buffer = new byte[STATUS_BYTES + 2];
    buffer[0] = myGameID;
    buffer[1] = MSG_ID_STATUS;
    status.copyToBuffer(buffer, 2);
    /*
     * Send the datagram via the multicast manager.
     */
    if (session != null) {
      return session.transmit(buffer);
    }
    else {
      return false;
    }
  }

  /**
   * Transmit the local player preferences to the remote player via the
   * network interface.
   * @return <code>true</code> if the transmission was successful.
   */
  private boolean transmitPrefs() {
    byte[] buffer = new byte[Preferences.PREFS_BYTES + 3];
    buffer[0] = myGameID;
    buffer[1] = MSG_ID_PREFS;
    buffer[2] = (byte) localPlayer.playerID;
    copyPrefsToBuffer(localPrefs, buffer, 3);
    /*
     * Send the datagram via the multicast manager.
     */
    if (session != null) {
      return session.transmit(buffer);
    }
    else {
      return false;
    }
  }

  public void unPause() {
    paused = false;
    synchronized(this) {
      interrupt();
    }
    if (session != null) {
      session.unPause();
    }
  }

  public void updateNetworkStatus(NetworkStatus status) {
    status.localPlayerId  = localPlayer.playerID;
    status.remotePlayerId = remotePlayer.playerID;
    if (session != null) {
      status.isConnected = session.hasInternetConnection();
    }
    else {
      status.isConnected = false;
    }
    status.reservedGameId = myGameID != MulticastManager.FILTER_OFF;
    status.playerJoined = remoteStatus != null;
    if (localStatus != null) {
      status.gotFieldData  = gotFieldData;
      status.gotPrefsData  = gotPrefsData;
    }
    else {
      status.gotFieldData  = false;
      status.gotPrefsData  = false;
    }
    status.readyToPlay = gameIsReadyForAction();
    if ((localIpAddress == null) && (session != null)) {
      localIpAddress = session.getLocalIpAddress();
    }
    status.localIpAddress = localIpAddress;
    if (remoteIpAddress == null) {
      getRemoteAddress();
    }
    status.remoteIpAddress = remoteIpAddress;
  }
};
