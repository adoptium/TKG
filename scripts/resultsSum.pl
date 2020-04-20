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
my $diagnostic = 'failure';
my $jdkVersion = "";
my $jdkImpl = "";
my $buildList = "";
my $spec = "";
my $customTarget = "";
my %spec2platform = (
	'linux_x86-64_cmprssptrs'      => 'x86-64_linux',
	'linux_x86-64'                 => 'x86-64_linux_xl',
	'linux_arm'                    => 'aarch32_linux',
	'linux_aarch64_cmprssptrs'     => 'aarch64_linux',
	'linux_aarch64'                => 'aarch64_linux_xl',
	'linux_ppc-64_cmprssptrs_le'   => 'ppc64le_linux',
	'linux_ppc-64_le'              => 'ppc64le_linux_xl',
	'linux_390-64_cmprssptrs'      => 's390x_linux',
	'linux_390-64'                 => 's390x_linux_xl',
	'aix_ppc-64_cmprssptrs'        => 'ppc64_aix',
	'zos_390-64_cmprssptrs'        => 's390x_zos',
	'osx_x86-64_cmprssptrs'        => 'x86-64_mac',
	'osx_x86-64'                   => 'x86-64_mac_xl',
	'win_x86-64_cmprssptrs'        => 'x86-64_windows',
	'win_x86-64'                   => 'x86-64_windows_xl',
	'win_x86'                      => 'x86-32_windows',
	'sunos_sparcv9-64_cmprssptrs'  => 'sparcv9_solaris',
);

for (my $i = 0; $i < scalar(@ARGV); $i++) {
	my $arg = $ARGV[$i];
	if ($arg =~ /^\-\-failuremk=/) {
		($failuremkarg) = $arg =~ /^\-\-failuremk=(.*)/;
	} elsif ($arg =~ /^\-\-resultFile=/) {
		($resultFile) = $arg =~ /^\-\-resultFile=(.*)/;
	} elsif ($arg =~ /^\-\-tapFile=/) {
		($tapFile) = $arg =~ /^\-\-tapFile=(.*)/;
	} elsif ($arg =~ /^\-\-diagnostic=/) {
		($diagnostic) = $arg =~ /^\-\-diagnostic=(.*)/;
	} elsif ($arg =~ /^\-\-jdkVersion=/) {
		($jdkVersion) = $arg =~ /^\-\-jdkVersion=(.*)/;
	} elsif ($arg =~ /^\-\-jdkImpl=/) {
		($jdkImpl) = $arg =~ /^\-\-jdkImpl=(.*)/;
	} elsif ($arg =~ /^\-\-buildList=/) {
		($buildList) = $arg =~ /^\-\-buildList=(.*)/;
	} elsif ($arg =~ /^\-\-spec=/) {
		($spec) = $arg =~ /^\-\-spec=(.*)/;
	} elsif ($arg =~ /^\-\-customTarget=/) {
		($customTarget) = $arg =~ /^\-\-customTarget=(.*)/;
	}
}

if (!$failuremkarg) {
	die "Please specify a valid file path using --failuremk= option!";
}

my $failures = resultReporter();
failureMkGen($failuremkarg, $failures);

sub resultReporter {
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
		#generate tap output
		my $dir = dirname($tapFile);
		if (!(-e $dir and -d $dir)) {
			make_path($dir);
		}
		open(my $fhOut, '>', $tapFile) or die "Cannot open file $tapFile!";
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
		if (exists $spec2platform{$spec}) {
			$platformParam = "&PLATFORM=" . $spec2platform{$spec};
		}
		my $customTargetParam = "";
		if ($customTarget ne '') {
			$customTargetParam = "&CUSTOM_TARGET=" . $customTarget;
		}
		my $hudsonUrl = $ENV{'HUDSON_URL'};
		if (length $hudsonUrl == 0) {
			$hudsonUrl = "https://ci.adoptopenjdk.net/";
		}
		print "To rebuild the failed test in a jenkins job, copy the following link and fill out the <Jenkins URL> and <FAILED test target>:\n";
		print "<Jenkins URL>/parambuild/?JDK_VERSION=$jdkVersion&JDK_IMPL=$jdkImpl$buildParam$platformParam$customTargetParam&TARGET=<FAILED test target>\n\n";
		print "For example, to rebuild the failed tests in <Jenkins URL>=${hudsonUrl}job/Grinder, use the following links:\n";
		foreach my $failedTarget (@failed) {
			print "${hudsonUrl}job/Grinder/parambuild/?JDK_VERSION=$jdkVersion&JDK_IMPL=$jdkImpl$buildParam$platformParam$customTargetParam&TARGET=$failedTarget\n";
		}
		print "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n";
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