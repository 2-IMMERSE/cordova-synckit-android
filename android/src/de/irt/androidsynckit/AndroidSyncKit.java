/************************************************************************/
/* FILE:                AndroidSyncKit.java                             */
/* DESCRIPTION:         Android Sync Kit codorva plugin interface       */
/* VERSION:             (see git)                                       */
/* DATE:                (see git)                                       */
/* AUTHOR:              Jonathan Rennison <jonathan.rennison@bt.com>    */
/*                                                                      */
/*                      © British Telecommunications plc 2016           */
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

import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.net.URISyntaxException;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AndroidSyncKit extends CordovaPlugin {
	public static final String TAG = "AndroidSyncKit";

	private long nextId = 0;
	private Map<Long, Synchroniser> mSynchroniserMap = new HashMap<Long, Synchroniser>();

	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if (action.equals("createSynchroniser")) {
			final Synchroniser s = new Synchroniser(args.getString(0), args.optString(1, null));
			int wallclockUpdatePeriodMillis = args.optInt(2);
			if (wallclockUpdatePeriodMillis > 0) {
				s.setWallclockUpdateInterval(wallclockUpdatePeriodMillis);
			}
			final long id = nextId++;
			mSynchroniserMap.put(id, s);
			JSONObject obj = new JSONObject();
			obj.put("type", "created");
			obj.put("id", id);
			PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
			result.setKeepCallback(true);
			callbackContext.sendPluginResult(result);
			return true;
		} else if (action.equals("obtainSynchronisationInformation")) {
			Long id = args.getLong(0);
			final Synchroniser s = mSynchroniserMap.get(id);
			if (s != null) {
				s.obtainSynchronisationInformation(new SynchroniserContentCallback() {
					@Override
					public void onContentIdChanged(String newContentId) {
						try {
							JSONObject obj = new JSONObject();
							obj.put("type", "contentIdChanged");
							obj.put("contentId", newContentId);
							PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
							result.setKeepCallback(true);
							callbackContext.sendPluginResult(result);
						} catch (Exception e) {
							Log.e(TAG, e.getMessage(), e);
							callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
						}
					}

					@Override
					public void onTimelinesAvailable() {
						try {
							JSONObject obj = new JSONObject();
							obj.put("type", "timelinesAvailable");
							obj.put("timelines", AndroidSyncKit.this.timelinesToJson(s.getTimelines()));
							PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
							result.setKeepCallback(true);
							callbackContext.sendPluginResult(result);
						} catch (Exception e) {
							Log.e(TAG, e.getMessage(), e);
							callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
						}
					}

					@Override
					public void onError(String errorDescription) {
						try {
							JSONObject obj = new JSONObject();
							obj.put("type", "error");
							obj.put("description", errorDescription);
							PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
							result.setKeepCallback(true);
							callbackContext.sendPluginResult(result);
						} catch (Exception e) {
							Log.e(TAG, e.getMessage(), e);
							callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
						}
					}

					@Override
					public void onSyncMessage(JSONObject msg) {
						try {
							JSONObject obj = new JSONObject();
							obj.put("type", "syncMessage");
							obj.put("msg", msg);
							PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
							result.setKeepCallback(true);
							callbackContext.sendPluginResult(result);
						} catch (Exception e) {
							Log.e(TAG, e.getMessage(), e);
							callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
						}
					}
				});
				JSONObject obj = new JSONObject();
				obj.put("type", "obtainStarted");
				PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
				result.setKeepCallback(true);
				callbackContext.sendPluginResult(result);
			} else {
				callbackContext.error("No such Synchroniser: " + id);
			}
			return true;
		} else if (action.equals("destroySynchroniser")) {
			Long id = args.getLong(0);
			Synchroniser s = mSynchroniserMap.get(id);
			if (s != null) {
				s.stopSynchronisation();
				mSynchroniserMap.remove(id);
			}
			callbackContext.success();
			return true;
		} else if (action.equals("startSynchroniser")) {
			Long id = args.getLong(0);
			int timelineId = args.getInt(1);
			final Synchroniser s = mSynchroniserMap.get(id);
			if (s != null) {
				List<Timeline> list = s.getTimelines();
				Timeline selected = null;
				if (list != null) {
					for (Timeline t : list) {
						if (t.getId() == timelineId) {
							selected = t;
							break;
						}
					}
				}
				if (selected != null) {
					s.startSynchronisation(selected, new SynchroniserSynchronisationCallback() {
						@Override
						public void wallclockSynced() {
							try {
								JSONObject obj = new JSONObject();
								obj.put("type", "wallclockSynced");
								PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
								result.setKeepCallback(true);
								callbackContext.sendPluginResult(result);
							} catch (Exception e) {
								Log.e(TAG, e.getMessage(), e);
								callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
							}
						}

						@Override
						public void wallclockUpdated() {
							try {
								JSONObject obj = new JSONObject();
								obj.put("type", "wallclockUpdated");
								if (s.isSynchronisedCurrentPtsValid()) {
									obj.put("timestamp", s.synchronisedCurrentPts() / 1000000000.d);
								} else {
									obj.put("timestamp", JSONObject.NULL);
								}
								PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
								result.setKeepCallback(true);
								callbackContext.sendPluginResult(result);
							} catch (Exception e) {
								Log.e(TAG, e.getMessage(), e);
								callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
							}
						}

						@Override
						public void synchronisedTimelineAvailable() {
							try {
								JSONObject obj = new JSONObject();
								obj.put("type", "available");
								PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
								result.setKeepCallback(true);
								callbackContext.sendPluginResult(result);
							} catch (Exception e) {
								Log.e(TAG, e.getMessage(), e);
								callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
							}
						}

						@Override
						public void synchronisedTimelineUnavailable() {
							try {
								JSONObject obj = new JSONObject();
								obj.put("type", "unavailable");
								PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
								result.setKeepCallback(true);
								callbackContext.sendPluginResult(result);
							} catch (Exception e) {
								Log.e(TAG, e.getMessage(), e);
								callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
							}
						}

						@Override
						public void synchronisationPropertiesChanged(boolean available, float speedMultiplier, long remoteWallclock, long remoteContentTime) {
							try {
								JSONObject obj = new JSONObject();
								obj.put("type", "propertiesChanged");
								JSONObject properties = new JSONObject();
								properties.put("available", available);
								if (available && s.isSynchronisedCurrentPtsValid()) {
									properties.put("speedMultiplier", speedMultiplier);
									properties.put("remoteWallclock", remoteWallclock);
									properties.put("remoteContentTime", remoteContentTime);
									obj.put("timestamp", s.synchronisedCurrentPts() / 1000000000.d);
								} else {
									obj.put("timestamp", JSONObject.NULL);
								}
								obj.put("properties", properties);
								PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
								result.setKeepCallback(true);
								callbackContext.sendPluginResult(result);
							} catch (Exception e) {
								Log.e(TAG, e.getMessage(), e);
								callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
							}
						}
					});
					JSONObject obj = new JSONObject();
					obj.put("type", "started");
					PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
					result.setKeepCallback(true);
					callbackContext.sendPluginResult(result);
				} else {
					callbackContext.error("No such Timeline: " + timelineId);
				}
			} else {
				callbackContext.error("No such Synchroniser: " + id);
			}
			return true;
		} else if (action.equals("stopSynchroniser")) {
			Long id = args.getLong(0);
			Synchroniser s = mSynchroniserMap.get(id);
			if (s != null) {
				s.stopSynchronisation();
				callbackContext.success();
			} else {
				callbackContext.error("No such Synchroniser: " + id);
			}
			return true;
		} else if (action.equals("getSynchroniserCurrentTime")) {
			Long id = args.getLong(0);
			Synchroniser s = mSynchroniserMap.get(id);
			if (s != null) {
				JSONObject obj = new JSONObject();
				if (s.isSynchronisedCurrentPtsValid()) {
					obj.put("timestamp", s.synchronisedCurrentPts() / 1000000000.d);
				} else {
					obj.put("timestamp", JSONObject.NULL);
				}
				callbackContext.success(obj);
			} else {
				callbackContext.error("No such Synchroniser: " + id);
			}
			return true;
		} else if (action.equals("overrideTimelineSyncUrl")) {
			Long id = args.getLong(0);
			Synchroniser s = mSynchroniserMap.get(id);
			if (s != null) {
				try {
					s.setTimelineSyncUrl(args.getString(1));
					callbackContext.success();
				} catch (URISyntaxException e) {
					Log.e(TAG, "URI syntax error: " + e.getMessage(), e);
					callbackContext.error("URI syntax error: " + e.getMessage());
				}
			} else {
				callbackContext.error("No such Synchroniser: " + id);
			}
			return true;
		} else if (action.equals("overrideWallclockUrl")) {
			Long id = args.getLong(0);
			Synchroniser s = mSynchroniserMap.get(id);
			if (s != null) {
				try {
					s.setWallclockUrl(args.getString(1));
					callbackContext.success();
				} catch (URISyntaxException e) {
					Log.e(TAG, "URI syntax error: " + e.getMessage(), e);
					callbackContext.error("URI syntax error: " + e.getMessage());
				}
			} else {
				callbackContext.error("No such Synchroniser: " + id);
			}
			return true;
		}
		return false;
	}

	private JSONArray timelinesToJson(List<Timeline> timelines) throws JSONException {
		JSONArray array = new JSONArray();
		if (timelines != null) {
			for (Timeline t : timelines) {
				JSONObject obj = new JSONObject();
				obj.put("id", t.getId());
				obj.put("selector", t.getTimelineSelectorString());
				array.put(obj);
			}
		}
		return array;
	}
}
