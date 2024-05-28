#!/usr/bin/perl

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
use Digest::SHA;
use Getopt::Long;
use File::Copy;
use File::Spec;
use File::Path qw(make_path);

# define test src root path
my $path;
# define task
my $task = "default";
my $dependencyList = "all";
my $customUrl = "";
my $curlOpts = "";

GetOptions ("path=s" => \$path,
			"task=s" => \$task,
			"dependencyList=s" => \$dependencyList,
			"customUrl=s" => \$customUrl,
			"curlOpts=s" => \$curlOpts)
	or die("Error in command line arguments\n");

if (not defined $path) {
	die "ERROR: Download path not defined!\n"
}

if (! -d $path) {
	print "$path does not exist, creating one.\n";
	my $isCreated = make_path($path, {chmod => 0777, verbose => 1});
}

# define directory path separator
my $sep = File::Spec->catfile('', '');

print "--------------------------------------------\n";
print "path is set to $path\n";
print "task is set to $task\n";
print "dependencyList is set to $dependencyList\n";

# Define a a hash for each dependent jar
# Contents in the hash should be:
#   url - required. url to download the dependent jar
#   fname - required. the dependent jar name
#   sha1 - sha1 value for the dependent jar (can be skipped if both shaurl and shafn are provided)
#   shaurl - optional. url to download the sha file
#   shafn  - optional. sha file name (has to be used with shaurl)
#   shaalg - optional. sha is calculated based on shaalg (default value is sha1)

my %base = (
	asm_all => {
		url => 'https://repo1.maven.org/maven2/org/ow2/asm/asm-all/6.0_BETA/asm-all-6.0_BETA.jar',
		fname => 'asm-all.jar',
		sha1 => '535f141f6c8fc65986a3469839a852a3266d1025'
	},
	asm => {
		url => 'https://repository.ow2.org/nexus/content/repositories/releases/org/ow2/asm/asm/9.0-beta/asm-9.0-beta.jar',
		fname => 'asm.jar',
		sha1 => 'a0f58cad836a410f6ba133aaa209aea7e54aaf8a'
	},
	byte_buddy => {
		url => 'https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.14.12/byte-buddy-1.14.12.jar',
		fname => 'byte-buddy.jar',
		sha1 => '6e37f743dc15a8d7a4feb3eb0025cbc612d5b9e1'
	},
	byte_buddy_agent => {
		url => 'https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy-agent/1.14.12/byte-buddy-agent-1.14.12.jar',
		fname => 'byte-buddy-agent.jar',
		sha1 => 'be4984cb6fd1ef1d11f218a648889dfda44b8a15'
	 },
	objenesis => {
		url => 'https://repo1.maven.org/maven2/org/objenesis/objenesis/3.3/objenesis-3.3.jar',
		fname => 'objenesis.jar',
		sha1 => '1049c09f1de4331e8193e579448d0916d75b7631'
	 },
	commons_cli => {
		url => 'https://repo1.maven.org/maven2/commons-cli/commons-cli/1.2/commons-cli-1.2.jar',
		fname => 'commons-cli.jar',
		sha1 => '2bf96b7aa8b611c177d329452af1dc933e14501c'
	},
	commons_exec => {
		url => 'https://repo1.maven.org/maven2/org/apache/commons/commons-exec/1.1/commons-exec-1.1.jar',
		fname => 'commons-exec.jar',
		sha1 => '07dfdf16fade726000564386825ed6d911a44ba1'
	},
	javassist => {
		url => 'https://repo1.maven.org/maven2/org/javassist/javassist/3.20.0-GA/javassist-3.20.0-GA.jar',
		fname => 'javassist.jar',
		sha1 => 'a9cbcdfb7e9f86fbc74d3afae65f2248bfbf82a0'
	},
	junit4 => {
		url => 'https://repo1.maven.org/maven2/junit/junit/4.10/junit-4.10.jar',
		fname => 'junit4.jar',
		sha1 => 'e4f1766ce7404a08f45d859fb9c226fc9e41a861'
	},
	mockito_core => {
		url => 'https://repo1.maven.org/maven2/org/mockito/mockito-core/5.11.0/mockito-core-5.11.0.jar',
		fname => 'mockito-core.jar',
		sha1 => 'e4069fa4f4ff2c94322cfec5f2e45341c6c70aff'
	},
	testng => {
		url => 'https://repo1.maven.org/maven2/org/testng/testng/6.14.2/testng-6.14.2.jar',
		fname => 'testng.jar',
		sha1 => '10c93c2c0d165e895a7582dfd8b165f108658db5'
	},
	jcommander => {
		url => 'https://repo1.maven.org/maven2/com/beust/jcommander/1.48/jcommander-1.48.jar',
		fname => 'jcommander.jar',
		sha1 => 'bfcb96281ea3b59d626704f74bc6d625ff51cbce'
	},
	asmtools => {
		url => 'https://ci.adoptium.net/view/Dependencies/job/dependency_pipeline/lastSuccessfulBuild/artifact/asmtools/asmtools-core-7.0.b10-ea.jar',
		fname => 'asmtools.jar',
		shaurl => 'https://ci.adoptium.net/view/Dependencies/job/dependency_pipeline/lastSuccessfulBuild/artifact/asmtools/asmtools-core-7.0.b10-ea.jar.sha256sum.txt',
		shafn => 'asmtools.jar.sha256sum.txt',
		shaalg => '256'
	},
	jaxb_api => {
		url => 'https://repo1.maven.org/maven2/javax/xml/bind/jaxb-api/2.3.0/jaxb-api-2.3.0.jar',
		fname => 'jaxb-api.jar',
		sha1 => '99f802e0cb3e953ba3d6e698795c4aeb98d37c48'
	},
	json_simple => {
		url => 'https://repo1.maven.org/maven2/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar',
		fname => 'json-simple.jar',
		sha1 => 'c9ad4a0850ab676c5c64461a05ca524cdfff59f1'
	},
	tohandler_simple => {
		url => 'https://openj9-jenkins.osuosl.org/job/Build_JDK_Timeout_Handler/lastSuccessfulBuild/artifact/openj9jtregtimeouthandler.jar',
		fname => 'openj9jtregtimeouthandler.jar',
		shaurl => 'https://openj9-jenkins.osuosl.org/job/Build_JDK_Timeout_Handler/lastSuccessfulBuild/artifact/openj9jtregtimeouthandler.jar.sha256sum.txt',
		shafn => 'openj9jtregtimeouthandler.jar.sha256sum.txt',
		shaalg => '256'
	},
	osgi => {
		url => 'https://repo1.maven.org/maven2/org/eclipse/platform/org.eclipse.osgi/3.16.100/org.eclipse.osgi-3.16.100.jar',
		fname => 'org.eclipse.osgi-3.16.100.jar',
		sha1 => '7ddb312f386b799d6e004d193a01c50169bf69f3'
	},
	jtreg_5_1_b01 => {
		url => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg5.1-b01.tar.gz',
		fname => 'jtreg_5_1_b01.tar.gz',
		shaurl => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg5.1-b01.tar.gz.sha256sum.txt',
		shafn => 'jtreg_5_1_b01.tar.gz.sha256sum.txt',
		shaalg => '256'
	},
	jtreg_7_3_1_1 => {
		url => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-7.3.1+1.tar.gz',
		fname => 'jtreg_7_3_1_1.tar.gz',
		shaurl => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-7.3.1+1.tar.gz.sha256sum.txt',
		shafn => 'jtreg_7_3_1_1.tar.gz.sha256sum.txt',
		shaalg => '256'
	},
	jython => {
		url => 'https://repo1.maven.org/maven2/org/python/jython-standalone/2.7.2/jython-standalone-2.7.2.jar',
		fname => 'jython-standalone.jar',
		sha1 => '15592c29538abd36d15570eda9fa055ed1a618ba'
	},
	jcstress => {
		url => 'https://builds.shipilev.net/jcstress/jcstress-tests-all-20240222.jar',
		fname => 'jcstress-tests-all-20240222.jar',
		sha1 => '200da75e67689e8a604ec6fe9a6f55b2c000b6ce'
	},
	hamcrest_core => {
		url => 'https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar',
		fname => 'hamcrest-core.jar',
		sha1 => '42a25dc3219429f0e5d060061f71acb49bf010a0'
	},
	bcprov_jdk18on => {
		url => 'https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/1.78.1/bcprov-jdk18on-1.78.1.jar',
		fname => 'bcprov-jdk18on.jar',
		sha1 => '39e9e45359e20998eb79c1828751f94a818d25f8'
	},
	junit_vintage_engine => {
		url => 'https://repo1.maven.org/maven2/org/junit/vintage/junit-vintage-engine/5.10.2/junit-vintage-engine-5.10.2.jar',
		fname => 'junit-vintage-engine.jar',
		sha1 => '2905387f99f86a6618d1f7c005e7a5946224f317'
	},
	junit_platform_suite => {
		url => 'https://repo1.maven.org/maven2/org/junit/platform/junit-platform-suite/1.10.1/junit-platform-suite-1.10.1.jar',
		fname => 'junit-platform-suite.jar',
		sha1 => 'a219dbd79ec2b1fc61b806554fcf4eb5c17a6d1d'
	},
	junit_jupiter_api => {
		url => 'https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.2/junit-jupiter-api-5.10.2.jar',
		fname => 'junit-jupiter-api.jar',
		sha1 => 'fb55d6e2bce173f35fd28422e7975539621055ef'
	},
	junit_jupiter_engine => {
		url => 'https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.10.2/junit-jupiter-engine-5.10.2.jar',
		fname => 'junit-jupiter-engine.jar',
		sha1 => 'f1f8fe97bd58e85569205f071274d459c2c4f8cd'
	},
	junit_jupiter_params => {
		url => 'https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-params/5.10.2/junit-jupiter-params-5.10.2.jar',
		fname => 'junit-jupiter-params.jar',
		sha1 => '359132c82a9d3fa87a325db6edd33b5fdc67a3d8'
	},
	junit_platform_suite_api => {
		url => 'https://repo1.maven.org/maven2/org/junit/platform/junit-platform-suite-api/1.10.2/junit-platform-suite-api-1.10.2.jar',
		fname => 'junit-platform-suite-api.jar',
		sha1 => '174bba1574c37352b0eb2c06e02b6403738ad57c'
	});

my @dependencies = split(',', $dependencyList);

# Put all dependent jars hash to array to prepare downloading
my @jars_info;
foreach my $dependency (keys %base) {
	foreach my $i (@dependencies) {
		if ($i eq "all" || $dependency eq $i) {
			push(@jars_info, $base{$dependency});
		}
	}
}

print "--------------------------------------------\n";
print "Starting download third party dependent jars\n";
print "--------------------------------------------\n";

if ($task eq "clean") {
	my $output = `rm $path/*.jar`;
	print "cleaning jar task completed.\n"
} elsif ($task eq "default") {
	print "downloading dependent third party jars to $path\n";
	for my $i (0 .. $#jars_info) {
		my $url = $jars_info[$i]{url};
		my $fn = $jars_info[$i]{fname};
		my $filename = $path . $sep . $fn;
		my $shaurl = $jars_info[$i]{shaurl};
		my $shafn = $jars_info[$i]{shafn};

		# if customUrl is provided, use customUrl and reset $url and $shaurl
		if ($customUrl ne "") {
			$url = "$customUrl/$fn";
			if (defined $shaurl && $shaurl ne '') {
				$shaurl = "$customUrl/$shafn";
			}
		}

		my $shaalg = $jars_info[$i]{shaalg};
		if (!$shaalg) {
			$shaalg = "sha1";
		}
		my $sha = Digest::SHA->new($shaalg);
		my $digest = "";

		if (-e $filename) {
			$sha->addfile($filename);
			$digest = $sha->hexdigest;
		}

		my $expectedsha = $jars_info[$i]{sha1};
		if (!$expectedsha) {
			$shafn = $path . $sep . $shafn;
			# if the sha file exists, parse the file and get the expected sha
			if (-e $shafn) {
				$expectedsha = getShaFromFile($shafn, $fn);
			}

			# if expectedsha is not set above and shaurl is provided, download the sha file
			# and parse the file to get the expected sha
			if (!$expectedsha && $shaurl) {
				downloadFile($shaurl, $shafn);
				$expectedsha = getShaFromFile($shafn, $fn);
			}
		}

		if ($expectedsha && $digest eq $expectedsha) {
			print "$filename exists with correct hash, not downloading\n";
			next;
		}

		# download the dependent third party jar
		downloadFile($url, $filename);

		# if shaurl is provided, re-download the sha file and reset the expectedsha value
		# as the dependent third party jar is newly downloadeded
		if ($shaurl) {
			downloadFile($shaurl, $shafn);
			$expectedsha = getShaFromFile($shafn, $fn);
		}

		if (!$expectedsha) {
			die "ERROR: cannot get the expected sha for file $fn.\n";
		}

		# validate dependencies sha sum
		$sha = Digest::SHA->new($shaalg);
		$sha->addfile($filename);
		$digest = $sha->hexdigest;

		if ($digest ne $expectedsha) {
			print "Expected sha is: $expectedsha,\n";
			print "Actual sha is  : $digest.\n";
			print "Please delete $filename and rerun the program!";
			die "ERROR: sha checksum error.\n";
		}
	}
	print "downloaded dependent third party jars successfully\n";
} else {
	die "ERROR: task unsatisfied!\n";
}

sub getShaFromFile {
	my ( $shafile, $fn ) = @_;
	my $sha = "";
	open my $fh, '<', $shafile or print "Can't open file $!";
	my $content = do { local $/; <$fh> };
	my @token = split / /, $content;
	if (@token > 1) {
		$sha = $token[0];
	} else {
		print "WARNING: cannot get the sha from $shafile.\nFile content: $content\n";
	}
	return $sha;
}

sub downloadFile {
	my ( $url, $filename ) = @_;
	print "downloading $url\n";
	my $output;

	# Delete existing file in case it is tagged incorrectly which curl would then honour..
	if (-e $filename) {
		qx(rm $filename);
	}

	# .txt SHA files are in ISO8859-1
	# note _ENCODE_FILE_NEW flag is set for zos
	if ('.txt' eq substr $filename, -length('.txt')) {
		$output = qx{_ENCODE_FILE_NEW=ISO8859-1 curl $curlOpts -k -o $filename $url 2>&1};
	} else {
		$output = qx{_ENCODE_FILE_NEW=UNTAGGED curl $curlOpts -k -o $filename $url 2>&1};
	}
	my $returnCode = $?;
	if ($returnCode == 0) {
		print "--> file downloaded to $filename\n";
	} else {
		if ($output) {
			print $output;
		}
		if (-e $filename) {
			unlink $filename or die "Can't delete '$filename': $!\n";
		}
		die "ERROR: downloading $url failed, return code: $returnCode\n";
	}
}
