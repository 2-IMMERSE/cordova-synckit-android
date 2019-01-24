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

public enum ContentPresentationStatus {

	/** If the Presentation Status is not (yet) known or is unavailable */
	UNKNOWN("unknown"),

	/**
	 * The TV Device is presenting Timed Content to the user
	 * Some portion of the Timed Content (such as video and/or audio)
	 * is being presented at any play speed, including paused, faster than normal, slower than normal or reverse
	*/
	OKAY("okay"),

	/**
	 * The TV Device is in the process of starting or changing what Timed Content is being presented
	 * to the user but has not yet begun presenting the Timed Content it is changing to
	*/
	TRANSITIONING("transitioning"),

	/**
	 * The TV Device is not currently presenting Timed Content to the user due to a problem either in
	 * receiving the Timed Content or in trying to present it
	 */
	FAULT("fault");

	private final String mStatusString;

	private ContentPresentationStatus(String statusString) {
		mStatusString = statusString;
	}

	/**
	 * Returns the String representation of this {@link DvbCssCiiPresentationStatus} object
	 * @return the String representation
	 */
	public String getStatusString() {
		return mStatusString;
	}

	/**
	 * Tries to match a given status string representation to a {@link DvbCssCiiPresentationStatus}
	 * @param statusString the String representation of the status
	 * @return the matching {@link DvbCssCiiPresentationStatus} or {@code DvbCssCiiPresentationStatus.UNKNOWN} if no matching object was found
	 */
	public static ContentPresentationStatus getStatusByString(String statusString) {
		for(ContentPresentationStatus status : ContentPresentationStatus.values()) {
			if(status.mStatusString.equalsIgnoreCase(statusString)) {
				return status;
			}
		}
		return ContentPresentationStatus.UNKNOWN;
	}
}
