# Android Notifications Binding

This binding can talk with more then 1 Google Play Store app, you only need to install the one of your choice.
This binding will try to standardize the way you interact with any of the apps, so if one app disappears, gets buggy, gets caught sending data back home, or excessively nags with ads, you can switch to one of the others with minimal changes to openHAB.

To achieve this goal the other advantage will be that you do not need to learn the API of the app yourself, and can be sending notifications much faster.

The two supported apps so far are:

+ TvOverlay (working well) <https://play.google.com/store/apps/details?id=com.tabdeveloper.tvoverlay&hl=en_AU&gl=US>

+ Android TV Notifications (work in progress, only text based messages work for now) <https://play.google.com/store/apps/details?id=de.cyberdream.androidtv.notifications.google&hl=en&gl=US>

Other apps which are not yet supported, but could be are:

+ PiPup <https://play.google.com/store/apps/details?id=nl.rogro82.pipup>

+ Push TV (might be cloud based and not local like the others) <https://play.google.com/store/apps/details?id=de.andreashuth.pushtv>

+ Gotify This will push fully locally to phones.

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
| address         | text    | Hostname or IP address of the device  | N/A     | yes      | no       |
| port            | number  | Port to access the device             |  5001   | yes      | yes      |
| pollTime        | number  | Amount of seconds between polling TV  |   6     | yes      | no       |
| resendFixed     | boolean | Store fixed notifications until ONLINE| true    | yes      | no       |

### `nfatvdisplay` Thing Configuration

| Name            | Type    | Description                           | Default | Required | Advanced |
|-----------------|---------|---------------------------------------|---------|----------|----------|
| address         | text    | Hostname or IP address of the device  | N/A     | yes      | no       |
| port            | number  | Port to access the device             |  7676   | yes      | yes      |
| pollTime        | number  | Amount of seconds between polling TV  |   6     | yes      | no       |

## TvOverlay Channels

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
| pixelShift                | Switch    | RW         | Tells the TV to shift the fixed notifications around a few pixels every minute to prevent burn-in |

## TvOverlay Fixed Notifications

The icon and imageUrl fields need some extra explaining, as the binding has extra abilities above the raw TvOverlay API.
You can send icons and images in the following formats:

+ `mdi:trash-can-outline` Any text that starts with `mdi:` will get passed on and you can send the name of any MDI icon from a [searchable website](https://pictogrammers.com/library/mdi/)

+ `https://picsum.photos/300/300` Any text with http or https will get passed on and get downloaded by the TV, so there may be a delay while the TV downloads the image.

+ `download:https://picsum.photos/300/300` If it starts with `download:` the http or https URL will get downloaded by the binding, and then passed on as base64.
This can avoid a delay where the TV downloads the image which is further away from the triggering event.
You may find this allows a camera to pass on the image of a running person before they run out of view.

+ `file:/etc/openhab/icons/classic/qualityofservice-1.png` A path on mac or linux that will be sent as base64.

+ `file:c:\\foo\\qualityofservice-1.png` Path for a windows locations that will be sent as base64.

## Actions

Please read the example section below if these do not make sense to you.
The messageID is very important as you can use it to edit a message even if it is still displaying on the TV screen.
It can also be used to delete the message if it is still waiting in the queue. 

There are 3 shorter ways to send messages, and these use the apps default settings for any of the missing options.
There are as follows:

+ boolean sendText(String messageID, @Nullable String title, String message)

+ boolean sendImage(String messageID, @Nullable String title, @Nullable String message, String imageURL)

+ boolean sendVideo(String messageID, @Nullable String title, @Nullable String message, String videoURL)

The 3 longer versions give access to all variables and any marked as `@Nullable` can have `null` sent to use the programs default setting if you do not wish to provide one.
The down side is you will need to read the API to use these, where the shorter versions can be used easily and allow changing programs much easier.
There are as follows:    
        
+ boolean sendText(String messageID, @Nullable String title, String message, @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration)
                       
+ boolean sendImage(String messageID, @Nullable String title, @Nullable String message, String imageURL, @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor, @Nullable String corner, @Nullable Integer duration)            
            
+ boolean sendVideo(String messageID, @Nullable String title, @Nullable String message, String videoURL, @Nullable String largeIcon, @Nullable String smallIcon,
            @Nullable String smallIconColor, @Nullable String corner, @Nullable Integer duration)
            
+ boolean sendFixedNotification(String messageID, @Nullable String messageColor, @Nullable String message,
            @Nullable String icon, @Nullable String iconColor, @Nullable String borderColor,
            @Nullable String backgroundColor, @Nullable Integer expiration, @Nullable String shape,
            @Nullable Boolean visible)
            
When using TvOverlay, the icons can be any of the material design icons (mdi), for example  `mdi:home-alert-outline` and `mdi:chevron-up-circle-outline` are valid strings, but many more can be selected.

## Examples

The most simple method is to use the `sendNotification` channel if you link it to a String item, you can use it in rules like this.
sendCommand(TvOverlay_Send_Notification, "Put your message here")
This has the advantage of not needing a thing type or UID, however you are limited to basic messages.

For advanced messages you need to use ACTIONS.

In these examples the `SonyTV` is the UniqueID of the `tvoverlaydisplay` thing.
Because the duration is not specified in the shorter versions, you can change the length of the time that the message is displayed for, by using the `remote` app on your phone, by starting the app on the TV, or by using the openHAB channel called `notificationDuration`.
The same can be done with the full methods if you use `null` and provide no value, the app will resort to using the default values for any provided with `null`.

Most of these features have a channel that can be used.

```
getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendVideo("123", "Backyard", "Movement Detected","rtsp://admin:password@192.168.4.6:554/Streaming/Channels/102?transportmode=unicast&profile=Profile_1")
```

Download a URL locally on openHAB with the binding and then send it to the TV.
This method reduces lag if you find someone runs through the cameras field of view before the snapshot is taken at the TV.

```
getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:TheTV").sendImage("Backyard","Backyard" ,"Movement Detected","download:http://192.168.1.2:8080/ipcamera/backyard/ipcamera.jpg")
```

```
getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendText("124", null, "Frank arrived home")
```

Any picture files that are in your `openHAB-conf\html` folder can be used by providing the static path.

```
getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendImage("125", "Temperatures", "Graph of all rooms","http://openhab:8080/static/graph.jpg")
```

The actions will return true or false based on if they succeed.
You can also cancel long duration messages if you save the ID for use in other rules.

```
var messageID="123"
var result = getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:SonyTV").sendText(messageID,"The garage door is open for >5 minutes", "Please close the roller door")
  if(!result){
    sendLogNotification("Message failed to reach the TV as it may be turned off. The garage door has been left open.")
  }else{
    //You can use messageID to cancel or change the message to include a picture if you wish.
  }
```

## TvOverlay Fixed Notification Examples


Advanced rule to show how you can cancel a fixed notification because the TV was turned off.
You can remove single fixed notifications this way, or turn off the resending of all fixed notification with the config `resendFixed`.

```
var messageID="KettleBoiled"
var result = getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:TheTV").sendFixedNotification(messageID, null, null,"mdi:kettle-steam-outline","E13D3A", "EA6E6B", null, 300, "circle", true)
  if(!result){
    sendLogNotification("Kettle is now hot but as TV is off no point displaying this when the kettle is cold.")
    getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:TheTV").sendFixedNotification(messageID, null, null,"mdi:kettle-steam-outline","E13D3A", "EA6E6B", null, 300, "circle", false) //false removes this so will not display
  }
```

Display a logo of which coloured bin needs to get taken out to the street.

```
var dayYear = now.getDayOfYear
var dayModulus = dayYear%7 //account for when year does not start on the same day of the week as today.
var weekNumber =(dayYear-dayModulus)/7
logInfo("BinTvIconRule", "{} Weeks into the year",weekNumber)
  if( weekNumber % 2 == 0) {//Even number of weeks
    getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:TheTV").sendFixedNotification("RedBin", null, null,"mdi:trash-can-outline","E13D3A", "EA6E6B", null, 54000, "circle", true)
  }else{//54000 = 15 hours
    getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:TheTV").sendFixedNotification("YellowBin", null, null,"mdi:trash-can-outline","FEEA3C", "FCEF7B", null, 54000, "circle", true)
  }
```

Display a warning logo that the sprinklers will be running soon to bring any washing inside.

```
getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:TheTV").sendFixedNotification("SprinklerWarning", null, null,"mdi:sprinkler-variant","62B1F0", "85C3F5", null, 21600, "circle", true)
```

Example of providing your own icon from a file on the Linux file system.

```
getActions("androidnotifications", "androidnotifications:tvoverlaydisplay:TheTV").sendFixedNotification("WhiteWifi", null, null,"file:/etc/openhab/icons/classic/qualityofservice-1.png","ffffff", null, null, 10, "circle", true)
```
