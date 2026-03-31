The goal of this project is to create a really simple Android app (to be installed by APK) to provide basic paging/push notification functionaltiy. 

Use-case / intended context: for my wife and I, as parents. I want an extremely simple tool that does one thing: send a Pushover notification to the other person. 

To support this use case, only two buttons:

- Emergency - sends payload to saved contact at maximum alert level 
- Call me ASAP - one tier down but for non-emergency use

As this is intended for a couple, a single settings page with these params:

- Your pushover API key (plus user key if also needed but I don't think so)
- Spouse's pushover user key 

And param's for:

- Your name (user)
- Spouse's name (recipient)

Alerts are formatted as:

Family Pager: {spouse} emergency/SOS!

Family Pager: {spouse} - call me ASAP