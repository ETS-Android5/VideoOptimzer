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
import com.att.arotcpcollector.ip.IPv4Header;
import com.att.arotcpcollector.tcp.TCPHeader;
import com.att.arotcpcollector.udp.UDPHeader;
import com.att.arotcpcollector.util.PacketUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * This object stores information about a socket connection from a VPN client. Each session
 * is used by background worker to serve request from client.
 * 
 * @author Borey Sao Date: May 19, 2014
 */
public class Session {
	private static final String TAG = "Session";
	
	//help synchronizing receiving and sending streams
	private final Object syncReceive = new Object();
	private final Object syncSend = new Object();

	private final Object syncClearReceive = new Object();
	private final Object syncClearSend = new Object();

	//for increasing and decreasing sendAmountSinceLastAck
	private final Object syncSendAmount = new Object();

	//for setting TCP/UDP header
	private final Object syncLastHeader = new Object();

	private SocketChannel socketchannel = null;
	private DatagramChannel udpChannel = null;

	private String destAddress;
	private int destPort = 0;
	private String sourceIp;
	private int sourcePort = 0;

	//sequence received from client
	private long recSequence = 0;

	//track ack we sent to client and waiting for ack back from client
	private long sendUnack = 0;
	private boolean isacked = false;//last packet was acknowledged yet?

	//
	private long intialSequenceNumber;
	private long intialAckNumber;

	//the next ack to send to client
	private long sendNext = 0;
	private int sendWindow = 0; //window = windowSize x windowScale
	private int sendWindowSize = 0;
	private int sendWindowScale = 0;

	//track how many byte of data has been sent since last ACK from client
	private volatile int sendAmountSinceLastAck = 0;

	//sent by client during SYN inside tcp options
	private int maxSegmentSize = 0;

	//indicate that 3-way handshake has been completed or not
	private boolean isConnected = false;

	//receiving buffer for storing data from remote host
	private final ByteArrayOutputStream receivingStream;

	//sending buffer for storing data from vpn client to be send to destination host
	private final ByteArrayOutputStream sendingStream;

	// Receiving buffer for storing clear content data from Secure Collector
	private final ByteArrayOutputStream clearReceivingStream;

	// Sending buffer for storing clear content data from Secure Collector
	private final ByteArrayOutputStream clearSendingStream;

	private boolean hasReceivedLastSegment = false;

	//last packet received from client
	private IPHeader lastIpHeader = null;
	private TCPHeader lastTcpHeader = null;
	private UDPHeader lastUdpHeader = null;

	//true when connection is about to be close
	private boolean closingConnection = false;

	//indicate data from client is ready for sending to destination
	private boolean isDataForSendingReady = false;

	//store data for retransmission
	private byte[] unackData = null;

	//in ACK packet from client, if the previous packet was corrupted, client will send flag in options field
	private boolean packetCorrupted = false;

	//track how many time a packet has been retransmitted => avoid loop
	private int resendPacketCounter = 0;

	private int timestampSender = 0;
	private int timestampReplyto = 0;
	private long creationTime;

	//indicate that vpn client has sent FIN flag and it has been acknowledged
	private boolean ackedToFin = false;
	
	//timestamp when FIN as been acknowledged, this is used to removed session after n minute
	private long ackedToFinTime = 0;

	//indicate that this session is currently being worked on by some SocketDataWorker already
	private volatile boolean isbusyread = false;
	private volatile boolean isbusywrite = false;

	//closing session and aborting connection, will be done by background task
	private volatile boolean abortingConnection = false;

	private SelectionKey selectionKey = null;
	private String sessionKey = null;

	private boolean inContinuationMsg = false;
	private boolean outContinuationMsg = false;

	private volatile boolean printLog = false;

    private long lastAccessed = System.currentTimeMillis();

	public boolean isOutContinuationMsg() {
		return outContinuationMsg;
	}

	public void setOutContinuationMsg(boolean outContinuationMsg) {
		this.outContinuationMsg = outContinuationMsg;
	}

	public boolean isInContinuationMsg() {
		return inContinuationMsg;
	}

	public void setInContinuationMsg(boolean continuationMsg) {
		this.inContinuationMsg = continuationMsg;
	}
	Session() {
		Log.d(TAG, "new Session created");
		receivingStream = new ByteArrayOutputStream();
		sendingStream = new ByteArrayOutputStream();
		clearReceivingStream = new ByteArrayOutputStream();
		clearSendingStream = new ByteArrayOutputStream();
	}

	/**
	 * decrease value of sendAmountSinceLastAck so that client's window is not
	 * full
	 *
	 * @param amount amount
	 */
	void decreaseAmountSentSinceLastAck(long amount) {
		synchronized (syncSendAmount) {
			sendAmountSinceLastAck -= (int) amount;
			if (sendAmountSinceLastAck <= 0) {
				sendAmountSinceLastAck = 0;
			}
		}
	}

	/**
	 * determine if client's receiving window is full or not.
	 * 
	 * @return true if client window is full
	 */
	public boolean isClientWindowFull() {
		boolean yes = false;
		if (sendWindow > 0 && sendAmountSinceLastAck >= sendWindow) {
			yes = true;
		} else if (sendWindow == 0 && sendAmountSinceLastAck > 65535) {
			yes = true;
		}
		return yes;
	}

	/**
	 * append more data
	 * 
	 * @param data data to be added
	 * @return success/failure
	 */
	public boolean addReceivedData(byte[] data) {
		boolean success = true;
		synchronized (syncReceive) {
			try {
				receivingStream.write(data);
			} catch (IOException e) {
				success = false;
			}
		}
		return success;
	}

	public void resetReceivingData() {
		synchronized (syncReceive) {
			receivingStream.reset();
		}
	}

	/**
	 * get all data received in the buffer and empty it.
	 * 
	 * @return received data
	 */
	public byte[] getReceivedData(int maxSize) {
		Log.d(TAG, "getReceivedData maxSize:" + maxSize);
		byte[] data;
		synchronized (syncReceive) {
			data = receivingStream.toByteArray();
			receivingStream.reset();
			if (data.length > maxSize) {
				byte[] small = new byte[maxSize];
				System.arraycopy(data, 0, small, 0, maxSize);
				int len = data.length - maxSize;
				receivingStream.write(data, maxSize, len);
				data = small;
			}
		}
		return data;
	}

	/**
	 * buffer has more data for vpn client
	 * 
	 * @return if buffer has more data for vpn client
	 */
	public boolean hasReceivedData() {
		return receivingStream.size() > 0;
	}

	public int getReceivedDataSize() {
		int size;
		synchronized (syncReceive) {
			size = receivingStream.size();
		}
		return size;
	}

	/**
	 * set data to be sent to destination server
	 * 
	 * @param data data to be sent
	 * @return success/failure
	 */
	public boolean setSendingData(byte[] data) {
		boolean success = true;
		synchronized (syncSend) {
			try {
				sendingStream.write(data);
			} catch (IOException e) {
				success = false;
			}
		}
		return success;
	}

	public int getSendingDataSize() {
		return sendingStream.size();
	}

	/**
	 * dequeue data for sending to server
	 * @return data to send to server
	 */
	public byte[] getSendingData(){
		byte[] data;
		synchronized(syncSend){
			data = sendingStream.toByteArray();
			sendingStream.reset();
		}
		return data;
	}
	/**
	 * buffer contains data for sending to destination server
	 * @return if buffer has data to send to server
	 */
	public boolean hasDataToSend() {
		return sendingStream.size() > 0;
	}

	public String getDestAddress() {
		return destAddress;
	}

	public void setDestAddress(String destAddress) {
		this.destAddress = destAddress;
	}

	public int getDestPort() {
		return destPort;
	}

	public void setDestPort(int destPort) {
		this.destPort = destPort;
	}

	public long getSendUnack() {
		return sendUnack;
	}

	public void setSendUnack(long sendUnack) {
		this.sendUnack = sendUnack;
	}

	public long getSendNext() {
		return sendNext;
	}

	public void setSendNext(long sendNext) {
		this.sendNext = sendNext;
	}

	public int getSendWindow() {
		return sendWindow;
	}

	public int getMaxSegmentSize() {
		return maxSegmentSize;
	}

	public void setMaxSegmentSize(int maxSegmentSize) {
		this.maxSegmentSize = maxSegmentSize;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		Log.d(TAG, "setConnected :" + isConnected);
		this.isConnected = isConnected;
	}

	public ByteArrayOutputStream getReceivingStream() {
		return receivingStream;
	}

	public ByteArrayOutputStream getSendingStream() {
		return sendingStream;
	}

	public String getSourceIp() {
		return sourceIp;
	}

	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}

	public int getSourcePort() {
		return sourcePort;
	}

	public void setSourcePort(int sourcePort) {
		this.sourcePort = sourcePort;
	}

	public int getSendWindowSize() {
		return sendWindowSize;
	}

	public void setSendWindowSizeAndScale(int sendWindowSize, int sendWindowScale) {
		Log.d(TAG, "setSendWindowSizeAndScale size:" + sendWindowSize + " scale:" + sendWindowScale);
		this.sendWindowSize = sendWindowSize;
		this.sendWindowScale = sendWindowScale;
		this.sendWindow = sendWindowSize * sendWindowScale;
	}

	public int getSendWindowScale() {
		return sendWindowScale;
	}

	public boolean isAcked() {
		return isacked;
	}

	public void setAcked(boolean isacked) {
		this.isacked = isacked;
	}

	public long getRecSequence() {
		return recSequence;
	}

	public void setRecSequence(long recSequence) {
		this.recSequence = recSequence;
	}

	public SocketChannel getSocketchannel() {
		return socketchannel;
	}

	public void setSocketchannel(SocketChannel socketchannel) {
		Log.d(TAG, "setting SocketChannel :" + socketchannel);
		this.socketchannel = socketchannel;
	}

	public DatagramChannel getUdpChannel() {
		return udpChannel;
	}

	public void setUdpChannel(DatagramChannel udpChannel) {
		this.udpChannel = udpChannel;
	}

	public boolean hasReceivedLastSegment() {
		return hasReceivedLastSegment;
	}

	public void setHasReceivedLastSegment(boolean hasReceivedLastSegment) {
		this.hasReceivedLastSegment = hasReceivedLastSegment;
	}

	public IPHeader getLastIPheader() {
		IPHeader header;
		synchronized (syncLastHeader) {
            header = lastIpHeader;
		}
		return header;
	}

	public void setLastIPheader(IPHeader lastIPheader) {
		synchronized (syncLastHeader) {
			this.lastIpHeader = lastIPheader;
		}
	}

	public TCPHeader getLastTCPheader() {
		TCPHeader header;
		synchronized (syncLastHeader) {
			header = lastTcpHeader;
		}
		return header;
	}

	public void setLastTCPheader(TCPHeader lastTCPheader) {
		synchronized (syncLastHeader) {
			this.lastTcpHeader = lastTCPheader;
		}
	}

	public UDPHeader getLastUDPheader() {
		UDPHeader header;
		synchronized (syncLastHeader) {
			header = lastUdpHeader;
		}
		return header;
	}

	public void setLastUDPheader(UDPHeader lastUDPheader) {
		synchronized (syncLastHeader) {
			this.lastUdpHeader = lastUDPheader;
		}
	}

	public boolean isClosingConnection() {
		return closingConnection;
	}

	public void setClosingConnection(boolean closingConnection) {
		this.closingConnection = closingConnection;
	}


	public boolean isDataForSendingReady() {
		return isDataForSendingReady;
	}

	public void setDataForSendingReady(boolean isDataForSendingReady) {
		this.isDataForSendingReady = isDataForSendingReady;
	}

	public byte[] getUnackData() {
		return unackData;
	}

	public void setUnackData(byte[] unackData) {
		this.unackData = unackData;
	}

	public boolean isPacketCorrupted() {
		return packetCorrupted;
	}

	public void setPacketCorrupted(boolean packetCorrupted) {
		this.packetCorrupted = packetCorrupted;
	}

	public int getResendPacketCounter() {
		return resendPacketCounter;
	}

	public void setResendPacketCounter(int resendPacketCounter) {
		this.resendPacketCounter = resendPacketCounter;
	}

	public int getTimestampSender() {
		return timestampSender;
	}

	public void setTimestampSender(int timestampSender) {
		this.timestampSender = timestampSender;
	}

	public int getTimestampReplyto() {
		return timestampReplyto;
	}

	public void setTimestampReplyto(int timestampReplyto) {
		this.timestampReplyto = timestampReplyto;
	}

    public long getCreationTime() {
		return creationTime;
	}

    public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public boolean isAckedToFin() {
		return ackedToFin;
	}

	public void setAckedToFin(boolean ackedToFin) {
		this.ackedToFin = ackedToFin;
	}

	public long getAckedToFinTime() {
		return ackedToFinTime;
	}

	public void setAckedToFinTime(long ackedToFinTime) {
		this.ackedToFinTime = ackedToFinTime;
	}

	public boolean isBusyRead() {
		return isbusyread;
	}

	public void setBusyRead(boolean isbusyread) {
		this.isbusyread = isbusyread;
	}

	public boolean isBusyWrite() {
		return isbusywrite;
	}

	public void setBusyWrite(boolean isbusywrite) {
		this.isbusywrite = isbusywrite;
	}

	public boolean isAbortingConnection() {
		return abortingConnection;
	}

	public void setAbortingConnection(boolean abortingConnection) {
		this.abortingConnection = abortingConnection;
	}

	public SelectionKey getSelectionkey() {
		return selectionKey;
	}

	public void setSelectionkey(SelectionKey selectionkey) {
		this.selectionKey = selectionkey;
	}
	
	public String getSessionName() {
		return getDestAddress() + ":" + getDestPort() + " - " + getSourceIp() + ":" + getSourcePort();
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	//	this.sessionKey = destAddress + ":" + destPort + "-" + sourceIp + ":" + sourcePort;
	}

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

	public boolean isPrintLog() {
		return printLog;
	}

	public void setPrintLog(boolean printLog) {
		this.printLog = printLog;
	}

	/**
	 * append more data
	 *
	 * @param data data to be added
	 * @return success/failure
	 */
	public boolean addClearReceivedData(byte[] data) {
		boolean success = true;
		synchronized (syncClearReceive) {
			try {
				clearReceivingStream.write(data);
			} catch (IOException e) {
				success = false;
			}
		}
		return success;
	}

	public void resetClearReceivedData() {
		synchronized (syncClearReceive) {
			clearReceivingStream.reset();
		}
	}

	/**
	 * get all data received in the buffer and empty it.
	 *
	 * @return received data
	 */
	public byte[] getClearReceivedData() {
		byte[] data;
		synchronized (syncClearReceive) {
			data = clearReceivingStream.toByteArray();
			clearReceivingStream.reset();
		}
		return data;
	}

	/**
	 * append more data
	 *
	 * @param data data to be added
	 * @return success/failure
	 */
	public boolean addClearSendingData(byte[] data) {
		boolean success = true;
		synchronized (syncClearSend) {
			try {
				clearSendingStream.write(data);
			} catch (IOException e) {
				success = false;
			}
		}
		return success;
	}

	public void resetClearSendingData() {
		synchronized (syncClearSend) {
			clearSendingStream.reset();
		}
	}

	public byte[] getClearSendingData() {
		byte[] data;
		synchronized (syncClearSend) {
			data = clearSendingStream.toByteArray();
			clearSendingStream.reset();
		}
		return data;
	}

	public long getIntialSequenceNumber() {
		return intialSequenceNumber;
	}

	public void setIntialSequenceNumber(long intialSequenceNumber) {
		this.intialSequenceNumber = intialSequenceNumber;
	}

	public long getIntialAckNumber() {
		return intialAckNumber;
	}

	public void setIntialAckNumber(long intialAckNumber) {
		this.intialAckNumber = intialAckNumber;
	}
}
