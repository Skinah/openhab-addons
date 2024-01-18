# AndroidNotifications Binding

This binding can talk with more then 1 Google Play store app, you only need one of them not both.
This binding will try to standardize the way you interact with the apps, so if one app disappears, gets buggy, or adds a large cost, you can switch to one of the others with minimal changes to openHAB.

The two supported apps so far are:

+ TvOverlay <https://play.google.com/store/apps/details?id=com.tabdeveloper.tvoverlay&hl=en_AU&gl=US>
+ Android TV Notifications <https://play.google.com/store/apps/details?id=de.cyberdream.androidtv.notifications.google&hl=en&gl=US>

Other apps which are not yet supported, but could be are:

+ Push TV <https://play.google.com/store/apps/details?id=de.andreashuth.pushtv>

Please refer to the github pages for help on how to setup the apps and any TV settings that are needed.
<https://github.com/gugutab/TvOverlay>

## Supported Things

- `nfatvdisplay`: Android TV Notifications, add one for each TV that is setup and working with this app.
- `tvoverlaydisplay`: TvOverlay, add one for each TV that is setup and working with this app.

It is possible more things will get added to capture the notifications sent from your phone and other non display devices.
This is close to working for the Android TV Notifications app, but not yet ready for release.

## Discovery

Is now supported.
If you have both apps installed on your TV, only the first one discovered one will get added.

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
| hotCorner                 | String    | RW         | Change which corner of the screen the messages and clock get displayed in |
| notificationDuration      | Number:Time | RW       | Change the default amount of time the messages get displayed for if `null` is used in a message |
| displayNotifications      | Switch    | RW         | Show or hide any normal non fixed type notifications |

## Actions

Please read the example section below if these do not make sense to you.
The messageID is very important as you can use it to edit a message even if it is still displaying on the TV screen.
It can also be used to delete the message if it is still waiting in the queue. 

There are 3 shorter ways to send messages, and these use the apps default settings for the missing options.
There are as follows:

+ sendText(int messageID, @Nullable String title, String message)

+ sendImage(int messageID, @Nullable String title, @Nullable String message, String imageURL)

+ sendVideo(int messageID, @Nullable String title, @Nullable String message, String videoURL)

The 3 longer versions give access to all variables and any marked as `@Nullable` can have `null` sent to use the programs default setting if you do not wish to provide one.
There are as follows:    
        
+ sendText(int messageID, @Nullable String title, String message, @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration)
                       
+ sendImage(int messageID, @Nullable String title, @Nullable String message, String imageURL, @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor, @Nullable String corner, @Nullable Integer duration)            
            
+ sendVideo(int messageID, @Nullable String title, @Nullable String message, String videoURL, @Nullable String largeIcon, @Nullable String smallIcon,
            @Nullable String smallIconColor, @Nullable String corner, @Nullable Integer duration)

## Examples

In these examples the `SonyTV` is the UniqueID of the `tvoverlaydisplay` thing.
Because the duration is not specified in the shorter versions, you can change the length of the time the message is displayed for by using the `remote` app on your phone, or by starting the app on the TV, or by using the channel called `notificationDuration`.
The same thing can be done with the full methods if you use `null` and provide no value, the app will resort to using the default values for any provided with `null`.

getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendVideo(123, "Backyard", "Movement Detected","rtsp://admin:password@192.168.4.6:554/Streaming/Channels/102?transportmode=unicast&profile=Profile_1")

getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendText(124, null, "Frank arrived home")

getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendImage(125, "Temperatures", "Graph of all rooms","http://openhab:8080/graph.jpg")


An even simpler method is also available using the `sendNotification` channel if you link it to a String item.

sendCommand(TvOverlay_Send_Notification, "Put your message here")
