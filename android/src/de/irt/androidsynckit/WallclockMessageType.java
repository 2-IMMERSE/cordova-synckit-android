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

public enum WallclockMessageType {

	/** Unknown or reserved message type*/
	MSGTYPE_UNKNOWN(-1),

	/*** Request from CSA */
	MSGTYPE_REQUEST(0),

	/*** Response from TV Device that will not be followed by a follow-up response */
	MSGTYPE_RESPONSE_NO_FOLLOWUP(1),

	/*** Response from TV Device that will be followed by a follow-up response */
	MSGTYPE_RESPONSE_WITH_FOLLOWUP(2),

	/*** Follow-up response from TV Device  */
	MSGTYPE_RESPONSE_FOLLOWUP(3);

	private final int mMsgType;

	private WallclockMessageType(int messageType) {
		mMsgType = messageType;
	}

	public int getMessageTypeValue() {
		return mMsgType;
	}

	public static WallclockMessageType getMessageTypeByValue(int msgValue) {
		for(WallclockMessageType msgType : WallclockMessageType.values()) {
			if(msgType.getMessageTypeValue() == msgValue) {
				return msgType;
			}
		}

		return WallclockMessageType.MSGTYPE_UNKNOWN;
	}
}
