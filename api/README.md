# SyncKit API

The SyncKit API is a client library that TV or Companion Screen web apps use to synchronise their media objects to an external timeline source.

The API provides bindings to library implementations that support both **interdevice synchronisation** via DVB-CSS (e.g. intra-home sync) and **distributed synchronisation** via a cloud-based [Synchronisation Service](https://github.com/2-IMMERSE/cloud-sync.git) (e.g. inter-home sync).


## API

### ```createSynchroniser()```
If the ```synckit.js``` library has been added to the CSA (companion screen application) or the TV application, the method ```createSynchroniser()``` is available on the global scope of the web application. Use ```createSynchroniser()``` to create a ```Synchroniser``` object.
<br>The method initialises a Synchroniser object for *interdevice* synchronisation or for *distributed* synchronisation based on the parameters supplied. It detects the browser's platform and configures itself by loading platform specific modules. From the timeline source supplied, it detects the sync mode: either **interdevice** or **distributed**. <br>Based on the sync mode and the device platform, it configures itself to use the required modules for the desired mode of operation. For example, if interdevice synchronisation mode is required (intra-home sync), the Synchroniser object is likely to run on the companion device's application and will configure itself to use required native (Android, iOS) APIs. <br>For distributed sync, the object will configure itself to use the cloud-based Synchronisation Service.

###### Parameters:

| Name | Type | Description |
| --- | --- | --- |
| ```syncUrl``` | ```string``` | Source of Synchronisation Timeline. For interdevice sync, this is CSS-CII server endpoint URL. For distributed sync, this the cloud-based Synchronisation Service URL. |
| ```sessionId``` | ```string``` | **Optional**. A session identifier supplied by the application. The sessionId is used to contact the correct Synchronisation Service instance in the cloud.|



### ```Synchroniser```
A singleton of type ```Synchroniser``` that enables synchronisation to an external timing source. The external timing source provides a timeline called the **Synchronisation Timeline**.

#### Properties
| Name | Type | Description |
| --- | --- | --- |
| ```contentId``` | ```string``` | A content identifier for the content currently shown on the master device |
| ```syncTimeline``` | ```object``` | A [CorrelatedClock](https://github.com/bbc/dvbcss-clocks) object representing the Synchronisation Timeline. It can be queried for the current time on the timeline. It also has these properties: timeline selector, unitsPerTick, unitsPerSecond and accuracy.|
| ```wallclock``` | ```object``` | A [CorrelatedClock](https://github.com/bbc/dvbcss-clocks) object representing the internal WallClock. It can be queried for its current time or used to set timers. |

#### Methods

##### ```startContentMonitor()```
For **interdevice** synchronisation, this method uses the sync_url to connect to the CII service endpoint on the TV and receive messages with the following information: a content identifier, WallClock sync service endpoint (CSS-WC) URL, timeline synchronisation service endpoint URL (CSS-TS) and a list of available timelines. <br>
For **distributed** synchronisation via the cloud-based [Synchronisation Service](https://github.com/2-IMMERSE/cloud-sync), this method uses the sync_url to make an HTTP GET request to the Synchronisation Service instance for this session to retrieve the following information: a content identifier (if applicable), the WallClock sync microservice endpoint URL, timeline synchronisation microservice endpoint URL and a list of available timelines.

###### Parameters:

| Name | Type | Description |
| --- | --- | --- |
| ```onContentIdChange``` | ```function``` | Callback function to notify the application about a change in contentId |
| ```onTimelinesAvailable``` | ```function``` | Callback function that is called to notify the application when a list of candidate timelines (timelineInfo objects) are available for synchronisation. The  **timelineSelector** string can  can be used by the application to specify which timeline to use for synchronisation. |

##### ```stopContentMonitor()```
For **interdevice** synchronisation, this closes the CSS-CII protocol connection and stops reporting contentId changes to the application.
For **distributed** synchronisation via the cloud-based [Synchronisation Service](https://github.com/2-IMMERSE/cloud-sync), this method stops making HTTP GET requests on the Synchronisation Service interface.



##### ```enableSynchronisation()```
Starts the Synchroniser object's sync operation. This will trigger connection requests to be sent to synchronisation services. The availability of the Synchronisation Timeline is signalled to the application via an event/callback..

###### Parameters:

| Name | Type | Description |
| --- | --- | --- |
| ```timelineSelector``` | ```string``` | Type of the timeline to be used by the ```Synchroniser``` for synchronisation. e.g. PCR/PTS: ```"urn:dvb:css:timeline:pts"```, Simple Elapsed Time:  ```"tag:rd.bbc.co.uk,2015-12-08:dvb:css:timeline:simple-elapsed-time:1000"```|
| ```syncAccuracy``` | ```float``` | Value in seconds for synchronisation accuracy threshold |



##### ```registerForTimelineUpdates()```
Registers the application to receive periodic updates about the synchronisation timeline progress. Changes to the synchronisation timeline such as a seek operation (new time position) or a pause operation (speed is 0.0) at the master terminal, will trigger a timeline update before the next scheduled update.

###### Parameters:

| Name | Type | Description |
| --- | --- | --- |
| ```updateInterval``` | ```float``` | Value in seconds for desired update periodicity |
| ```onNewTimelineUpdate``` | ```function``` | Callback function to notify the application of the current time on the Synchronisation Timeline. This is the time in seconds on the master timeline. |


##### ```disableSynchronisation()```
Disables the interdevice/distributed synchronisation running in an application. Closes connections to synchronisation protocol endpoints. The Synchroniser object goes back to the initialized state. The ```enableSynchronisation()``` method can be called to enable sync again.

###### Parameters:

| Name | Type | Description |
| --- | --- | --- |
| ```onSyncStopped``` | ```function``` | Callback function that is called to notify the application when synchronisation protocols have been stopped. |

##### ```currentTime()```
Returns the currentTime on the Synchronisation Timeline in seconds.

##### ```currentTicks()```
Returns the currentTime on the Synchronisation Timeline in ticks.

##### ```on()```
Sets an event listener.

###### Parameters

| Name | Type | Description |
| --- | --- | --- |
| ```eventName``` | ```string``` | Name of the event (see Events). |
| ```callback``` | ```function``` | Callback function that is called when the event occurs. |

###### Events

| Name | Description |
| --- | --- |
| ```WALLCLK_SYNCED``` | Is emitted if the internal WallClock is synchronised.|
| ```TIMELINES_AVAIL``` | Is emitted when sync master notifies the Synchroniser object about available timelines for synchronisation.|
| ```SYNCTIMELINE_AVAIL``` | Is emitted when the sync timeline is available.|
| ```SYNCTIMELINE_UNAVAIL``` | Is emitted when the sync timeline becomes unavailable due to network error. |
| ```CONTENTID_CHNG``` | Is emitted if a change in contentId is reported.|
| ```LOW_SYNC_ACCURACY``` | Is emitted if sync accuracy degrades beyond set threshold.|

#### ```createSyncController()```
If the ```Synchroniser``` object is a synchronised state, a ```SyncController``` object can be created to synchronise the playback of a media object.

###### Parameters

| Name | Type | Description |
| --- | --- | --- |
| ```mediaObject``` | ```Object``` | Media object the controller is monitoring. |
| ```correlation``` | ```object``` | Object containing a pair of timestamps to describe the relationship between the Synchronisation Timeline and the media object's timeline. |
| ```reSyncInterval``` | ```float``` | Period in seconds after which the media object playback state is re-evaluated against the Synchronisation Timeline. |
| ```onControllerStateChange``` | ```function``` | Callback function to notify the application about the SyncController state changes |

### ```SyncController```
A controller object that will adapt the playback of a media object (or a 2IMMERSE DMAPPComponent object) . The object provides all necessary information to establish an App-to-App-Communication channel and to synchronise its media presentation to the presentation on the HbbTV terminal by means of the DVB CSS protocol.


#### Properties
| Name | Type | Description |
| --- | --- | --- |
| ```mediaObject``` | ```Object``` | Media object the controller is monitoring. |
| ```correlation``` | ```Object``` | Object containing a pair of timestamps to describe the relationship between the Synchronisation Timeline and the media object's timeline. |
| ```reSyncInterval``` | ```float``` | Period in seconds after which the media object playback state is re-evaluated against the Synchronisation Timeline.|
| ```syncJitter``` | ```float``` | The jitter in seconds  when resynchronising media playback. |


#### Methods

##### ```start()```
Starts the resync operation of the SyncController.

##### ```stop()```
Stops the resync operation of the SyncController.


## Usage

There are 3 ways to use the Synchroniser Object to achieve synchronisation of media playback. They vary in the degree of control deffered to the object.

### 1. Timeline Update Pull Approach
Create a Synchroniser object and read time from its synchronised clock representing the Synchronisation Timeline.
  1. The application calls ```createSynchroniser()``` with a sync service URL to create a ```Synchroniser``` object
  2. It calls ```startContentMonitor()``` to be notified about contentId changes and a list of timelines available for sync from the master.
  2. Upon receiving a list of timelines (```TIMELINES_AVAIL``` event), ```enableSynchronisation()``` is called to start the underlying sync protocols.
  3. After receiving a ```SYNCTIMELINE_AVAIL``` event, the app reads the current time on the Synchronisation Timeline using ```currentTime()```.
  4. Using the current time value, the app evaluates the media object position (taking care to map the time value to the media object timeline).

```javascript
var sync_url = "ws://192.168.1.2:8261/cii";
var synchroniser = createSynchroniser(sync_url);

var timelinesAvailableListener = function(timelines)
{
  synchroniser.enableSynchronisation(timelines[0],  0.01);
}

var contentIdChangeListener = function(contentId)
{

}

synchroniser.on("CONTENTID_CHNG", contentIdChangeListener);
synchroniser.on("TIMELINES_AVAIL", timelinesAvailableListener);

synchroniser.startContentMonitor();

synchroniser.on("SYNCTIMELINE_AVAIL", function()
{
  var experience_now = synchroniser.currentTime();
  // compare time with corresponding time on media object timeline.
  //...

});
```

### 2. Timeline Update Push Approach
Create a Synchroniser object and register for Synchronisation Timeline updates.
1. The application calls ```createSynchroniser()``` with a sync service URL to create a ```Synchroniser``` object
2. It calls ```startContentMonitor()``` to be notified about contentId changes and a list of timelines available for sync from the master.
3. Upon receiving a list of timelines (```TIMELINES_AVAIL``` event), ```enableSynchronisation()``` is called to start the underlying sync protocols.
4. The app registers for timestamps stating current time on Synchronisation Timeline using ```registerForTimelineUpdates()```.
5. Using the current time value, the app evaluates the media object position (taking care to map the time value to the media object timeline).

```javascript
// a timeline update handler
var updateHandler = function(time_now)
{
  //...
}
// sync_url and sessionId obtained by application via Layout Service
var sync_url = "http://2immerse/syncservice/";
var sessionId = "4ads4asdf42";


var synchroniser = createSynchroniser(sync_url, sessionId);

var timelinesAvailableListener = function(timelines)
{
  // use a particular timeline from available set for synchronisation
  synchroniser.enableSynchronisation(timelines[0],  0.01);
}

synchroniser.on("TIMELINES_AVAIL", timelinesAvailableListener);

synchroniser.startContentMonitor();

synchroniser.registerForTimelineUpdates(1.0, updateHandler);
```

### 3. SyncController Delegate Approach
Create a ```Synchroniser``` object and delegate the resynchronisation of each media project to a ```SyncController``` object.
1. The application calls ```createSynchroniser()``` with a sync service URL to create a ```Synchroniser``` object
2. It calls ```startContentMonitor()``` to be notified about contentId changes and a list of timelines available for sync from the master.
3. Upon receiving a list of timelines (```TIMELINES_AVAIL``` event), ```enableSynchronisation()``` is called to start the underlying sync protocols.
4. The app creates a SyncController object using ```createSyncController()```and passes it a correlation object (a pair of timestamps) to map time from the Synchronisation Timeline and the media object timeline.
5. The app starts the SyncController object.
6. When the synchronisation timeline becomes available, the SyncController resynchronises the media object's playback to follow the sync timeline's progress.

```javascript
var videoplayer = document.getElementById('video');
var controller;

var sync_url = "http://2immerse/syncservice/";
var sessionId = "4ads4asdf42";


var synchroniser = createSynchroniser(sync_url, sessionId);

var timelinesAvailableListener = function(timelines)
{
  // use a particular timeline from available set for synchronisation
  synchroniser.enableSynchronisation(timelines[0],  0.01);
}

synchroniser.on("TIMELINES_AVAIL", timelinesAvailableListener);

synchroniser.startContentMonitor();

synchroniser.on("SYNCTIMELINE_AVAIL", function()
{
  controller = synchroniser.createSyncController(videoplayer, {experience_time:0, media_time:0}, 4.0);
  controller.start();

});


```
