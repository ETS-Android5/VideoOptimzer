/*
 *  Copyright 2014 AT&T
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

package com.att.arotcpcollector;

import android.util.Log;

import com.att.arotcpcollector.ip.IPHeader;
import com.att.arotcpcollector.socket.SocketNIODataService;
import com.att.arotcpcollector.socket.SocketProtector;
import com.att.arotcpcollector.tcp.TCPHeader;
import com.att.arotcpcollector.udp.UDPHeader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Iterator;

/**
 * Manage in-memory storage for VPN client session.
 * @author Borey Sao Date: May 20, 2014
 */
public class SessionManager {
	private static final int SESSION_LIMIT = 50;
	public static final String TAG = "SessionManager";

	private static Object syncObj = new Object();
	private static volatile SessionManager instance = null;
	private SessionTable table = null;
	public static Object syncTable = new Object();
	private SocketProtector protector = null;
	Selector selector;

	private SessionManager() {
		table = new SessionTable(SESSION_LIMIT);
		protector = SocketProtector.getInstance();
		try {
			selector = Selector.open();
		} catch (IOException e) {
			Log.e(TAG, "Failed to create Socket Selector");
		}
	}

	public static SessionManager getInstance() {
		if (instance == null) {
			synchronized (syncObj) {
				if (instance == null) {
					instance = new SessionManager();
				}
			}
		}
		return instance;
	}

	public Selector getSelector() {
		return selector;
	}

	/**
	 * keep java garbage collector from collecting a session
	 * 
	 * @param session
	 */
	public void keepSessionAlive(Session session) {
		if (session != null) {
			String sessionKey = session.getSessionKey();
			if (sessionKey == null) {
				sessionKey = this.createKey(session.getDestAddress(), session.getDestPort(), session.getSourceIp(), session.getSourcePort());
			}
			synchronized (syncTable) {
				table.put(sessionKey, session);
			}
		}
	}

	public Iterator<Session> getAllSession() {
		return table.values().iterator();
	}

	public int addClientUDPData(IPHeader ip, UDPHeader udp, byte[] buffer, Session session) {
		int start = ip.getIPHeaderLength() + 8;
		int len = udp.getLength() - 8;//exclude header size
		if (len < 1) {
			return 0;
		}
		if ((buffer.length - start) < len) {
			len = buffer.length - start;
		}
		byte[] data = new byte[len];
		System.arraycopy(buffer, start, data, 0, len);
		session.setSendingData(data);
		return len;
	}

	/**
	 * add data from client which will be sending to the destination server
	 * later one when receiving PSH flag.
	 * 
	 * @param ip
	 * @param tcp
	 * @param buffer
	 */
	public int addClientData(IPHeader ip, TCPHeader tcp, byte[] buffer) {
		Session session = getSession(ip.getDestinationIP().getHostAddress(), tcp.getDestinationPort(), ip.getSourceIP().getHostAddress(), tcp.getSourcePort());
		if (session == null) {
			return 0;
		}
		int len = 0;
		//check for duplicate data
		if (session.getRecSequence() != 0 && tcp.getSequenceNumber() < session.getRecSequence()) {
			return len;
		}
		int start = ip.getIPHeaderLength() + tcp.getTCPHeaderLength();
		len = buffer.length - start;
		byte[] data = new byte[len];
		System.arraycopy(buffer, start, data, 0, len);
		//appending data to buffer
		session.setSendingData(data);
		return len;
	}

	// ** not used 
	//	public boolean hasSession(int ipaddress, int port, int srcIp, int srcPort) {
	//		String key = createKey(ipaddress, port, srcIp, srcPort);
	//		return table.containsKey(key);
	//	}

	/**
	 * Look to see if packet belongs to a session. A key is created to identify
	 * a session.
	 * 
	 * @param ipAddress
	 * @param destPort
	 * @param srcIpAddress
	 * @param srcPort
	 * @return session, null if no session found
	 */
	public Session getSession(String ipAddress, int destPort, String srcIpAddress, int srcPort) {

		String sessionKey = createKey(ipAddress, destPort, srcIpAddress, srcPort);
		Session session = null;

		synchronized (syncTable) {
			if (table.containsKey(sessionKey)) {
				session = table.get(sessionKey);
			}
		}
		return session;
	}

	public Session getSessionByKey(String sessionKey) {
		Session session = null;
		synchronized (syncTable) {
			if (table.containsKey(sessionKey)) {
				session = table.get(sessionKey);
			}
		}
		return session;
	}

	public Session getSessionByDatagramChannel(DatagramChannel channel) {
		return table.getSessionByChannel(channel);
	}

	public Session getSessionByChannel(SocketChannel channel) {
		Session session = null;
		synchronized (syncTable) {
			Iterator<Session> it = table.values().iterator();
			while (it.hasNext()) {
				Session sess = it.next();
				if (sess.getSocketchannel() == channel) {
					session = sess;
					break;
				}
			}
		}
		return session;
	}

	public void removeSessionByChannel(SocketChannel channel) {
		String key = null;
		String tmp = null;
		Session session = null;
		synchronized (syncTable) {
			Iterator<String> it = table.keySet().iterator();
			while (it.hasNext()) {
				tmp = it.next();
				Session sess = table.get(tmp);
				if (sess != null && sess.getSocketchannel() == channel) {
					key = tmp;
					session = sess;
					break;
				}

			}
		}
		if (key != null) {
			synchronized (syncTable) {
				table.remove(key);
			}
		}
		if (session != null) {
			Log.d(TAG,
					"closed session -> " + session.getDestAddress() + ":" + session.getDestPort() + "-"
							+ session.getSourceIp() + ":" + session.getSourcePort());
			session = null;
		}
	}

	/**
	 * remove session from memory, then close socket connection.
	 * 
	 * @param ip
	 * @param port
	 * @param srcIp
	 * @param srcPort
	 */
	public void closeSession(String ip, int port, String srcIp, int srcPort) {
		String keys = createKey(ip, port, srcIp, srcPort);
		Session session = null; //getSession(ip, port, srcIp, srcPort);
		synchronized (syncTable) {
			session = table.remove(keys);
		}
		if (session != null) {
			try {
				SocketChannel chan = session.getSocketchannel();
				if (chan != null) {
					chan.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.d(TAG,
					"closed session -> " + session.getDestAddress() + ":" + session.getDestPort() + "-"
							+ session.getSourceIp() + ":" + session.getSourcePort());
			session = null;
		}
	}

	/**
	 * Close the session
	 * 
	 * @param session
	 */
	public void closeSession(Session session) {
		if (session == null) {
			return;
		}
		
		String sessionKey = session.getSessionKey();
		if (sessionKey == null) {
			sessionKey = this.createKey(session.getDestAddress(), session.getDestPort(), session.getSourceIp(), session.getSourcePort());
		}
		
		synchronized (syncTable) {
			table.remove(sessionKey);
		}
		if (session != null) {
			try {
				SocketChannel chan = session.getSocketchannel();
				if (chan != null) {
					chan.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.d(TAG,
					"closed session -> " + session.getDestAddress() + ":" + session.getDestPort() + "-"
							+ session.getSourceIp() + ":" + session.getSourcePort());
			session = null;
		}
	}

	/**
	 * Create new UDP Session
	 * 
	 * @param ip
	 * @param port
	 * @param srcIp
	 * @param srcPort
	 * @return
	 */
	public Session createNewUDPSession(String ip, int port, String srcIp, int srcPort) {
		String sessionKey = createKey(ip, port, srcIp, srcPort);
		boolean found = false;
		synchronized (syncTable) {
			found = table.containsKey(sessionKey);
		}
		if (found) {
			return null;
		}
		Session ses = new Session();
		ses.setDestAddress(ip);
		ses.setDestPort(port);
		ses.setSourceIp(srcIp);
		ses.setSourcePort(srcPort);
		ses.setConnected(false);
		ses.setSessionKey(sessionKey);
		ses.setCreationTime(System.nanoTime());

		DatagramChannel channel = null;

		try {
			channel = DatagramChannel.open();
			channel.socket().setSoTimeout(0);
			channel.configureBlocking(false);

		} catch (SocketException ex) {
			Log.e(TAG, ">>>>---> failed open SocketException:" + ex.getMessage());
			return null;
		} catch (IOException e) {
			Log.e(TAG, ">>>>---> Failed to create SocketChannel: " + e.getMessage());
			return null;
		}
		protector.protect(channel.socket());

		//initiate connection to reduce latency
		SocketAddress addr = new InetSocketAddress(ip, port);
		Log.d(TAG, "initialized connection to remote UDP server: " + ip + ":" + port + " from " + srcIp + ":" + srcPort);

		try {
			channel.connect(addr);

			Object isudp = new Object();
			synchronized (SocketNIODataService.syncSelectorForUse) {
				selector.wakeup();
				synchronized (SocketNIODataService.syncSelectorForSelection) {
					SelectionKey selectkey = null;
					if (!channel.isConnected()) {
						selectkey = channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE, isudp);
					} else {
						selectkey = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, isudp);
					}
					ses.setSelectionkey(selectkey);
					Log.d(TAG, "Registered udp selector successfully");
				}
			}
		} catch (UnresolvedAddressException | UnsupportedAddressTypeException | SecurityException | IOException e) {
			Log.e(TAG, "Channel cannot be connected for session " + sessionKey, e);
		}

		ses.setConnected(channel.isConnected());

		ses.setUdpChannel(channel);

		synchronized (syncTable) {
			if (!table.containsKey(sessionKey)) {
				table.put(sessionKey, ses);
			} else {
				found = true;
			}
		}
		if (found) {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (ses != null) {
			if (found) {
				Log.d(TAG, "UDP session already existed for " + sessionKey + ".");
			} else {
				Log.d(TAG, "New UDP session successfully created for " + sessionKey + ".");
			}
		}
		return ses;
	}

	/**
	 * Retrieves the existing session if present. Returns null if no existing session is found.
	 * @param sessionKey
	 * @return
	 */
	public Session getSession(String sessionKey) {
		synchronized (syncTable) {
			return table.get(sessionKey);
		}
	}

	/**
	 * Create new TCP Session
	 * 
	 * @param ip
	 * @param port
	 * @param srcIp
	 * @param srcPort
	 * @return
	 * @throws SessionCreateException 
	 */
	public Session createNewSession(String ip, int port, String srcIp, int srcPort, long sequenceNumber, long ackNumber, boolean printLog) {
		String sessionKey = createKey(ip, port, srcIp, srcPort);
		boolean found = false;
		
		Session session = new Session();
		session.setDestAddress(ip);
		session.setDestPort(port);
		session.setSourceIp(srcIp);
		session.setSourcePort(srcPort);
		session.setConnected(false);
		session.setSessionKey(sessionKey);
		session.setPrintLog(printLog);
		session.setCreationTime(System.nanoTime());
		session.setIntialSequenceNumber(sequenceNumber);
		session.setIntialAckNumber(ackNumber);

		SocketChannel channel = null;
		boolean connected = false;
		// We will do 3 tries to open the channel
		for (int i = 1; i <= 3; ++i) {
			Log.d(TAG, "Channel connection attempt " + i + " for session " + sessionKey);

			if (channel == null || !channel.isOpen()) {
				try {
					channel = SocketChannel.open();
					channel.socket().setTcpNoDelay(false);
					channel.socket().setKeepAlive(true);
					channel.socket().setSoTimeout(0);
					channel.configureBlocking(false);
				} catch (SocketException ex) {
					Log.e(TAG, ">>>>---> failed open SocketException:" + ex.getMessage());
					return null;
				} catch (IOException e) {
					Log.e(TAG, ">>>>---> Failed to create SocketChannel: " + e.getMessage());
					return null;
				}
			}

			Log.d(TAG, "created new socketchannel for " + ip + ":" + port + "-" + srcIp + ":" + srcPort + " for session " + sessionKey);
			protector.protect(channel.socket());
			Log.d(TAG, "Protected new socketchannel");

			// Initiate connection to reduce latency
			SocketAddress addr = new InetSocketAddress(ip, port);
			Log.d(TAG, "Initiate connecting to remote tcp server: " + ip + ":" + port);
			try {
				connected = channel.connect(addr);

				Log.d(TAG, "Local: " + channel.getLocalAddress().toString() + ", Remote: " + channel.getRemoteAddress().toString());
				// Register for non-blocking operation
				synchronized (SocketNIODataService.syncSelectorForUse) {
					selector.wakeup();
					synchronized (SocketNIODataService.syncSelectorForSelection) {
						SelectionKey selectkey = channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
						session.setSelectionkey(selectkey);
						Log.d(TAG, "Registered tcp selector successfully");
					}
				}

				if (channel.isConnected() || channel.isConnectionPending()) {
					break;
				}
			} catch (ClosedChannelException ex) {
				Log.e(TAG, ">>>>---> failed connect ClosedChannelException:" + ex.getMessage());
			} catch (UnresolvedAddressException ex2) {
				Log.e(TAG, ">>>>---> failed connect UnresolvedAddressException:" + ex2.getMessage());
			} catch (UnsupportedAddressTypeException ex3) {
				Log.e(TAG, ">>>>---> failed connect UnsupportedAddressTypeException:" + ex3.getMessage());
			} catch (SecurityException ex4) {
				Log.e(TAG, ">>>>---> failed connect SecurityException:" + ex4.getMessage());
			} catch (IOException ex5) {
				Log.e(TAG, ">>>>---> failed connect IOException:", ex5);
			}
		}

		session.setConnected(connected);

		session.setSocketchannel(channel);

		synchronized (syncTable) {
			if (!table.containsKey(sessionKey)) {
				table.put(sessionKey, session);
			} else {
				found = true;
			}
		}
		if (found) {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			session = null;
		}

		return session;
	}

	/**
	 * create session key based on destination ip + port and source ip + port
	 * 
	 * @param ip
	 * @param port
	 * @param srcIp
	 * @param srcPort
	 * @return
	 */
	public String createKey(String ip, int port, String srcIp, int srcPort) {
		//	return (new StringBuilder("").append(ip).append(":").append(port).append("-").append(srcIp).append(":").append(srcPort)).toString();
		//	return String.format("%d:%d-%d:%d", ip, port, srcIp, srcPort);
		//	return String.format("%X:%X-%X:%X", ip, port, srcIp, srcPort);
		return ip + ":" + port + "-" + srcIp + ":" + srcPort;
	}
}
