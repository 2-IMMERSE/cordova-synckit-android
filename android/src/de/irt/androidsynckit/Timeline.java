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

import org.json.JSONObject;

import android.util.Log;

/**
 * Class representing a Timeline to synchronise content with.
 * @author Fabian Sattler, IRT GmbH
 */
public class Timeline {

	private final String TAG = "Timeline";

	private TimelineType mTimelineType;
	private String mTimelineSelectorString;
	private int mUnitsPerSecond;
	private int mUnitsPerTick;
	private int mId;

	private static int nextId = 1;

	protected Timeline(JSONObject timeLineJson) {
		mId = nextId;
		nextId++;

		mTimelineSelectorString = timeLineJson.optString("timelineSelector");
		if(SynchroniserFactory.LOG_DEBUG)Log.d(TAG, "TimeLineSelectorType: " + mTimelineSelectorString);

		String[] selectorTypeArr = mTimelineSelectorString.split(":");
		if(selectorTypeArr.length >= 5) {
			if(selectorTypeArr[4].equalsIgnoreCase("pts")) {
				mTimelineType = TimelineType.TIMELINETYPE_PTS;
			}
			if(selectorTypeArr[4].equalsIgnoreCase("ct")) {
				mTimelineType = TimelineType.TIMELINETYPE_CT;
			}
			if(selectorTypeArr[4].equalsIgnoreCase("tsap")) {
				mTimelineType = TimelineType.TIMELINETYPE_TSAP;
			}
			if(selectorTypeArr[4].equalsIgnoreCase("temi")) {
				mTimelineType = TimelineType.TIMELINETYPE_TEMI;
			}
			if(selectorTypeArr[4].equalsIgnoreCase("mpd")) {
				mTimelineType = TimelineType.TIMELINETYPE_MPD;
			}

			if(mTimelineType != null) {
				switch (mTimelineType) {
				case TIMELINETYPE_TSAP:
				case TIMELINETYPE_TEMI: {

					try {
						int componentTag = Integer.parseInt(selectorTypeArr[5]);
						int timelineId = Integer.parseInt(selectorTypeArr[6]);
					} catch (NumberFormatException numExc) {
						Log.e(TAG, "Exception parsing ComponentTag or TimelineID");
						numExc.printStackTrace();
					}
					break;
				}
				case TIMELINETYPE_MPD: {
					int ticksPerSecond = 0;
					int periodId = 0;
					switch (selectorTypeArr.length) {
					case 8: {
						ticksPerSecond = Integer.parseInt(selectorTypeArr[7]);
						break;
					}
					case 9: {
						ticksPerSecond = Integer.parseInt(selectorTypeArr[7]);
						periodId = Integer.parseInt(selectorTypeArr[8]);
						break;
					}
					default:
						break;
					}
					break;
				}
				default:
					break;
				}
			}

			if(mTimelineType == null) {
				mTimelineType = TimelineType.TIMELINETYPE_UNKNOWN;
			}
		} else {
			mTimelineType = TimelineType.TIMELINETYPE_UNKNOWN;
		}

		JSONObject timelinePropObj = timeLineJson.optJSONObject("timelineProperties");
		if(timelinePropObj != null) {
			mUnitsPerSecond = timelinePropObj.optInt("unitsPerSecond", -1);
			mUnitsPerTick = timelinePropObj.optInt("unitsPerTick", -1);
		} else {
			mUnitsPerSecond = -1;
			mUnitsPerTick = -1;
		}
	}

	/**
	 * Returns the ID of this {@link Timeline}
	 * @return integer ID
	 */
	public int getId() {
		return mId;
	}

	/**
	 * Returns the {@link TimelineType} of this {@link Timeline}
	 * @return the {@link TimelineType} of this {@link Timeline}
	 */
	public TimelineType getTimelineType() {
		return mTimelineType;
	}

	/**
	 * Returns the TimelineSelectorString of this {@link Timeline}
	 * @return the TimelineSelectorString of this {@link Timeline}
	 */
	public String getTimelineSelectorString() {
		return mTimelineSelectorString;
	}

	/**
	 * Returns the units per second of this {@link Timeline}
	 * @return  the units per second of this {@link Timeline}
	 */
	public int getUnitsPerSecond() {
		return mUnitsPerSecond;
	}

	/**
	 * Returns the units per tick of this {@link Timeline}
	 * @return the units per tick of this {@link Timeline}
	 */
	public int getUnitsPerTick() {
		return mUnitsPerTick;
	}

	/**
	 * Returns the Content presentation time (position) for a given content time
	 * @param contentTime the content to calculate the presentation time for
	 * @return the corresponding presentation time for the given content time or {@code -1} if UnitsPerSecond or UnitsPerTick are not known.
	 */
	public long contentTimeToPresentationTime(long contentTime) {
		if(mUnitsPerSecond > 0 && mUnitsPerTick > 0) {
			return contentTime / (long)(mUnitsPerSecond / mUnitsPerTick);
		}
		return -1;
	}

	public long millisecondsPtsToContentTime(long msPts) {
		if(mUnitsPerSecond > 0 && mUnitsPerTick > 0) {
			return msPts * (mUnitsPerSecond / mUnitsPerTick);
		}
		return -1;
	}
}
