# stream-recorder
Akka based REST http endpoint to record live streams, twitch, and youtube

Just a side project I started for use with a mobile app to record favorite
streams with the touch of a button.

This depends on [streamlink](https://streamlink.github.io/index.html) to be installed on the system. 

### Config
stream = actual stream I care about

test_stream = a test stream to test on

dir = where to store the files you want to record

### REST API
#### Record
http://localhost:8080/record this will record a file from the stream
link and save it to a file called <TIMESTAMP\>.mp4
#### Stop
http://localhost:8080/stop this will stop the current recording
#### Rename
http://localhost:8080/rename?name=<newName\> this will take the file
you just recorded and rename it to newName
