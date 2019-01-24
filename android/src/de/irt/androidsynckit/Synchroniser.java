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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class Synchroniser {

	private final String TAG;

	//Instantiation
	private final String mSyncUrl;
	private final String mSessionId;

	//Obtained values
	private String mContentId;
	private List<Timeline> mTimelines;
	private ContentPresentationStatus mPresentationStatus = ContentPresentationStatus.UNKNOWN;
	private ContentIdStatus mContentIdStatus = ContentIdStatus.UNKNOWN;
	private String mTimelineSyncUrlString;
	private String mWallClockUrlString;
	private URI mTimelineSyncUrl;
	private URI mWallClockUrl;
	private int mProtoMajorVersion;
	private int mProtoMinorVersion;

	private Wallclock mWallclock;
	private boolean mWallclockSynced = false;

	//Sync WS
	private WebSocketClient mWsSyncClient = null;
	private Timeline mSynchronisedTimeline;
	private float mCurrentSpeedMulti = -1;
	private long mCurrentRemoteWc = -1;
	private long mCurrentRemoteContentTime = -1;
	private boolean mIsContentAvailable = false;

	//Config
	private int mSetWallclockUpdateInterval = 0;

	/*
	HandlerThread mHandlerThread;
	Handler mHandler;
	*/

	private WebSocketClient mSyncWsClient;

	protected Synchroniser(String syncUrl, String sessionId) {
		mSyncUrl = syncUrl;
		if(sessionId.isEmpty() || sessionId == null) {
			mSessionId = UUID.randomUUID().toString();
		} else {
			mSessionId = sessionId;
		}

		TAG = "Synchroniser: " + mSessionId;

		/*
		mHandlerThread = new HandlerThread("irt:ipstreamplayer", android.os.Process.THREAD_PRIORITY_AUDIO);
		mHandlerThread.start();
		mHandler = new Handler(mHandlerThread.getLooper());
		*/

		mTimelines = new ArrayList<Timeline>();
	}

	/**
	 * Returns the given SessionId at creation or a generated SessionId
	 * @return the given SessionId at creation or a generated SessionId
	 */
	public String getSessionId() {
		return mSessionId;
	}

	/**
	 * Returns the ContentId obtained via DvbCss
	 * @return the ContentId obtained via DvbCss or {@code null} if not yet obtained or unavailable.
	 */
	public String getContentId() {
		return mContentId;
	}

	/**
	 * Returns the available {@link Timeline}s or an empty list.
	 * @return the available {@link Timeline}s or an empty list.
	 */
	public List<Timeline> getTimelines() {
		return mTimelines;
	}

	/**
	 * Returns the current presentationstatus of the content
	 * @return the current presentationstatus of the content
	 */
	public ContentPresentationStatus getContentPresentationStatus() {
		return mPresentationStatus;
	}

	/**
	 * Returns the current status of the contentId
	 * @return the current status of the contentId
	 */
	public ContentIdStatus getContentIdStatus() {
		return mContentIdStatus;
	}

	public String getWallclockUrl() {
		return mWallClockUrlString;
	}

	public String getTimelineSyncUrl() {
		return mTimelineSyncUrlString;
	}

	public void setWallclockUrl(String urlString) throws URISyntaxException {
		if(!urlString.isEmpty()) {
			mWallClockUrl = new URI(urlString);
			mWallClockUrlString = urlString;
		} else {
			mWallClockUrl = null;
			mWallClockUrlString = null;
		}
	}

	public void setTimelineSyncUrl(String urlString) throws URISyntaxException {
		if(!urlString.isEmpty()) {
			mTimelineSyncUrl = new URI(urlString);
			mTimelineSyncUrlString = urlString;
		} else {
			mTimelineSyncUrl = null;
			mTimelineSyncUrlString = null;
		}
	}

	public Wallclock getWallclock() {
		return mWallclock;
	}

	public void setWallclockUpdateInterval(int wallclockUpdateInterval) {
		mSetWallclockUpdateInterval = wallclockUpdateInterval;
	}

	public Timeline getSynchronisedTimeline() {
		return mSynchronisedTimeline;
	}

	/**
	 * Starts to obtain all needed information to synchronise content
	 * @param callback the {@link SynchroniserCallback} to notify about the status
	 */
	public void obtainSynchronisationInformation(final SynchroniserContentCallback callback) {
		if(mSyncWsClient == null) {
			try {
				URI syncUri = new URI(mSyncUrl);

				mTimelines.clear();

				mSyncWsClient = new WebSocketClient(syncUri, new Draft_17()) {

					@Override
					public void onOpen(ServerHandshake handshakedata) {
						if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "CII Websocket onOpen Code: " + handshakedata.getHttpStatus());
					}

					@Override
					public void onMessage(String message) {
						Log.d(TAG, "CII Websocket onMessage: " + message);

						try {
							JSONObject jsonObj = new JSONObject(message);

							if(callback != null) {
								callback.onSyncMessage(jsonObj);
							}

							String presStat = jsonObj.optString("presentationStatus", "unknown");
							if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "PresentationStatus: " + presStat);

							//this may happen on some terminals. just for sanity
							if(presStat.equalsIgnoreCase("null")) {
								presStat = "unknown";
							}
							mPresentationStatus = ContentPresentationStatus.getStatusByString(presStat);

							String contIdStat = jsonObj.optString("contentIdStatus", "unknown");
							//this may happen on some terminals. just for sanity
							if(contIdStat.equalsIgnoreCase("null")) {
								contIdStat = "unknown";
							}
							mContentIdStatus = ContentIdStatus.getContentIdStatusByString(contIdStat);

							setTimelineSyncUrl(jsonObj.optString("tsUrl"));

							setWallclockUrl(jsonObj.optString("wcUrl"));

							String[] protVersionStringArr = jsonObj.optString("protocolVersion", "0.0").split("[.]");
							if(protVersionStringArr.length > 1) {
								mProtoMajorVersion = Integer.parseInt(protVersionStringArr[0]);
								mProtoMinorVersion = Integer.parseInt(protVersionStringArr[1]);
							}

							String contId = jsonObj.optString("contentId");
							if(!contId.isEmpty()) {
								mContentId = contId;

								if(callback != null) {
									callback.onContentIdChanged(mContentId);
								}
							}

							JSONArray timelineArr = jsonObj.optJSONArray("timelines");
							if(timelineArr != null) {
								for(int i = 0; i < timelineArr.length(); i++) {
									JSONObject timelineObj = timelineArr.getJSONObject(i);

									Timeline tl = new Timeline(timelineObj);
									mTimelines.add(tl);
								}
							}

							if(!mTimelines.isEmpty()) {
								if(callback != null) {
									callback.onTimelinesAvailable();
								}
							}

						} catch (JSONException e) {
							if(SynchroniserFactory.LOG_DEBUG)Log.e(TAG, "Error parsing message to JSON : " + message);

							if(callback != null) {
								callback.onError("Error parsing CII");
							}
						} catch (URISyntaxException e) {
							if(SynchroniserFactory.LOG_DEBUG)Log.e(TAG, "Error parsing TS URI: " + message);

							if(callback != null) {
								callback.onError("Error parsing CSS TS URL");
							}
						}
					}

					@Override
					public void onError(Exception ex) {
						Log.d(TAG, "CII Websocket onError!");
						ex.printStackTrace();
					}

					@Override
					public void onClose(int code, String reason, boolean remote) {
						Log.d(TAG, "CII Websocket onClose Code: " + code + " Reason: " + reason + " fromRemote: " + remote);
					}
				};
			} catch(URISyntaxException uriExc) {
				if(SynchroniserFactory.LOG_DEBUG)Log.e(TAG, "Cannot create URI from String: " + mSyncUrl);

				if(callback != null) {
					callback.onError("URL Parser Error");
				}
			}

			mSyncWsClient.connect();
		}
	}

	/**
	 * Starts synchronisation with the given {@link Timeline}
	 * @param syncTimeline the {@link Timeline} to synchronise with
	 */
	public void startSynchronisation(Timeline syncTimeline, final SynchroniserSynchronisationCallback syncCallback) {
		if(syncTimeline != null) {

			mSynchronisedTimeline = syncTimeline;

			mWallclockSynced = false;
			mWallclock = new Wallclock(mWallClockUrl, new Runnable() {
				@Override
				public void run() {
					if (!mWallclockSynced) {
						mWallclockSynced = true;
						if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "Wallclock synced");
						syncCallback.wallclockSynced();
					}
					syncCallback.wallclockUpdated();
				}
			});
			if (mSetWallclockUpdateInterval > 0) {
				mWallclock.setUpdatePeriod(mSetWallclockUpdateInterval);
			}
			mWallclock.start();

			if(mTimelineSyncUrl != null) {
				mWsSyncClient = new WebSocketClient(mTimelineSyncUrl, new Draft_17()) {

					@Override
					public void onOpen(ServerHandshake handshakedata) {
						if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "TS Websocket onOpen!");

						String setupdata = generateSetupDataJson().toString();
						if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "TS Websocket sending setup Data: " + setupdata);
						mWsSyncClient.send(generateSetupDataJson().toString());

						syncCallback.synchronisedTimelineAvailable();
					}

					@Override
					public void onMessage(String message) {
						if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "TS Websocket onMessage: " + message);

						try {
							JSONObject controlTimestampObj = new JSONObject(message);

							String speedMultiString = controlTimestampObj.optString("timelineSpeedMultiplier", "0.0");
							if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "SpeedMultiStr: " + speedMultiString);
							boolean available = true;
							float speedMulti;
							if(speedMultiString.equalsIgnoreCase("null")) {
								speedMulti = 0;
								available = false;
							} else {
								//int speedInt = Integer.parseInt(speedMultiString);
								//TODO
								speedMulti = Float.parseFloat(speedMultiString);
								//speedMulti = (float)speedInt;
							}
							mCurrentSpeedMulti = speedMulti;
							mCurrentRemoteWc = Long.parseLong(controlTimestampObj.optString("wallClockTime", "-1"));
							long myWcTime = mWallclock.getCurrentRemoteWallclock();

							String contTimeString = controlTimestampObj.optString("contentTime", "-1");
							if(contTimeString.equalsIgnoreCase("null")) {
								mCurrentRemoteContentTime = 0;
								available = false;
							} else {
								mCurrentRemoteContentTime = Long.parseLong(contTimeString);
							}

							mIsContentAvailable = available;

							syncCallback.synchronisationPropertiesChanged(available, mCurrentSpeedMulti, mCurrentRemoteWc, mCurrentRemoteContentTime);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onError(Exception ex) {
						if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "TS Websocket onError!");
						ex.printStackTrace();
					}

					@Override
					public void onClose(int code, String reason, boolean remote) {
						if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "TS Websocket onClose Code: " + code + " Reason: " + reason + " fromRemote: " + remote);

						syncCallback.synchronisedTimelineUnavailable();
					}
				};

				mWsSyncClient.connect();
			}
		}
	}

	/**
	 * Stops the synchronisation
	 */
	public void stopSynchronisation() {
		//TODO stop everything
		if(mWsSyncClient != null) {
			if(mWsSyncClient.isOpen()) {
				mWsSyncClient.close();
			}
			mWsSyncClient = null;
		}

		if(mWallclock != null) {
			if(mWallclock.isRunning()) {
				mWallclock.stop();
			}
			mWallclock.destroy();
			mWallclock = null;
		}
	}

	private JSONObject generateSetupDataJson() {
		JSONObject setupdataObj = new JSONObject();
		try {
			setupdataObj.put("contentIdStem", mContentId);
			setupdataObj.put("timelineSelector", mSynchronisedTimeline.getTimelineSelectorString());
		} catch (JSONException e) {
			if(SynchroniserFactory.LOG_DEBUG)Log.e(TAG, "Error generating SetupDataJSON!");
			if(SynchroniserFactory.LOG_DEBUG)e.printStackTrace();
		}

		return setupdataObj;
	}

	public long synchronisedCurrentPts() {
		if (!this.isContentAvailable() || !this.isSynchronisedCurrentPtsValid()) {
			Log.e(TAG, "synchronisedCurrentPts called when content or wallclock not available");
			(new Throwable()).printStackTrace();
		}
		long curRemWcDiff = (mWallclock.getCurrentRemoteWallclock() - mCurrentRemoteWc);
		long elapsedContentTime = (long)(curRemWcDiff * ((double)mSynchronisedTimeline.getUnitsPerSecond() / 1000000000.d));
		elapsedContentTime *= mCurrentSpeedMulti;
		return (long) ((mCurrentRemoteContentTime + elapsedContentTime) / ((double)mSynchronisedTimeline.getUnitsPerSecond() / 1000000000.d));
	}

	public boolean isSynchronisedCurrentPtsValid() {
		return mWallclock.isCurrentRemoteWallclockValid() && this.isContentAvailable();
	}

	public boolean isContentAvailable() {
		return mIsContentAvailable;
	}
}
