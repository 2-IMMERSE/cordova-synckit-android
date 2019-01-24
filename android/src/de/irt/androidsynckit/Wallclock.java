/************************************************************************/
/*                COPYRIGHT Institut für Rundfunktechnik 2016           */
/*                                                                      */
/*	 Licensed under the Apache License, Version 2.0 (the "License");	*/
/*   you may not use this file except in compliance with the License.	*/
/*   You may obtain a copy of the License at							*/
/*       	     http://www.apache.org/licenses/LICENSE-2.0             */
/*  Unless required by applicable law or agreed to in writing, software */
/*  distributed under the License is distributed on an "AS IS" BASIS,	*/
/*	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 	*/
/*	implied.															*/
/*  See the License for the specific language governing permissions and	*/
/*  limitations under the License.										*/
/************************************************************************/
package de.irt.androidsynckit;

import java.io.IOException;
import java.lang.Exception;
import java.lang.Runnable;
import java.lang.RuntimeException;
import java.lang.Thread;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import android.os.SystemClock;
import android.util.Log;

public class Wallclock {

	private final String TAG = "Wallclock";

	private static long NANOPART = 1000000000;

	private final URI mWcUri;
	private int mUpdatePeriod = 1000;
	private boolean mIsRunning = false;
	private boolean mIsDestroyed = false;

	private Timer mUpdateTimer = null;
	private TimerTask mUpdateTimerTask = null;

	private WcUdpClient mWcUdpClient = null;

	protected Wallclock(URI wcUdpUri, Runnable updateCallback) {
		mWcUri = wcUdpUri;

		mWcUdpClient = new WcUdpClient(mWcUri, updateCallback);

		mUpdateTimerTask = new TimerTask() {

			@Override
			public void run() {
				mWcUdpClient.transmit();
			}
		};
	}

	public int getCurrentUpdatePeriod() {
		return mUpdatePeriod;
	}

	public void setUpdatePeriod(int periodMillis) {
		if(mUpdatePeriod != periodMillis) {
			mUpdatePeriod = periodMillis;

			resetUpdateTimer();
			if(mIsRunning) {
				mUpdateTimer.scheduleAtFixedRate(mUpdateTimerTask, 0, mUpdatePeriod);
			}
		}
	}

	public void start() {
		if(mIsDestroyed) throw new RuntimeException("Can't stop destroyed Wallclock");
		if(!mIsRunning) {
			mIsRunning = true;

			resetUpdateTimer();
			mUpdateTimer.scheduleAtFixedRate(mUpdateTimerTask, 0, mUpdatePeriod);
		}
	}

	public void stop() {
		if(mIsRunning) {
			mIsRunning = false;

			resetUpdateTimer();
		}
	}

	public void destroy() {
		stop();
		mWcUdpClient.destroy();
		mUpdateTimerTask = null;
	}

	private void resetUpdateTimer() {
		if(mUpdateTimer != null) {
			mUpdateTimer.cancel();
			mUpdateTimer = null;
		}
		if(mIsRunning) {
			mUpdateTimer = new Timer();
		}
	}

	public boolean isRunning() {
		return mIsRunning;
	}

	public long getCurrentOffset() {
		return mWcUdpClient.getCurrentWcOffset();
	}

	public int getCurentRoundTripTime() {
		return mWcUdpClient.getCurrentRtt();
	}

	public long remoteToLocalWallclockTimestamp(long remoteWcTimestamp) {
		return remoteWcTimestamp - mWcUdpClient.getCurrentWcOffset();
	}

	public long getCurrentLocalWallclock() {
		return SystemClock.elapsedRealtimeNanos();
	}

	public long getCurrentRemoteWallclock() {
		return SystemClock.elapsedRealtimeNanos() + mWcUdpClient.getCurrentWcOffset();
	}

	public boolean isCurrentRemoteWallclockValid() {
		return mWcUdpClient.isCurrentWcOffsetValid();
	}

	private class WcUdpClient {

		private final String UDPTAG = "WcUdpClient";

		private final InetSocketAddress mWcAddress;

		private DatagramSocket wcClientSocket = null;

		private long mCurrentWcOffset = 0;
		private boolean mCurrentWcOffsetValid = false;
		private int mCurrentRtt = 0;

		private Runnable mUpdateCallback;

		private Thread mReadThread = null;

		private boolean mIsDestroyed = false;

		public WcUdpClient(URI wcUri, Runnable updateCallback) {
			mUpdateCallback = updateCallback;
			mWcAddress = new InetSocketAddress(wcUri.getHost(), wcUri.getPort());
			try {
				wcClientSocket = new DatagramSocket();
				mReadThread = new Thread(null, new Runnable() {
					@Override
					public void run() {
						try {
							byte[] replyBuffer = new byte[32];
							while (true) {
								DatagramPacket replyPacket = new DatagramPacket(replyBuffer, 32);
								wcClientSocket.receive(replyPacket);
								if (mIsDestroyed) break;

								long replyTime = SystemClock.elapsedRealtimeNanos();

								byte[] replydata = replyPacket.getData();

								if(replydata[0] != 0x00) {
									if(SynchroniserFactory.LOG_DEBUG)Log.w(UDPTAG, "Wrong WC Version: " + replydata[0]);
									return;
								}

								WallclockMessageType msgType = WallclockMessageType.getMessageTypeByValue(replydata[1]);
								switch (msgType) {
								case MSGTYPE_RESPONSE_NO_FOLLOWUP: {
									parseWcMsg(replydata, replyTime);
									break;
								}
								case MSGTYPE_RESPONSE_WITH_FOLLOWUP: {
									if(SynchroniserFactory.LOG_DEBUG)Log.w(UDPTAG, "MsgResponse follow up will follow!");
									break;
								}
								case MSGTYPE_RESPONSE_FOLLOWUP: {
									if(SynchroniserFactory.LOG_DEBUG)Log.w(UDPTAG, "MsgResponse follow up data!");
									parseWcMsg(replydata, replyTime);
									break;
								}
								default:
									break;
								}
							}
						} catch (Exception e) {
							if (!mIsDestroyed) e.printStackTrace();
						}
						if(SynchroniserFactory.LOG_DEBUG)Log.w(UDPTAG, "read thread finished");
					}
				}, "wcClientSocket read thread");
				mReadThread.start();
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		public void transmit() {
			if (mIsDestroyed) return;
			try {
				//Request update
				byte[] reqBuf = new byte[32];
				reqBuf[0] = 0x00; //Version
				reqBuf[1] = 0x00; //MsgType = Request From CSA
				reqBuf[2] = 0x00; //Precision is an 8-bit twos-compliment signed integer log base 2 of the measurement precision
				reqBuf[3] = 0x00; //Reserved

				//MaxFreqError, not applicable for request
				reqBuf[4] = 0x00;
				reqBuf[5] = 0x00;
				reqBuf[6] = 0x00;
				reqBuf[7] = 0x00;

				//This is t1. The local time the message is being sent
				//long originTime = System.nanoTime();
				long originTime = SystemClock.elapsedRealtimeNanos();
				if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "OriginTime at TX: " + originTime);
				long originTimeSecs = originTime / NANOPART;
				long originTimeNanos = originTime % NANOPART;

				//Originate TimeValue secs
				reqBuf[8] |= ((int)originTimeSecs & 0xFF000000) >> 24;
				reqBuf[9] |= ((int)originTimeSecs & 0x00FF0000) >> 16;
				reqBuf[10] |= ((int)originTimeSecs & 0x0000FF00) >> 8;
				reqBuf[11] |= ((int)originTimeSecs & 0x000000FF);
				//Originate TimeValue nano
				reqBuf[12] |= ((int)originTimeNanos & 0xFF000000) >> 24;
				reqBuf[13] |= ((int)originTimeNanos & 0x00FF0000) >> 16;
				reqBuf[14] |= ((int)originTimeNanos & 0x0000FF00) >> 8;
				reqBuf[15] |= ((int)originTimeNanos & 0x000000FF);

				//Receive TimeValue secs
				reqBuf[16] = 0x00;
				reqBuf[17] = 0x00;
				reqBuf[18] = 0x00;
				reqBuf[19] = 0x00;
				//Receive TimeValue nano
				reqBuf[20] = 0x00;
				reqBuf[21] = 0x00;
				reqBuf[22] = 0x00;
				reqBuf[23] = 0x00;

				//Transmit TimeValue secs
				reqBuf[24] = 0x00;
				reqBuf[25] = 0x00;
				reqBuf[26] = 0x00;
				reqBuf[27] = 0x00;
				//Transmit TimeValue nano
				reqBuf[28] = 0x00;
				reqBuf[29] = 0x00;
				reqBuf[30] = 0x00;
				reqBuf[31] = 0x00;

				//Send update msg
				DatagramPacket updatePacket = new DatagramPacket(reqBuf, reqBuf.length, mWcAddress);
				wcClientSocket.send(updatePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private long mLastRttTime = 0;
		private void parseWcMsg(byte[] wcData, long replyTime) {
			//TODO
			// is an 8-bit twos-compliment signed integer log base 2 of the measurement precision of the Wall Clock WC Server measured in seconds
			int precision = ~wcData[2];
			precision = 31 - Integer.numberOfLeadingZeros(precision);

			// is an unsigned 32-bit integer of the maximum frequency error of the Wall Clock at the WC Server
			// measured in 1/256ths of parts per million (ppm).
			// If the value of this field is N, then the maximum frequency error of the Wall Clock in the WC Server is N/256 ppm or less
			//TODO
			int maxFreqError = (wcData[4] & 0xFF) << 24 | (wcData[5] & 0xFF) << 16 | (wcData[6] & 0xFF) << 8 | (wcData[7]  & 0xFF);
			maxFreqError = maxFreqError/256;

			int originTimeSecs = (wcData[8] & 0xFF) << 24 | (wcData[9] & 0xFF) << 16 | (wcData[10] & 0xFF) << 8 | (wcData[11] & 0xFF);
			int originTimeNanos = (wcData[12] & 0xFF) << 24 | (wcData[13] & 0xFF) << 16 | (wcData[14] & 0xFF) << 8 | (wcData[15] & 0xFF);
			long originTime = (originTimeSecs*NANOPART) + originTimeNanos;
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "OriginTime:   " + originTime);
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "ReplyTime:    " + replyTime);

			int receiveTimeSecs = (wcData[16] & 0xFF) << 24 | (wcData[17] & 0xFF) << 16 | (wcData[18] & 0xFF) << 8 | (wcData[19] & 0xFF);
			int receiveTimeNanos = (wcData[20] & 0xFF) << 24 | (wcData[21] & 0xFF) << 16 | (wcData[22] & 0xFF) << 8 | (wcData[23] & 0xFF);
			long receiveTime = (receiveTimeSecs*NANOPART) + receiveTimeNanos;
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "RemoteReceiveTime:  " + receiveTime);

			int transmitTimeSecs = (wcData[24] & 0xFF) << 24 | (wcData[25] & 0xFF) << 16 | (wcData[26] & 0xFF) << 8 | (wcData[27] & 0xFF);
			int transmitTimeNanos = (wcData[28] & 0xFF) << 24 | (wcData[29] & 0xFF) << 16 | (wcData[30] & 0xFF) << 8 | (wcData[31] & 0xFF);
			long transmitTime = (transmitTimeSecs*NANOPART) + transmitTimeNanos;
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "RemoteTransmitTime: " + transmitTime);
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "RemoteWorkTime:     " + (transmitTime-receiveTime) );

			//Offset: ((t2+t3)-(t1+t4))/2
			//Offset: ( (receiveTime+transmitTime) - (originTime + mReplyTime) ) / 2
			long myWc = SystemClock.elapsedRealtimeNanos() + mWcUdpClient.getCurrentWcOffset();
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "MyWallclock:  " + myWc);
			//Log.d(UDPTAG, "MyWallcDiff:  " + (transmitTime-myWc) );
			long lastOffset = mCurrentWcOffset;
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "Wallclock OffsetLast: " + lastOffset);

			mCurrentWcOffset = ( (receiveTime + transmitTime) - (originTime + replyTime) ) / 2;
			mCurrentWcOffsetValid = true;
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "Wallclock OffsetCurr: " + mCurrentWcOffset);
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "Wallclock OffsetDiff: " + (lastOffset - mCurrentWcOffset) + " : " + ( ((double)(lastOffset - mCurrentWcOffset)) / NANOPART) );

			//RoundTripTime = (t4-t1) - (t3-t2)
			mCurrentRtt = (int)((replyTime - originTime) - (transmitTime - receiveTime));
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "Wallclock RTT: " + mCurrentRtt);
			if(SynchroniserFactory.LOG_DEBUG)Log.d(UDPTAG, "Wallclock RTT Time: " + mLastRttTime);

			mLastRttTime = mCurrentWcOffset-mCurrentRtt;

			if (mUpdateCallback != null) {
				mUpdateCallback.run();
			}
		}

		long getCurrentWcOffset() {
			return mCurrentWcOffset;
		}

		public boolean isCurrentWcOffsetValid() {
			return mCurrentWcOffsetValid;
		}

		int getCurrentRtt() {
			return mCurrentRtt;
		}

		void destroy() {
			if (!mIsDestroyed) {
				mIsDestroyed = true;
				mReadThread = null;
				if (wcClientSocket != null) {
					if(SynchroniserFactory.LOG_DEBUG)Log.w(UDPTAG, "closing socket");
					wcClientSocket.close();
					wcClientSocket = null;
				}
			}
		}
	}
}
