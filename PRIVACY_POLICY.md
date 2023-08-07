# Privacy Policy

This privacy policy goes into detail about how your data is used and collected. All of the code for this app (InterprofileTransferrer) is licensed under the MIT license in this GitHub repository.

This app and privacy policy are developed by VeryRandomCreator.

When this privacy policy mentions "I", it refers to the developer or the writer of the privacy policy and developer of the app, VeryRandomCreator.
When this privacy policy mentions "you" or "your", it refers to the user or the belonging(s) of the user, respectively.

## Personal Information

I have not developed this app with any intention or knowledge that personal information about the user is sent or used. Personal information is information such as your name, phone number, email, address, or anything identifiable.

## Device Information

No information unique to your device (ex: ids) is saved or sent.

## Permissions

### `INTERNET`

The `android.permission.INTERNET` permission is requested in the 'AndroidManifest.xml' file: {LINK TO LINE IN MANIFEST}

This permission is required in order to manage the sockets needed to transfer files. The sockets only have access to ports on `127.0.0.1`, preventing files from being sent and received to other ip addresses. The specific port can be chosen to the user's discretion.  

### `POST_NOTIFICATIONS`

The `android.permission.POST_NOTIFICATIONS` permission is requested in the 'AndroidManifest.xml' file: {LINK TO LINE IN MANIFEST}

This permission allows the app to start the notifications needed to create the foreground services to run the file transfering mechanism.

### `FOREGROUND_SERVICE`

The `android.permission.FOREGROUND_SERVICE` permission is requested in the 'AndroidManifest.xml' file: {LINK TO LINE IN MANIFEST}

This permission allows the app to create a foreground service for the client and server parts of the app

### `VIBRATE`

The `android.permission.VIBRATE` permission is requested in the 'AndroidManifest.xml' file: {LINK TO LINE IN MANIFEST}

This permission allows the app to vibrate the device.

## Attributions

All attributions and source notices can be found in the NOTICE.md file {LINK TO NOTICE.md file}

## Final Comments

I originally made this app to solve the problem I faced when transferring files between GrapheneOS profiles. I decided to publish it to gain experience and grow an online developer presence.

More details about the app can be found in README.md {LINK TO PAGE}.

## Contact

If there are any problems, questions, or concerns do not hesitate to contact my email.

Thank you,  
VeryRandomCreator

veryrandomcreator@gmail.com
