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

public enum ContentIdStatus {

	/** If the ContentId Status is not (yet) known or is unavailable */
	UNKNOWN("unknown"),

	/** Only some parts of the final contentId are available yet */
	PARTIAL("partial"),

	/** The contentId has is in its final state */
	FINAL("final");

	private final String mStatusString;

	private ContentIdStatus(String statusString) {
		mStatusString = statusString;
	}

	/**
	 * Returns the String representation of this {@link DvbCssCiiContentIdStatus} object
	 * @return the String representation
	 */
	public String getContentIdStatusString() {
		return mStatusString;
	}

	/**
	 * Tries to match a given status string representation to a {@link DvbCssCiiContentIdStatus}
	 * @param statusString the String representation of the status
	 * @return the matching {@link DvbCssCiiContentIdStatus} or {@code DvbCssCiiContentIdStatus.UNKNOWN} if no matching object was found
	 */
	public static ContentIdStatus getContentIdStatusByString(String statusString) {
		for(ContentIdStatus cidStatus : ContentIdStatus.values()) {
			if(cidStatus.mStatusString.equalsIgnoreCase(statusString)) {
				return cidStatus;
			}
		}
		return ContentIdStatus.UNKNOWN;
	}
}
