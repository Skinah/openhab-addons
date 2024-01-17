# AndroidNotifications Binding

This binding can talk with more then 1 Google Play store app, you only need one of them not both.
This binding will try to standardize the way you interact with the apps, so if one app disappears, gets buggy, or adds a large cost involved, you can switch to the other with minimal changes to openHAB.

The two supported apps so far are:
+ TvOverlay <https://play.google.com/store/apps/details?id=com.tabdeveloper.tvoverlay&hl=en_AU&gl=US>
+ Android TV Notifications <https://play.google.com/store/apps/details?id=de.cyberdream.androidtv.notifications.google&hl=en&gl=US>

Other apps which are not support but could be are:
+ Push TV <https://play.google.com/store/apps/details?id=de.andreashuth.pushtv>

Please refer to the github pages for help on how to setup the apps and TV settings needed.
<https://github.com/gugutab/TvOverlay>

 
## Supported Things

- `nfatvdisplay`: Android TV Notifications, add one for each TV that is setup and working with this app.
- `tvoverlaydisplay`: TvOverlay, add one for each TV that is setup and working with this app.

It is possible more things will get added to capture the notifications sent from your phone and other non display devices.
This is close to working for the Android TV Notifications app, but not yet ready for release.

## Discovery

Will be supported in the future, functionality first to make the binding useful.

## Thing Configuration

### `tvoverlaydisplay` Thing Configuration

| Name            | Type    | Description                           | Default | Required | Advanced |
|-----------------|---------|---------------------------------------|---------|----------|----------|
| hostname        | text    | Hostname or IP address of the device  | N/A     | yes      | no       |
| port            | number  | Port to access the device             |  5001   | yes      | yes      |

### `nfatvdisplay` Thing Configuration

| Name            | Type    | Description                           | Default | Required | Advanced |
|-----------------|---------|---------------------------------------|---------|----------|----------|
| hostname        | text    | Hostname or IP address of the device  | N/A     | yes      | no       |
| port            | number  | Port to access the device             |  7676   | yes      | yes      |

## Channels

Not yet documented, just use the main UI to see what is available and read the descriptions in the UI !
| Channel | Type   | Read/Write | Description                 |
|---------|--------|------------|-----------------------------|
| control | Switch | RW         | This is the control channel |

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
Because the duration is not specified in the shorter versions, you can change the length of the time the message is displayed for by using the `remote` app on your phone, by starting the app on the TV, or by using the channel called `notificationDuration`.
The same thing can be done with the full methods if you use `null` and provide no value, the app will resort to using the default values for any provided with `null`.

getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendVideo(123, "Backyard", "Movement Detected","rtsp://admin:password@192.168.4.6:554/Streaming/Channels/102?transportmode=unicast&profile=Profile_1")

getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendText(123, null, "Frank arrived home")

getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendImage(123, "Temperatures", "Graph of all rooms","http://openhab:8080/graph.jpg")
