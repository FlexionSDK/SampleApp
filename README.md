![Flexion Logo](/images/flexion-logo.png?raw=true)

Flexion SDK Sample App - "Fun Flowers"
=======

Introduction
---------------
This is an example app using the Flexion billing SDK. For more information on the Flexion SDK, see **[SDK Integration Guide](https://flexion.readme.io/docs/flexion-sdk-integration-introduction-en)**


This app is a simple game where the player can buy seeds and use it to 'grow'
randomly generated flowers. The player starts the game with a set amount of seeds. 
When the player grows a new flower, they consume a seed. If the player runs 
out of seeds, they can buy more using an in-app purchase.

The app uses the Connected Test Mode app.

Replace "<PERSONAL_ACCESS_TOKEN>" with your own access token in the build.gradle file to access the Flexion SDK. 


---------------
Item Consumption Mechanics
---------------

It's important to note the consumption mechanics for each item:

SEEDS: when seeds are purchased, the "seeds" item is then owned. We
consume it when we apply that item's effects to our app's world, which to
us means giving the player a fixed number of seeds. This happens immediately
after purchase! It's at this point (and not when the user grows a flower) that the
"seeds" item is CONSUMED. Consumption should always happen when your game
world was safely updated to apply the effect of the purchase. So, in an
example scenario:

+ BEFORE:      the player has 5 seeds
+ ON PURCHASE: the player has 5 seeds, "seeds" item is owned
+ IMMEDIATELY: the player has 25 seeds, "seeds" item is consumed
+ AFTER:       the player has 25 seeds, "seeds" item NOT owned any more

Another important point to notice is that it may so happen that
the application crashed (or anything else happened) after the user
purchased the "seeds" item, but before it was consumed. That's why,
on startup, we check if we own the "seeds" item, and, if so,
we have to apply its effects to our world and consume it. This
is also very important!


Build Instructions
---------------

1. Clone [The Sample App](https://github.com/FlexionSDK/SampleApp.git) repository to your local machine
2. Set the <PERSONAL_ACCESS_TOKEN> in the root build.gradle
3. Build with gradle


License
---------------

This application is licensed under the under the [Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0). Please note that this does not include the Flexion SDK. It may be used with the application but is not released under the Apache License Version 2.0. 

Fun Flowers copyright 2025 Flexion Mobile Ltd.