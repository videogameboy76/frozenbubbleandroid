# Introduction #

This Wiki entry details how to build Frozen Bubble for Android from the source code.

# Details #

To build the Frozen Bubble for Android application, you must first download the archive of the Android SDK ADT bundle.  The ADT (Android Development Tools) bundle comes with Eclipse - Eclipse is the IDE.  SDK stands for software development kit (the tools to build Android applications from source code), and IDE stands for integrated development environment (a graphical user interface that simplifies software development).

http://developer.android.com/sdk/index.html

Click the button on the side that says "Download the SDK ADT Bundle for Windows".  If you are using Linux or an Apple computer, you will need the SDK for the appropriate operating system instead.  After clicking this link, you will have to select the 32 or 64 bit version - the 64 bit version should be fine unless you are using Windows XP, which is 32-bit only.

After you extract the SDK, you need to run the SDK manager, which is a program that is placed in the root of the extracted SDK folder.  After you launch the SDK manager, you should see something like this:

![http://imgur.com/peufLFg.jpg](http://imgur.com/peufLFg.jpg)

At a minimum, you must select to install the Android 1.6 (API 4) SDK Platform.  This is the API level for Frozen Bubble, which means that an Android device must have version 1.6 or later of Android to play the game.

If you are building older versions of Frozen Bubble (version 1.12 and prior), at this point would be able to build the game - but now because of the addition of the native code for the music player, you need to download and install the Android NDK.  NDK stands for "native development kit", where "native" refers to C/C++ code.  The Android SDK only supports Java if you don't have the NDK.  So, you must install the NDK to build the latest version (currently 1.15) of Frozen Bubble.

http://developer.android.com/tools/sdk/ndk/index.html

Once you have extracted the NDK, launch Eclipse - it is located in the Eclipse folder of the extracted SDK folder.  You will have to add the path to the NDK to the environment, by selecting "Window → Preferences" in Eclipse.

Select "NDK" under "Android" in the list on the left.  **Note that the build path for the NDK isn't allowed to contain any spaces or the program won't compile**:

![http://imgur.com/fzNmvqa.jpg](http://imgur.com/fzNmvqa.jpg)

Next, you will have to get the source code for Frozen Bubble.  I recommend using Tortoise SVN (again, I hope you are using Windows) as it is by far the easiest source control tool to work with.

http://tortoisesvn.net/downloads.html

After you install Tortoise SVN, simply right-click over a folder, then select "Tortoise SVN → Repo-browser", and then enter the following URL:

http://frozenbubbleandroid.googlecode.com/svn

Right-click the topmost level in the dropdown list on the left, then select "Export".  Export everything into an empty folder on your computer:

![http://imgur.com/9qW7ZI8.jpg](http://imgur.com/9qW7ZI8.jpg)

**It has been noted that on the project source page, the instructions for checking out the code anonymously do not work, because this project does not have a trunk folder.**

https://code.google.com/p/frozenbubbleandroid/source/checkout

states that in order to check out the project anonymously you must use:

http://frozenbubbleandroid.googlecode.com/svn/trunk/ frozenbubbleandroid-read-only

Use this to anonymously check out the source instead:

http://frozenbubbleandroid.googlecode.com/svn/ frozenbubbleandroid-read-only

After the files are copied to your computer, you can import the project into Eclipse.  Open Eclipse, then select "File → Import".  Then select "Existing Projects into Workspace" under "General", and click "Next".

Browse to the folder you placed the source files in with Tortoise SVN, and check "Copy projects into workspace":

![http://imgur.com/hi7Ic3o.jpg](http://imgur.com/hi7Ic3o.jpg)

Now, you should be able to build the project in Eclipse.