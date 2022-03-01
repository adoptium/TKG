[Related Issue](https://github.com/adoptium/TKG/issues/176)

The liveMonitor scripts reports on the number of openjdk tests in a particular directory and addresses the user story of a developer that is running tests locally and wishes to see current/live status.

User story includes:
Developer wishes to know how many tests in total there are in a particular directory (more granular than test targets defined in playlists)
Developer wishes to have current test status during a live run of tests on their laptop (more granular that TAP results)

### LiveMonitor Usage

Prerequisite: 

Python3 Installed

openjdk tests only
`export BUILD_LIST=openjdk`
before compiling or you need to run just openjdk tests and pipe them.

### count-java-tests
TKG has a script that counts how many test exists in a specified folder. This script currently works on just openjdk tests. It simply checks for the java files contains "@test" annotation. Here is an example of how you can use this script :

`aqa-tests/TKG# python3 -u scripts/liveMonitor_countTests/count-java-tests.py ../openjdk/openjdk-jdk/test/langtools/tools/javac/warnings/`

The output of the code above is : 

    
    Counting tests in 'aqa-tests/openjdk/openjdk-jdk/test/langtools/tools/javac/warnings/' ...

    Found 41 java files

    . ................ 11 
    6594914 ........... 2 
    6747671 ........... 1 
    6885255 ........... 1 

    7090499 ........... 1 
    AuxiliaryClass .... 3 
    NestedDeprecation . 1 
    suppress ......... 10 



    Found 30 java files containing @test

# jtreg-monitor
1. You need to change the verbose option of jtreg. In order to do that, you need to change 1 line of code in [/openjdk/openjdk.mk](https://github.com/adoptium/aqa-tests/blob/master/openjdk/openjdk.mk) file. 

    You need to change 

        `JTREG_BASIC_OPTIONS += -v:fail,error,time,nopass`  
    line to

        `JTREG_BASIC_OPTIONS += -v:all`

2. After that, you are ready to run the scripts. Here is the example of how you can do it : 

	`make _sanity.openjdk | python3 -u scripts/liveMonitor_countTests/jtreg-monitor.py`

