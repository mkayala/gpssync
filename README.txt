A set of command-line tools for activity-tracking websites. 

My two main goals are to be able to copy all of my history from
Runkeeper to my hard drive* and then up to Strava, and then to upload
new runs to both services at once with a single command. In my mind,
this would be an improvement on the web sites for just uploading to
Runkeeper that currently exist. 

Limited support for communication with Garmin devices.  Limited
support at the moment for Runkeeper and Strava.  Presently backed by a
dumb CSV-based "database" of references to GPX files and users.

The principle motivation for the CSV base is to allow any spreadsheet
software to be used as a GUI for things like bulk marking some
activities to be uploaded to one service or another, or adding
a reference something old by hand. 

I'm too lazy to add copyright code to all of this, but I intend it to
be as reusable as possible. With attribution, if possible, but
whatever. There isn't a ton of good open source code against the
RunKeeper HealthGraph API, so if this is useful to anyone writing
something cool, that's great. 

*I have recently become aware that this part can also be done here: 
  http://runkeeper.com/exportDataForm 

## Supported operations:
- Runkeeper: 
  - Get an oauth token
  - Upload activities
  - Download activities
  - Save downloaded activity to GPX file (98% implemented)
- Strava:
  - Upload activities
- Garmin devices: 
  - Use 'gant' program to retrieve TCX files
  - Use 'gpsbabel' program to convert TCX files to GPX
  - Could pretty trivially be extended to use gpsbabel to download
    from devices where possible; my own device requires gant. 

## Compilation
I need to mavenize this or something. In the meantime, you need the
following java libraries: 
- httpclient4
  http://hc.apache.org/ 
- jackson 
  http://jackson.codehaus.org/
- jgpx (which has a couple of requirements of its own)
  http://code.google.com/p/jgpx/
- jetty (only for the Runkeeper oauth part)
  http://jetty.codehaus.org/jetty/
- joda time (lol, dates in java)
  http://joda-time.sourceforge.net/
- probably something else I forgot 

## Files to know about
You need a base directory containing these things
gpx/ -- a directory of gpx files
gpssync_activities.csv -- a list of activities  
gpssync_people.csv -- a list of people 
gpssync.properties -- configuration 

## gpssync_people.csv format
name,runkeeper_auth_token,strava_user,strava_pass

The tool RunkeeperOAuthTool can be run to get a Runkeeper auth token
for whoever is the logged-in user. Usage is 
  java  -Dgpssync.basedir=[baseDirectory]
    net.ruthandtodd.gpssync.rkoauth.RunkeeperOAuthTool user
and it writes the token to the csv file for you. 

## gpssync_activities.csv format
date,filename,type,services_list,people_list,upload_to_rk,upload_to_strava

type is one of RUN, HIKE, BIKE or omitted 
services_list is a |-delimited list of services (RUNKEEPER and/or STRAVA
  at the moment)
people_list is a |-delimited list of names from the people file
upload_to_* are flags (1 and true are true) that can be used with the 
  uploadMarked command to upload multiple activities at once. 

The upload flads and the columns of the people file are two of the
biggest limitations on adding services at the moment. A more general
setup is clearly possible, but you would probably want to go to a real
database at that point. 

## Configuration
Other than passing in your base directory at run time, the only
configuration currently required is related to Garmin devices. The
three properties are: 

gpssync.gantpath=[where the gant command lives]
gpssync.gantauth=[where the auth file you need lives]
gpssync.gpsbabelpath=[where the gpsbabel command lives]


## Usage
Run with a command like 
  java  -Dgpssync.basedir=[baseDirectory]
    net.ruthandtodd.gpssync.runner.Runner [some command] [some args]

Implemented commands include: 
addAllToUser user [type]
  connects to a Garmin device and adds all 'new' activities,
  associated with user. Uploads to services configured for the user.
addLatestToUser user [type]
  connects to a Garmin device and adds all 'new' activities, but only
  the latest is associated with user. Uploads to services configured
  for the user. 
addFromDirectory path
  adds any gpx found in path, with no user
uploadMarked
  uploads any activites marked

