################################################################################
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

use strict;
use warnings;
use Data::Dumper;
use File::Basename;
use File::Path qw/make_path/;
use lib ".";
require "moveDmp.pl";

my $resultFile;
my $failuremkarg;
my $tapFile;
my $tapPath;
my $tapName;
my $platFile;
my $diagnostic = 'failure';
my $jdkVersion = "";
my $jdkImpl = "";
my $jdkVendor = "";
my $buildList = "";
my $spec = "";
my $customTarget = "";
my $testTarget = "";

for (my $i = 0; $i < scalar(@ARGV); $i++) {
	my $arg = $ARGV[$i];
	if ($arg =~ /^\-\-failuremk=/) {
		($failuremkarg) = $arg =~ /^\-\-failuremk=(.*)/;
	} elsif ($arg =~ /^\-\-resultFile=/) {
		($resultFile) = $arg =~ /^\-\-resultFile=(.*)/;
	} elsif ($arg =~ /^\-\-tapPath=/) {
		($tapPath) = $arg =~ /^\-\-tapPath=(.*)/;
	} elsif ($arg =~ /^\-\-tapName=/) {
		($tapName) = $arg =~ /^\-\-tapName=(.*)/;
	} elsif ($arg =~ /^\-\-platFile=/) {
		($platFile) = $arg =~ /^\-\-platFile=(.*)/;
	} elsif ($arg =~ /^\-\-diagnostic=/) {
		($diagnostic) = $arg =~ /^\-\-diagnostic=(.*)/;
	} elsif ($arg =~ /^\-\-jdkVersion=/) {
		($jdkVersion) = $arg =~ /^\-\-jdkVersion=(.*)/;
	} elsif ($arg =~ /^\-\-jdkImpl=/) {
		($jdkImpl) = $arg =~ /^\-\-jdkImpl=(.*)/;
	} elsif ($arg =~ /^\-\-jdkVendor=/) {
		($jdkVendor) = $arg =~ /^\-\-jdkVendor=(.*)/;
	} elsif ($arg =~ /^\-\-buildList=/) {
		($buildList) = $arg =~ /^\-\-buildList=(.*)/;
	} elsif ($arg =~ /^\-\-spec=/) {
		($spec) = $arg =~ /^\-\-spec=(.*)/;
	} elsif ($arg =~ /^\-\-customTarget=/) {
		($customTarget) = $arg =~ /^\-\-customTarget=(.*)/;
	} elsif ($arg =~ /^\-\-testTarget=/) {
		($testTarget) = $arg =~ /^\-\-testTarget=(.*)/;
	}
}

if (!$failuremkarg) {
	die "Please specify a valid file path using --failuremk= option!";
}

my $spec2platform = readPlatform();
my $failures = resultReporter($spec2platform);
failureMkGen($failuremkarg, $failures);

sub readPlatform {
	my %map;
	my $fhIn;
	if (open($fhIn, '<', $platFile)) {
		while ( my $line = <$fhIn> ) {
			$line =~ s/#.*//;
			if ($line =~ /(.*)=(.*)/) {
				$map{$1} = $2;
			}
		}
	}
	return \%map;
}

sub resultReporter {
	my ($spec2platform) = @_;
	my $numOfExecuted = 0;
	my $numOfFailed = 0;
	my $numOfPassed = 0;
	my $numOfSkipped = 0;
	my $numOfDisabled = 0;
	my $numOfTotal = 0;
	my $runningDisabled = 0;
	my @passed;
	my @failed;
	my @disabled;
	my @capSkipped;
	my $tapString = '';
	my $fhIn;

	print "\n\n";

	if (open($fhIn, '<', $resultFile)) {
		while ( my $result = <$fhIn> ) {
			if ($result =~ /===============================================\n/) {
				my $output = "    output:\n      |\n";
				$output .= '        ' . $result;
				my $testName = '';
				my $startTime = 0;
				my $endTime = 0;
				while ( $result = <$fhIn> ) {
					# remove extra carriage return
					$result =~ s/\r//g;
					$output .= '        ' . $result;
					if ($result =~ /Running test (.*) \.\.\.\n/) {
						$testName = $1;
					} elsif ($result =~ /^\Q$testName\E Start Time: .* Epoch Time \(ms\): (.*)\n/) {
						$startTime = $1;
					} elsif ($result =~ /^\Q$testName\E Finish Time: .* Epoch Time \(ms\): (.*)\n/) {
						$endTime = $1;
						$tapString .= "    duration_ms: " . ($endTime - $startTime) . "\n  ...\n";
						last;
					} elsif ($result eq ($testName . "_PASSED\n")) {
						$result =~ s/_PASSED\n$//;
						push (@passed, $result);
						$numOfPassed++;
						$numOfTotal++;
						$tapString .= "ok " . $numOfTotal . " - " . $result . "\n";
						$tapString .= "  ---\n";
						if ($diagnostic eq 'all') {
							$tapString .= $output;
						}
					} elsif ($result eq ($testName . "_FAILED\n")) {
						$result =~ s/_FAILED\n$//;
						push (@failed, $result);
						$numOfFailed++;
						$numOfTotal++;
						$tapString .= "not ok " . $numOfTotal . " - " . $result . "\n";
						$tapString .= "  ---\n";
						if (($diagnostic eq 'failure') || ($diagnostic eq 'all')) {
							$tapString .= $output;
						}
						if ($spec =~ /zos/) {
							my $dmpDir = dirname($resultFile).'/'.$testName;
							moveTDUMPS($output, $dmpDir, $spec);
						}
					} elsif ($result eq ($testName . "_DISABLED\n")) {
						$result =~ s/_DISABLED\n$//;
						push (@disabled, $result);
						$numOfDisabled++;
						$numOfTotal++;
						$tapString .= "ok " . $numOfTotal . " - " . $testName . " # skip (disabled)\n";
						$tapString .= "  ---\n";
					} elsif ($result eq ("Test is disabled due to:\n")) {
						$runningDisabled = 1;
					} elsif ($result =~ /(capabilities \(.*?\))\s*=>\s*${testName}_SKIPPED\n/) {
						my $capabilities = $1;
						push (@capSkipped, "$testName - $capabilities");
						$numOfSkipped++;
						$numOfTotal++;
						$tapString .= "ok " . $numOfTotal . " - " . $testName . " # skip\n";
						$tapString .= "  ---\n";
					} elsif ($result =~ /(jvm options|platform requirements).*=>\s*${testName}_SKIPPED\n/) {
						$numOfSkipped++;
						$numOfTotal++;
						$tapString .= "ok " . $numOfTotal . " - " . $testName . " # skip\n";
						$tapString .= "  ---\n";
					}
				}
			}
		}

		close $fhIn;
	}

	#generate console output
	print "TEST TARGETS SUMMARY\n";
	print "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n";

	if ($numOfDisabled != 0) {
		printTests(\@disabled, "DISABLED test targets");
		print "\n";
	}

	# only print out skipped due to capabilities in console
	if (@capSkipped) {
		printTests(\@capSkipped, "SKIPPED test targets due to capabilities");
		print "\n";
	}

	if ($numOfPassed != 0) {
		printTests(\@passed, "PASSED test targets");
		print "\n";
	}

	if ($numOfFailed != 0) {
		printTests(\@failed, "FAILED test targets");
		print "\n";
	}

	$numOfExecuted = $numOfTotal - $numOfSkipped - $numOfDisabled;

	print "TOTAL: $numOfTotal   EXECUTED: $numOfExecuted   PASSED: $numOfPassed   FAILED: $numOfFailed";
	# Hide numOfDisabled when running disabled tests list.
	if ($runningDisabled == 0) {
		print "   DISABLED: $numOfDisabled";   
	}
	print "   SKIPPED: $numOfSkipped";
	print "\n";
	if ($numOfTotal > 0) {
		# set tap file name if not given
		if ($tapName eq '') {
			my $platform = "";
			if (exists $spec2platform->{$spec}) {
				$platform  = $spec2platform->{$spec};
			}
			if (index($testTarget, "testList") != -1) {
    			$testTarget = 'testList';
			} 
			$tapName = "Test_openjdk$jdkVersion\_$jdkImpl\_$testTarget\_$platform.tap";
		}
		$tapFile = $tapPath.$tapName;
		#generate tap output
		my $dir = dirname($tapFile);
		if (!(-e $dir and -d $dir)) {
			make_path($dir);
		}
		open(my $fhOut, '>', $tapFile) or die "Cannot open file $tapFile!";
		my $timeStamp = gmtime();

		#add AQACert.log content in TAP file
		my $AQACert = $tapPath."../AQACert.log";
		my $AQACertContent = `cat $AQACert`;
		$AQACertContent =~ s/\n/\n# /g;
		print $fhOut "# AQACert.log content: \n# " . $AQACertContent . "\n";

		#add SHA.txt content in TAP file
		my $SHAFile = $tapPath."../SHA.txt";
		my $SHAContent = `cat $SHAFile`;
		$SHAContent =~ s/\n/\n# /g;
		print $fhOut "#SHA.txt content: \n# " . $SHAContent . "\n";

		print $fhOut "# Timestamp: " . $timeStamp . " UTC \n";

		#add customTarget to TAP file
 		if ($customTarget ne '') {
 			print $fhOut "# custom target: " . "${customTarget}"
 		}

		print $fhOut "1.." . $numOfTotal . "\n";
		print $fhOut $tapString;
		close $fhOut;
		if ($numOfFailed == 0) {
			print "ALL TESTS PASSED\n";
		}
	}

	print "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n";

	if ($numOfFailed != 0) {
		my $buildParam =  "";
		if ($buildList ne '') {
			$buildParam = "&BUILD_LIST=" . $buildList;
		}
		my $platformParam = "";
		if (exists $spec2platform->{$spec}) {
			$platformParam = "&PLATFORM=" . $spec2platform->{$spec};
		}
		my $customTargetParam = "";
		if ($customTarget ne '') {
			$customTargetParam = "&CUSTOM_TARGET=" . $customTarget;
		}
		my $hudsonUrl = $ENV{'HUDSON_URL'};
		if ((!defined $hudsonUrl) || ($hudsonUrl eq '')) {
			$hudsonUrl = "https://ci.adoptopenjdk.net/";
			my $vendorParam =  "";
			if ($jdkVendor ne '') {
				$vendorParam = "&JDK_VENDOR=" . $jdkVendor;
			}
			my $rebuildLinkBase = "parambuild/?JDK_VERSION=$jdkVersion&JDK_IMPL=$jdkImpl$vendorParam$buildParam$platformParam$customTargetParam";
			print "To rebuild the failed test in a jenkins job, copy the following link and fill out the <Jenkins URL> and <FAILED test target>:\n";
			print "<Jenkins URL>/$rebuildLinkBase&TARGET=<FAILED test target>\n\n";
			print "For example, to rebuild the failed tests in <Jenkins URL>=${hudsonUrl}job/Grinder, use the following links:\n";

			foreach my $failedTarget (@failed) {
				print "${hudsonUrl}job/Grinder/$rebuildLinkBase&TARGET=$failedTarget\n";
			}
			if ($numOfFailed > 1) {
				my $failedList = "&TARGET=testList%20TESTLIST=" . join(",", @failed);
				print "rebuild the failed tests in one link:\n";
				print "${hudsonUrl}job/Grinder/$rebuildLinkBase$failedList\n";
			}
			print "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n";
		}
	}
	unlink($resultFile);

	return \@failed;
}

sub printTests() {
	my ($tests, $msg) = @_;
	print "$msg:\n\t";
	print join("\n\t", @{$tests});
	print "\n";
}

sub failureMkGen {
	my $failureMkFile = $_[0];
	my $failureTargets = $_[1];
	if (@$failureTargets) {
		open( my $fhOut, '>', $failureMkFile ) or die "Cannot create make file $failureMkFile!";
		my $headerComments =
		"########################################################\n"
		. "# This is an auto generated file. Please do NOT modify!\n"
		. "########################################################\n"
		. "\n";
		print $fhOut $headerComments;
		print $fhOut ".DEFAULT_GOAL := failed\n\n"
					. "D = /\n\n"
					. "ifndef TEST_ROOT\n"
					. "\tTEST_ROOT := \$(shell pwd)\$(D)..\n"
					. "endif\n\n"
					. "include settings.mk\n\n"
					. "failed:\n";
		foreach my $target (@$failureTargets) {
			print $fhOut '	@$(MAKE) -C $(TEST_ROOT) -f autoGen.mk ' . $target . "\n";
		}
		print $fhOut "\n.PHONY: failed\n"
					. ".NOTPARALLEL: failed";
		close $fhOut;
	}
}
