# LiveMonitor Scripts

The liveMonitor scripts report on the number of openjdk tests or the live status in a particular directory, addressing the user story of a developer that is running tests locally and wishes to see the live status.

## User stories:
- Developer wishes to know how many tests in total there are in a particular directory (more granular than test targets defined in playlists)

- Developer wishes to have current test status during a live run of tests on their laptop (more granular that TAP results)

[Related Issue](https://github.com/adoptium/TKG/issues/176)


## LiveMonitor Prerequisites

- Python3 Installed

- openjdk tests only, these tests are required to be piped before compiling the script

`export BUILD_LIST=openjdk`





## count-java-tests

The count-java-tests script counts how many test exists in a specified folder.It simply checks for the java files contains "@test" annotation. This script currently works on just openjdk tests.


#### Sample usage:

`aqa-tests/TKG# python3 -u scripts/liveMonitor_countTests/count-java-tests.py ../openjdk/openjdk-jdk/test/langtools/tools/javac/warnings/`

#### Sample output:

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

## jtreg-monitor

The jtreg-monitor provides the live status of the TAP results that have been running.


Before running the scripts, the verbose option of jtreg needs to be changed. 


In [/openjdk/openjdk.mk](https://github.com/adoptium/aqa-tests/blob/master/openjdk/openjdk.mk) file:

    `JTREG_BASIC_OPTIONS += -v:fail,error,time,nopass`  
needs to be changed to:

    `JTREG_BASIC_OPTIONS += -v:all`

#### Sample usage:

	`make _sanity.openjdk | python3 -u scripts/liveMonitor_countTests/jtreg-monitor.py`


#### Sample output:

    /<localAddress>/TKG# % make _sanity.openjdk | python3 -u scripts/liveMonitor_countTests/jtreg-monitor.py

    ============== MONITORING STDIN FOR JTREG RESULTS ==============

    openjdk version "17.0.2" 2022-01-18
    IBM Semeru Runtime Open Edition 17.0.2.0 (build 17.0.2+8)
    Eclipse OpenJ9 VM 17.0.2.0 (build openj9-0.30.0, JRE 17 Mac OS X amd64-64-Bit Compressed References 20220127_94 (JIT enabled, AOT enabled)
    OpenJ9   - 9dccbe076
    OMR      - dac962a28
    JCL      - 64cd399ca28 based on jdk-17.0.2+8)

    Attempting to destroy all caches in cacheDir /<localAddress>/javasharedresources/

    JVMSHRC806I Compressed references persistent shared cache "sharedcc_<localAddress>" has been destroyed. Use option -Xnocompressedrefs if you want to destroy a non-compressed references cache.
    JVMSHRC005I No shared class caches available
    0:00:13.373978 / ...ng/annotation/AnnotationType/AnnotationTypeDeadlockTest.java / {}
    TEST (1) 0:00:00.000705 @ 2022-03-07 23:19:09): java/lang/annotation/AnnotationType/AnnotationTypeDeadlockTest.java Passed
    0:00:13.374683 / ...ng/annotation/AnnotationType/AnnotationTypeDeadlockTest.java / {'Passed': 1}
    0:00:13.760148 / java/lang/HashCode.java / {'Passed': 1}                                        
    TEST (2) 0:00:00.000953 @ 2022-03-07 23:19:10): java/lang/HashCode.java Passed
    0:00:13.761101 / java/lang/HashCode.java / {'Passed': 2}
    0:00:14.111257 / java/lang/Compare.java / {'Passed': 2} 
    TEST (3) 0:00:00.001275 @ 2022-03-07 23:19:10): java/lang/Compare.java Passed
    0:00:14.112532 / java/lang/Compare.java / {'Passed': 3}
    0:00:14.112637 / java/lang/IntegralPrimitiveToString.java / {'Passed': 3}

    ============== RESULTS ==============


    ============== Passed (3) ==============

    Passed (1/3796) - TEST (1) 0:00:00.000705 @ 2022-03-07 23:19:09): java/lang/annotation/AnnotationType/AnnotationTypeDeadlockTest.java Passed
    Passed (2/3796) - TEST (2) 0:00:00.000953 @ 2022-03-07 23:19:10): java/lang/HashCode.java Passed
    Passed (3/3796) - TEST (3) 0:00:00.001275 @ 2022-03-07 23:19:10): java/lang/Compare.java Passed

    ============== Error (3) ==============

    Error (1/10) - TEST (129) 0:00:00.000753 @ 2022-03-07 23:19:27): java/lang/ClassLoader/nativeLibrary/NativeLibraryTest.java 
    -------> Error. Use -nativepath to specify the location of native code

    Error (2/10) - TEST (409) 0:00:00.000211 @ 2022-03-07 23:21:50): java/lang/ProcessBuilder/Basic.java#id0 
    -------> Error. Use -nativepath to specify the location of native code

    Error (3/10) - TEST (536) 0:00:00.000235 @ 2022-03-07 23:22:15): java/lang/RuntimeTests/loadLibrary/exeLibraryCache/LibraryFromCache.java 
    -------> Error. Use -nativepath to specify the location of native code

    ============== Failed (3) ==============

    Failed (1/10) - TEST (559) 0:00:00.000404 @ 2022-03-07 23:22:19): java/lang/StackWalker/DumpStackTest.java 
    -------> Failed. Execution failed: `main' threw exception: java.lang.RuntimeException: StackTraceElements mismatch at index 3. Expected [DumpStackTest.testLambda(DumpStackTest.java)], but get [DumpStackTest$$Lambda$3/0x000000008808c1a0.accept(Unknown Source)]

    Failed (2/10) - TEST (1350) 0:00:00.000345 @ 2022-03-07 23:27:03): java/lang/StackWalker/DumpStackTest.java 
    -------> Failed. Execution failed: `main' threw exception: java.lang.RuntimeException: StackTraceElements mismatch at index 3. Expected [DumpStackTest.testLambda(DumpStackTest.java)], but get [DumpStackTest$$Lambda$3/0x000000007811c1a0.accept(Unknown Source)]

    Failed (3/10) - TEST (2011) 0:00:00.000416 @ 2022-03-07 23:38:28): java/util/Currency/CurrencyTest.java 
    -------> Failed. Execution failed: `main' threw exception: java.lang.RuntimeException: Test data and JRE's currency data are inconsistent. test: (file: 3 data: 170), JRE: (file: 3 data: 169)

    ============== SUMMARY ==============

    Started: 2022-03-07 23:18:56
    Ended: 2022-03-08 00:04:06
    Time elapsed 0:45:09.979457
    Number of tests: 9
    Results: {'Passed': 3, 'Error': 3, 'Failed': 3}