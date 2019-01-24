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

/**
 * Interface for a Synchronisers content callback
 * @author Fabian Sattler, IRT GmbH
 */
public interface SynchroniserContentCallback {

	/**
	 * Notifies about a changed ContentId
	 * @param newContentId the new ContentId
	 */
	void onContentIdChanged(String newContentId);

	/**
	 * Notifies that {@link Timeline}s for the {@link Synchroniser} are available
	 */
	void onTimelinesAvailable();

	/**
	 * An error occured while obtaining content and timeline information.
	 * @param errorDescription
	 */
	void onError(String errorDescription);

	/**
	 * Notification of synchronisation message received
	 * @param msg
	 */
	void onSyncMessage(JSONObject msg);
}
