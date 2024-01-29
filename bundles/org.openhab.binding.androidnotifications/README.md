# AndroidNotifications Binding

This binding can talk with more then 1 Google Play Store app, you only need one of them not both.
This binding will try to standardize the way you interact with any of the apps, so if one app disappears, gets buggy, gets caught sending data back home, or excessively nags with ads, you can switch to one of the others with minimal changes to openHAB.

The two supported apps so far are:

+ TvOverlay (working well) <https://play.google.com/store/apps/details?id=com.tabdeveloper.tvoverlay&hl=en_AU&gl=US>
+ Android TV Notifications (work in progress, Text based messages now work) <https://play.google.com/store/apps/details?id=de.cyberdream.androidtv.notifications.google&hl=en&gl=US>

Other apps which are not yet supported, but could be are:

+ Push TV <https://play.google.com/store/apps/details?id=de.andreashuth.pushtv>
+ PiPup <https://play.google.com/store/apps/details?id=nl.rogro82.pipup>

## How to Install and Setup

Please refer to the github pages linked just below for help on how to setup the apps and any TV settings that are needed.
They will be the most up to date and detailed sources for using their app.

### TvOverlay

<https://github.com/gugutab/TvOverlay>

## Supported Things

- `nfatvdisplay`: Android TV Notifications, add one for each TV that is setup and working with this app.
- `tvoverlaydisplay`: TvOverlay, add one for each TV that is setup and working with this app.

It is possible more things will get added to capture the notifications sent from your phone and other non display devices.
This is close to working for the Android TV Notifications app, but not yet ready for release.

## Discovery

If you have both apps installed on your TV, only the first one discovered one will get added.
You can manually add the thing and provide just the IP address and the setup from openHAB is very simple, please report back if yours does not get auto found so this can be improved for others.

## Thing Configuration

### `tvoverlaydisplay` Thing Configuration

| Name            | Type    | Description                           | Default | Required | Advanced |
|-----------------|---------|---------------------------------------|---------|----------|----------|
| address        | text    | Hostname or IP address of the device  | N/A     | yes      | no       |
| port            | number  | Port to access the device             |  5001   | yes      | yes      |

### `nfatvdisplay` Thing Configuration

| Name            | Type    | Description                           | Default | Required | Advanced |
|-----------------|---------|---------------------------------------|---------|----------|----------|
| address        | text    | Hostname or IP address of the device  | N/A     | yes      | no       |
| port            | number  | Port to access the device             |  7676   | yes      | yes      |

## Channels

Not yet fully documented, just use the main UI to see what is available, and read the descriptions in the UI until I find the time to add them here.

| Channel                   | Type      | Read/Write | Description                                                                              |
|---------------------------|-----------|------------|------------------------------------------------------------------------------------------|
| sendTestNotification      | Switch    | RW         | Use this switch to send a test message to check if the app on the TV is setup correctly |
| sendNotification          | String    | RW         | Use this to send simple messages |
| displayFixedNotifications | Switch    | RW         | Show or hide any Fixed Notifications |
| fixedNotificationsVisibility | Dimmer | RW         | Change how transparent the fixed notifications are |
| overlayVisibility         | Dimmer    | RW         | Use this to reduce the brightness of the main TV screen allowing messages to be seen easier |
| clockOverlayVisibility    | Dimmer    | RW         | Change how transparent the clock is |
| hotCorner                 | String    | RW         | Change which corner of the screen the messages and clock get displayed in. TvOverlay string. |
| displayCorner             | String    | RW         | Change which corner of the screen the messages and clock get displayed in. Values 0-4 |
| notificationDuration      | Number:Time | RW       | Change the default amount of time the messages get displayed for if `null` is used in a message |
| displayNotifications      | Switch    | RW         | Show or hide any normal non fixed type notifications |
| fontSize                  | String    | RW         | Size of the font, value 0 to 4 |       

## Actions

Please read the example section below if these do not make sense to you.
The messageID is very important as you can use it to edit a message even if it is still displaying on the TV screen.
It can also be used to delete the message if it is still waiting in the queue. 

There are 3 shorter ways to send messages, and these use the apps default settings for the missing options.
There are as follows:

+ sendText(String messageID, @Nullable String title, String message)

+ sendImage(String messageID, @Nullable String title, @Nullable String message, String imageURL)

+ sendVideo(String messageID, @Nullable String title, @Nullable String message, String videoURL)

The 3 longer versions give access to all variables and any marked as `@Nullable` can have `null` sent to use the programs default setting if you do not wish to provide one.
There are as follows:    
        
+ sendText(String messageID, @Nullable String title, String message, @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration)
                       
+ sendImage(String messageID, @Nullable String title, @Nullable String message, String imageURL, @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor, @Nullable String corner, @Nullable Integer duration)            
            
+ sendVideo(String messageID, @Nullable String title, @Nullable String message, String videoURL, @Nullable String largeIcon, @Nullable String smallIcon,
            @Nullable String smallIconColor, @Nullable String corner, @Nullable Integer duration)
            
+ boolean sendFixedNotification(String messageID, @Nullable String messageColor, @Nullable String message,
            @Nullable String icon, @Nullable String iconColor, @Nullable String borderColor,
            @Nullable String backgroundColor, @Nullable Integer expiration, @Nullable String shape,
            @Nullable Boolean visible)
            
When using TvOverlay, the icons can be any of the material design icons (mdi), for example  `mdi:home-alert-outline` and `mdi:chevron-up-circle-outline` are valid strings, but many more can be selected.

## Examples

The most simple method is to use the `sendNotification` channel if you link it to a String item, you can use it like this in rules.
sendCommand(TvOverlay_Send_Notification, "Put your message here")
This has the advantage of not needing a thing type or UID, however you are limited to basic messages.

For advanced messages you need to use ACTIONS.

In these examples the `SonyTV` is the UniqueID of the `tvoverlaydisplay` thing.
Because the duration is not specified in the shorter versions, you can change the length of the time that the message is displayed for, by using the `remote` app on your phone, by starting the app on the TV, or by using the openHAB channel called `notificationDuration`.
The same can be done with the full methods if you use `null` and provide no value, the app will resort to using the default values for any provided with `null`.

getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendVideo("123", "Backyard", "Movement Detected","rtsp://admin:password@192.168.4.6:554/Streaming/Channels/102?transportmode=unicast&profile=Profile_1")

getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendText("124", null, "Frank arrived home")

Any picture files that are in your `openHAB-conf\html` folder can be used by using the static path.

getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendImage("125", "Temperatures", "Graph of all rooms","http://openhab:8080/static/graph.jpg")

The actions will return true or false based on if they succeed.
Do not forget you can also cancel long duration messages if you save the ID for using in other rules.

```
var messageID="123"
var result = getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendText(messageID,"The garage door is open for >5 minutes", "Please close the roller door")
  if(!result){
    sendLogNotification("Message failed to reach the TV as it may be turned off. The garage door has been left open.")
  }else{
    //You can use messageID to cancel or change the message to include a picture if you wish.
  }
```
