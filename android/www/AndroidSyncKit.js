/************************************************************************/
/* FILE:                AndroidSyncKit.js                               */
/* DESCRIPTION:         Android Sync Kit codorva plugin JS interface    */
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

"use strict";

var exec = require('cordova/exec');

/**
 * @callback Synchroniser~ContentCallback
 * @param {string} contentId
 */
/**
 * Timeline info
 *
 * @typedef {Object} Synchroniser~Timeline
 * @property {number} id Timeline ID
 * @property {string} selector Timeline selector string
 */
/**
 * @callback Synchroniser~TimelinesAvailableCallback
 * @param {Synchroniser~Timeline[]} timelines
 */
/**
 * @callback Synchroniser~ErrorCallback
 * @param {string} errorMessage
 */

/**
 * @classdesc
 *
 * AndroidSyncKit Synchroniser.
 *
 * @constructor
 * @param {!Object} params
 * @param {!string} params.url URL to use
 * @param {string=} params.name optional name
 * @param {Function=} params.initCallback optional construction completion callback
 * @param {Synchroniser~ErrorCallback=} params.errorCallback optional error callback
 * @param {number=} params.wallclockUpdatePeriodMillis optional wallclock update period in ms
 */
function Synchroniser(params) {
	var self = this;
	var success = function(result) {
		if (result.type === "created") {
			self.id = result.id;
			if (params.initCallback) params.initCallback();
		}
	};
	var error = function(code) {
		if (params.errorCallback) {
			params.errorCallback("Cordova error: " + code);
		}
	};
	exec(success, error, "AndroidSyncKit", "createSynchroniser", [params.url, params.name, params.wallclockUpdatePeriodMillis]);
}

/** Destroy synchroniser */
Synchroniser.prototype.destroy = function() {
	exec(function() { }, function() { }, "AndroidSyncKit", "destroySynchroniser", [this.id]);
};

/**
 * Obtain synchronisation information
 *
 * @param {!Object} params
 * @param {Function=} params.obtainStartedCallback optional obtain started notification callback
 * @param {Synchroniser~ContentCallback=} params.contentCallback optional content ID change callback
 * @param {Synchroniser~TimelinesAvailableCallback=} params.timelinesAvailableCallback optional timelines available notification callback
 * @param {Synchroniser~ErrorCallback=} params.errorCallback optional error callback
 */
Synchroniser.prototype.obtainSynchronisationInformation = function(params) {
	var self = this;
	var success = function(result) {
		if (result.type === "contentIdChanged" && params.contentCallback) {
			params.contentCallback(result.contentId);
		}
		if (result.type === "timelinesAvailable" && params.timelinesAvailableCallback) {
			params.timelinesAvailableCallback(result.timelines);
		}
		if (result.type === "error" && params.errorCallback) {
			params.errorCallback(result.description);
		}
		if (result.type === "obtainStarted" && params.obtainStartedCallback) {
			params.obtainStartedCallback();
		}
		if (result.type === "syncMessage" && params.syncMessageCallback) {
			params.syncMessageCallback(result.msg);
		}
	};
	var error = function(code) {
		if (params.errorCallback) {
			params.errorCallback("Cordova error: " + code);
		}
	};
	exec(success, error, "AndroidSyncKit", "obtainSynchronisationInformation", [this.id]);
};

/**
 * @callback Synchroniser~PropertiesChangedCallback
 * @param {number} timestamp Current time in seconds
 * @param {Object} properties
 */

/**
 * Start synchronisation
 *
 * @param {!Object} params
 * @param {!number} params.timelineId Timeline ID
 * @param {Function=} params.startedCallback optional started completion callback
 * @param {Function=} params.wallclockSyncedCallback optional wall clock synced notification callback
 * @param {Synchroniser~TimestampCallback=} params.wallclockUpdatedCallback optional wall clock updated notification callback
 * @param {Function=} params.availableCallback optional synchronised timeline available (socket connected) callback
 * @param {Function=} params.unavailableCallback optional synchronised timeline unavailable (socket disconnected) callback
 * @param {Synchroniser~PropertiesChangedCallback=} params.propertiesChangedCallback optional properties changed callback
 * @param {Synchroniser~ErrorCallback=} params.errorCallback optional error callback
 */
Synchroniser.prototype.start = function(params) {
	var self = this;
	self.have_properties = false;
	var success = function(result) {
		if (result.type === "wallclockSynced" && params.wallclockSyncedCallback) {
			params.wallclockSyncedCallback();
		}
		if (result.type === "wallclockUpdated" && params.wallclockUpdatedCallback) {
			params.wallclockUpdatedCallback(self.have_properties ? result.timestamp : null);
		}
		if (result.type === "available" && params.availableCallback) {
			params.availableCallback();
		}
		if (result.type === "unavailable") {
			self.have_properties = false;
			if (params.unavailableCallback) {
				params.unavailableCallback();
			}
		}
		if (result.type === "propertiesChanged") {
			self.have_properties = true;
			if (params.propertiesChangedCallback) {
				params.propertiesChangedCallback(result.timestamp, result.properties);
			}
		}
		if (result.type === "started" && params.startedCallback) {
			params.startedCallback();
		}
	};
	var error = function(code) {
		if (params.errorCallback) {
			params.errorCallback("Cordova error: " + code);
		}
	};
	exec(success, error, "AndroidSyncKit", "startSynchroniser", [this.id, params.timelineId]);
};

/**
 * Stop synchroniser
 *
 * @param {Function=} callback Called with one parameter: null on success, error string on failure
 */
Synchroniser.prototype.stop = function(callback) {
	var success = function(result) {
		if (callback) callback(null);
	};
	var error = function(code) {
		if (callback) {
			callback("Cordova error: " + code);
		}
	};
	exec(success, error, "AndroidSyncKit", "stopSynchroniser", [this.id]);
};

/**
 * @callback Synchroniser~TimestampCallback
 * @param {?number} timestamp Current time in seconds, or null if not available
 */
/**
 * Get current time
 *
 * @param {!Synchroniser~TimestampCallback} callback timeline timestamp callback
 * @param {Synchroniser~ErrorCallback=} errorCallback optional error callback
 */
Synchroniser.prototype.getCurrentTime = function(callback, errorCallback) {
	if (!this.have_properties) {
		callback(null);
		return;
	}
	var success = function(result) {
		if (callback) callback(result.timestamp);
	};
	var error = function(code) {
		if (errorCallback) {
			errorCallback("Cordova error: " + code);
		}
	};
	exec(success, error, "AndroidSyncKit", "getSynchroniserCurrentTime", [this.id]);
};

/**
 * Override wallclock URL
 *
 * @param {!string} url Wallclock URL
 * @param {Function=} callback optional completion callback
 * @param {Synchroniser~ErrorCallback=} errorCallback optional error callback
 */
Synchroniser.prototype.overrideTimelineSyncUrl = function(url, callback, errorCallback) {
	return this._overrideUrl("overrideTimelineSyncUrl", url, callback, errorCallback);
};

/**
 * Override wallclock URL
 *
 * @param {!string} url Wallclock URL
 * @param {Function=} callback optional completion callback
 * @param {Synchroniser~ErrorCallback=} errorCallback optional error callback
 */
Synchroniser.prototype.overrideWallclockUrl = function(url, callback, errorCallback) {
	return this._overrideUrl("overrideWallclockUrl", url, callback, errorCallback);
};

Synchroniser.prototype._overrideUrl = function(cmd, url, callback, errorCallback) {
	var success = function(result) {
		if (callback) callback();
	};
	var error = function(code) {
		if (errorCallback) {
			errorCallback("Cordova error: " + code);
		}
	};
	exec(success, error, "AndroidSyncKit", cmd, [this.id, url]);
};

exports.Synchroniser = Synchroniser;
