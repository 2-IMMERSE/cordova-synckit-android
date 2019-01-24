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

public interface SynchroniserSynchronisationCallback {

	/**
	 * Notifies that the {@link Wallclock} is synchronised
	 */
	void wallclockSynced();

	/**
	 * Notifies that the {@link Wallclock} has received an update
	 */
	void wallclockUpdated();

	/**
	 * Notifies that the Synchronised timeline is available
	 */
	void synchronisedTimelineAvailable();

	/**
	 * Notifies that the Synchronised timeline is no longer available. This may happen due to network errors.
	 */
	void synchronisedTimelineUnavailable();

	void synchronisationPropertiesChanged(boolean available, float speedMultiplier, long remoteWallclock, long remoteContentTime);
}
