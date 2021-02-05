# TKG

TestKitGen (TKG) is a lightweight test harness for bringing together a diverse set of tests or commands under some common behaviour.  All testing that runs at the AdoptOpenJDK project is run using TKG to yoke together many types of testing (that run on a variety of test frameworks).  

TKG standardizes: 
- test target generation (generates make targets based on contents of playlist.xml files)
- test output, defaulting to Test Anything Protocol (TAP) as the simplest, lowest common denominator
- the way tests are tagged, grouped, included and excluded (categorizing by elements in the playlist file, by group, impl, version, etc)
