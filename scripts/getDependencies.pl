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
my $testDependencyUrl = "";
my $curlOpts = "";

GetOptions ("path=s" => \$path,
			"task=s" => \$task,
			"dependencyList=s" => \$dependencyList,
			"testDependencyUrl=s" => \$testDependencyUrl,
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
$testDependencyUrl = $ENV{'TEST_DEPENDENCY_URL'} if ($testDependencyUrl eq '' && defined($ENV{'TEST_DEPENDENCY_URL'}));

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
		url => 'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.0-beta/asm-9.0-beta.jar',
		fname => 'asm.jar',
		sha1 => 'a0f58cad836a410f6ba133aaa209aea7e54aaf8a'
	},
	byte_buddy => {
		url => 'https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.17.4/byte-buddy-1.17.4.jar',
		fname => 'byte-buddy.jar',
		sha1 => 'ffb8488d93290eff074fb542a596e4c5a26d0315'
	},
	byte_buddy_agent => {
		url => 'https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy-agent/1.15.4/byte-buddy-agent-1.15.4.jar',
		fname => 'byte-buddy-agent.jar',
		sha1 => '58e850dde88f3cf20f41f659440bef33f6c4fe02'
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
		url => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/asmtools/asmtools-core-7.0.b10-ea.jar',
		fname => 'asmtools.jar',
		shaurl => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/asmtools/asmtools-core-7.0.b10-ea.jar.sha256sum.txt',
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
	jtreg_7_4_1 => {
		url => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-7.4+1.tar.gz',
		fname => 'jtreg_7_4_1.tar.gz',
		shaurl => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-7.4+1.tar.gz.sha256sum.txt',
		shafn => 'jtreg_7_4_1.tar.gz.sha256sum.txt',
		shaalg => '256'
	},
	jtreg_7_5_2_1 => {
		url => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-7.5.2+1.tar.gz',
		fname => 'jtreg_7_5_2_1.tar.gz',
		shaurl => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-7.5.2+1.tar.gz.sha256sum.txt',
		shafn => 'jtreg_7_5_2_1.tar.gz.sha256sum.txt',
		shaalg => '256'
	},
	jtreg_8_2 => {
		url => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-8+2.tar.gz',
		fname => 'jtreg_8_2.tar.gz',
		shaurl => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-8+2.tar.gz.sha256sum.txt',
		shafn => 'jtreg_8_2.tar.gz.sha256sum.txt',
		shaalg => '256'
	},
	jtreg_8_1_1 => {
		url => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-8.1+1.tar.gz',
		fname => 'jtreg_8_1_1.tar.gz',
		shaurl => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-8.1+1.tar.gz.sha256sum.txt',
		shafn => 'jtreg_8_1_1.tar.gz.sha256sum.txt',
		shaalg => '256'
	},
	jtreg_8_2_1_1 => {
		url => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-8.2.1+1.tar.gz',
		fname => 'jtreg_8_2_1_1.tar.gz',
		shaurl => 'https://ci.adoptium.net/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/jtreg-8.2.1+1.tar.gz.sha256sum.txt',
		shafn => 'jtreg_8_2_1_1.tar.gz.sha256sum.txt',
		shaalg => '256'
	},
	jython => {
		url => 'https://repo1.maven.org/maven2/org/python/jython-standalone/2.7.2/jython-standalone-2.7.2.jar',
		fname => 'jython-standalone.jar',
		sha1 => '15592c29538abd36d15570eda9fa055ed1a618ba'
	},
	liberty_runtime => {
		 url => 'https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/24.0.0.1/openliberty-24.0.0.1.zip',
		fname => 'liberty_runtime.zip',
		sha1 => 'c59edb53e8e88e674b67f0941e1abfc30eb0cd49'
 	},
	jcstress => {
		url => 'https://builds.shipilev.net/jcstress/jcstress-tests-all-20240222.jar',
		fname => 'jcstress-tests-all-20240222.jar',
		sha1 => '200da75e67689e8a604ec6fe9a6f55b2c000b6ce'
	},
	maven => {
		url => 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.10/apache-maven-3.9.10-bin.tar.gz',
		fname => 'apache-maven-bin.tar.gz',
		sha1 => 'b06edaf09ae03e77d65a9fa6ffc2f6db311b8334'
	},
	dacapo => {
		url => '-L https://download.dacapobench.org/chopin/dacapo-23.11-MR2-chopin-minimal.zip',
		fname => 'dacapo.zip',
		sha1 => '2dd4704900e43f4e22c70255b555c02177ef07c4'
	},
	icu4j_61_1 => {
		url => 'https://repo1.maven.org/maven2/com/ibm/icu/icu4j/61.1/icu4j-61.1.jar',
		fname => 'icu4j-61_1.jar',
		sha1 => '55c98eb1838b2a4bb9a07dc36bd378532d64d0cdcb7ceee914236866a7de4464',
		shaalg => '256'
	},
	icu4j_localespi_61_1 => {
		url => 'https://repo1.maven.org/maven2/com/ibm/icu/icu4j-localespi/61.1/icu4j-localespi-61.1.jar',
		fname => 'icu4j-localespi-61_1.jar',
		sha1 => '99691e7562bcb211fc247ea27d4abf94486c5f373907da71a17bb358d97829b1',
		shaalg => '256'
	},
	unicode_gb18030_2022 => {
		url => 'https://www.unicode.org/Public/mappings/iso10646/GB18030-2022.txt',
		fname => 'GB18030-2022.txt',
		sha1 => '80c3fe2ae1062abf56456f52518bd670f9ec3917b7f85e152b347ac6b6faf880',
		shaalg => '256'
	},
	unicode_unihan_17_0_0 => {
		url => 'https://www.unicode.org/Public/17.0.0/ucd/Unihan.zip',
		fname => 'Unihan-17.0.0.zip',
		sha1 => 'f7a48b2b545acfaa77b2d607ae28747404ce02baefee16396c5d2d7a8ef34b5e',
		shaalg => '256'
	},
	unicode_unihan_16_0_0 => {
		url => 'https://www.unicode.org/Public/zipped/16.0.0/Unihan.zip',
		fname => 'Unihan-16.0.0.zip',
		sha1 => 'b8f000df69de7828d21326a2ffea462b04bc7560022989f7cc704f10521ef3e0',
		shaalg => '256'
	},
	unicode_unihan_15_1_0 => {
		url => 'https://www.unicode.org/Public/zipped/15.1.0/Unihan.zip',
		fname => 'Unihan-15.1.0.zip',
		sha1 => 'a0226610e324bcf784ac380e11f4cbf533ee1e6b3d028b0991bf8c0dc3f85853',
		shaalg => '256'
	},
	unicode_unihan_15_0_0 => {
		url => 'https://www.unicode.org/Public/zipped/15.0.0/Unihan.zip',
		fname => 'Unihan-15.0.0.zip',
		sha1 => '24b154691fc97cb44267b925d62064297086b3f896b57a8181c7b6d42702a026',
		shaalg => '256'
	},
	unicode_unihan_14_0_0 => {
		url => 'https://www.unicode.org/Public/zipped/14.0.0/Unihan.zip',
		fname => 'Unihan-14.0.0.zip',
		sha1 => '2ae4519b2b82cd4d15379c17e57bfb12c33c0f54da4977de03b2b04bcf11852d',
		shaalg => '256'
	},
	unicode_unihan_13_0_0 => {
		url => 'https://www.unicode.org/Public/zipped/13.0.0/Unihan.zip',
		fname => 'Unihan-13.0.0.zip',
		sha1 => 'e380194c4835ad85aa50e8750a58c1f605dbfc4aba9e3e3b0ca25b9530c02f64',
		shaalg => '256'
	},
	unicode_unihan_12_1_0 => {
		url => 'https://www.unicode.org/Public/zipped/12.1.0/Unihan.zip',
		fname => 'Unihan-12.1.0.zip',
		sha1 => '6e4553f3b5fffe0d312df324d020ef1278d9595932ae03f4e8a2d427de83cdcd',
		shaalg => '256'
	},
	unicode_unihan_12_0_0 => {
		url => 'https://www.unicode.org/Public/zipped/12.0.0/Unihan.zip',
		fname => 'Unihan-12.0.0.zip',
		sha1 => '6e4553f3b5fffe0d312df324d020ef1278d9595932ae03f4e8a2d427de83cdcd',
		shaalg => '256'
	},
	unicode_unihan_11_0_0 => {
		url => 'https://www.unicode.org/Public/zipped/11.0.0/Unihan.zip',
		fname => 'Unihan-11.0.0.zip',
		sha1 => '72079d9d64acd452e1496f67b975a82a989f0023f37aed092c51cf13567fc833',
		shaalg => '256'
	},
	unicode_unihan_10_0_0 => {
		url => 'https://www.unicode.org/Public/zipped/10.0.0/Unihan.zip',
		fname => 'Unihan-10.0.0.zip',
		sha1 => '01232063a8529636cf155ba7a1dbad329cb2e63acde83a3d607b5eafa8f933a5',
		shaalg => '256'
	},
	unicode_ucd_unicodedata_17_0_0 => {
		url => 'https://www.unicode.org/Public/17.0.0/ucd/UnicodeData.txt',
		fname => 'UnicodeData-17.0.0.txt',
		sha1 => '2e1efc1dcb59c575eedf5ccae60f95229f706ee6d031835247d843c11d96470c',
		shaalg => '256'
	},
	unicode_ucd_blocks_17_0_0 => {
		url => 'https://www.unicode.org/Public/17.0.0/ucd/Blocks.txt',
		fname => 'Blocks-17.0.0.txt',
		sha1 => 'c0edefaf1a19771e830a82735472716af6bf3c3975f6c2a23ffbe2580fbbcb15',
		shaalg => '256'
	},
	unicode_ucd_scripts_17_0_0 => {
		url => 'https://www.unicode.org/Public/17.0.0/ucd/Scripts.txt',
		fname => 'Scripts-17.0.0.txt',
		sha1 => '9f5e50d3abaee7d6ce09480f325c706f485ae3240912527e651954d2d6b035bf',
		shaalg => '256'
	},
	unicode_ucd_normalization_17_0_0 => {
		url => 'https://www.unicode.org/Public/17.0.0/ucd/NormalizationTest.txt',
		fname => 'NormalizationTest-17.0.0.txt',
		sha1 => '5019ffd530751a741900c849c0e010332f142a3612234639bd200b82138a87db',
		shaalg => '256'
	},
	unicode_ucd_propvalaliases_17_0_0 => {
		url => 'https://www.unicode.org/Public/17.0.0/ucd/PropertyValueAliases.txt',
		fname => 'PropertyValueAliases-17.0.0.txt',
		sha1 => '64e9a5f76f7a1e8b5a47d6a1f9a26522a251208f5276bdfa1559dac7cf2e827a',
		shaalg => '256'
	},
	unicode_ucd_unicodedata_16_0_0 => {
		url => 'https://www.unicode.org/Public/16.0.0/ucd/UnicodeData.txt',
		fname => 'UnicodeData-16.0.0.txt',
		sha1 => 'ff58e5823bd095166564a006e47d111130813dcf8bf234ef79fa51a870edb48f',
		shaalg => '256'
	},
	unicode_ucd_blocks_16_0_0 => {
		url => 'https://www.unicode.org/Public/16.0.0/ucd/Blocks.txt',
		fname => 'Blocks-16.0.0.txt',
		sha1 => 'f3907b395d410f1b97342292ca6bc83dd12eb4b205f2a0c48efdef99e517d7b0',
		shaalg => '256'
	},
	unicode_ucd_scripts_16_0_0 => {
		url => 'https://www.unicode.org/Public/16.0.0/ucd/Scripts.txt',
		fname => 'Scripts-16.0.0.txt',
		sha1 => '9e88f0a677df47311106340be8ede2ecdacd9c1c931831218d2be6d5508e0039',
		shaalg => '256'
	},
	unicode_ucd_normalization_16_0_0 => {
		url => 'https://www.unicode.org/Public/16.0.0/ucd/NormalizationTest.txt',
		fname => 'NormalizationTest-16.0.0.txt',
		sha1 => 'd811971453e7075e1ad56fb1b301eece5aa80757b81f6156e74a1bfb3ae5ceb1',
		shaalg => '256'
	},
	unicode_ucd_propvalaliases_16_0_0 => {
		url => 'https://www.unicode.org/Public/16.0.0/ucd/PropertyValueAliases.txt',
		fname => 'PropertyValueAliases-16.0.0.txt',
		sha1 => '440fd3e5460b9bfe31da67b6f923992e1989d31fe2ed91e091c4b8f8e2620bf9',
		shaalg => '256'
	},
	unicode_ucd_unicodedata_15_1_0 => {
		url => 'https://www.unicode.org/Public/15.1.0/ucd/UnicodeData.txt',
		fname => 'UnicodeData-15.1.0.txt',
		sha1 => '2fc713e6a31a87c4850a37fe2caffa4218180fadb5de86b43a143ddb4581fb86',
		shaalg => '256'
	},
	unicode_ucd_blocks_15_1_0 => {
		url => 'https://www.unicode.org/Public/15.1.0/ucd/Blocks.txt',
		fname => 'Blocks-15.1.0.txt',
		sha1 => '443ee0524a775bf021777c296f5b591b5611c8aef6bc922887d27b0bc13892b5',
		shaalg => '256'
	},
	unicode_ucd_scripts_15_1_0 => {
		url => 'https://www.unicode.org/Public/15.1.0/ucd/Scripts.txt',
		fname => 'Scripts-15.1.0.txt',
		sha1 => '0eacb65169ae6eb1d399cd70826b3da15fff19f6f586eecf819b70c83b1d9b32',
		shaalg => '256'
	},
	unicode_ucd_normalization_15_1_0 => {
		url => 'https://www.unicode.org/Public/15.1.0/ucd/NormalizationTest.txt',
		fname => 'NormalizationTest-15.1.0.txt',
		sha1 => '871238e37e3be0696ec2bd0891119a041b052da1a84485eda05a5438724b223e',
		shaalg => '256'
	},
	unicode_ucd_propvalaliases_15_1_0 => {
		url => 'https://www.unicode.org/Public/15.1.0/ucd/PropertyValueAliases.txt',
		fname => 'PropertyValueAliases-15.1.0.txt',
		sha1 => '4b7411fc592c4985e5f03643aa0bddfdfd45250ff1790d358926614d20e37652',
		shaalg => '256'
	},
	unicode_ucd_unicodedata_15_0_0 => {
		url => 'https://www.unicode.org/Public/15.0.0/ucd/UnicodeData.txt',
		fname => 'UnicodeData-15.0.0.txt',
		sha1 => '806e9aed65037197f1ec85e12be6e8cd870fc5608b4de0fffd990f689f376a73',
		shaalg => '256'
	},
	unicode_ucd_blocks_15_0_0 => {
		url => 'https://www.unicode.org/Public/15.0.0/ucd/Blocks.txt',
		fname => 'Blocks-15.0.0.txt',
		sha1 => '529dc5d0f6386d52f2f56e004bbfab48ce2d587eea9d38ba546c4052491bd820',
		shaalg => '256'
	},
	unicode_ucd_scripts_15_0_0 => {
		url => 'https://www.unicode.org/Public/15.0.0/ucd/Scripts.txt',
		fname => 'Scripts-15.0.0.txt',
		sha1 => 'cca85d830f46aece2e7c1459ef1249993dca8f2e46d51e869255be140d7ea4b0',
		shaalg => '256'
	},
	unicode_ucd_normalization_15_0_0 => {
		url => 'https://www.unicode.org/Public/15.0.0/ucd/NormalizationTest.txt',
		fname => 'NormalizationTest-15.0.0.txt',
		sha1 => 'fb9ac8cc154a80cad6caac9897af55a4e75176af6f4e2bb6edc2bf8b1d57f326',
		shaalg => '256'
	},
	unicode_ucd_propvalaliases_15_0_0 => {
		url => 'https://www.unicode.org/Public/15.0.0/ucd/PropertyValueAliases.txt',
		fname => 'PropertyValueAliases-15.0.0.txt',
		sha1 => '13a7666843abea5c6b7eb8c057c57ab9bb2ba96cfc936e204224dd67d71cafad',
		shaalg => '256'
	},
	unicode_ucd_unicodedata_14_0_0 => {
		url => 'https://www.unicode.org/Public/14.0.0/ucd/UnicodeData.txt',
		fname => 'UnicodeData-14.0.0.txt',
		sha1 => '36018e68657fdcb3485f636630ffe8c8532e01c977703d2803f5b89d6c5feafb',
		shaalg => '256'
	},
	unicode_ucd_blocks_14_0_0 => {
		url => 'https://www.unicode.org/Public/14.0.0/ucd/Blocks.txt',
		fname => 'Blocks-14.0.0.txt',
		sha1 => '598870dddef7b34b5a972916528c456aff2765b79cd4f9647fb58ceb767e7f17',
		shaalg => '256'
	},
	unicode_ucd_scripts_14_0_0 => {
		url => 'https://www.unicode.org/Public/14.0.0/ucd/Scripts.txt',
		fname => 'Scripts-14.0.0.txt',
		sha1 => '52db475c4ec445e73b0b16915448c357614946ad7062843c563e00d7535c6510',
		shaalg => '256'
	},
	unicode_ucd_normalization_14_0_0 => {
		url => 'https://www.unicode.org/Public/14.0.0/ucd/NormalizationTest.txt',
		fname => 'NormalizationTest-14.0.0.txt',
		sha1 => '7cb30cc2abe6c29c292b99095865d379ce1213045c78c4ff59c7e9391bbe2331',
		shaalg => '256'
	},
	unicode_ucd_propvalaliases_14_0_0 => {
		url => 'https://www.unicode.org/Public/14.0.0/ucd/PropertyValueAliases.txt',
		fname => 'PropertyValueAliases-14.0.0.txt',
		sha1 => 'eb755757e20b72b330b2948df3cf2ff7adb0e31bb060140dc09dafb132ace2cd',
		shaalg => '256'
	},
	unicode_ucd_unicodedata_13_0_0 => {
		url => 'https://www.unicode.org/Public/13.0.0/ucd/UnicodeData.txt',
		fname => 'UnicodeData-13.0.0.txt',
		sha1 => 'bdbffbbfc8ad4d3a6d01b5891510458f3d36f7170422af4ea2bed3211a73e8bb',
		shaalg => '256'
	},
	unicode_ucd_blocks_13_0_0 => {
		url => 'https://www.unicode.org/Public/13.0.0/ucd/Blocks.txt',
		fname => 'Blocks-13.0.0.txt',
		sha1 => '81a82b6a9fcf1a9c12f588d7a1decd73a9afdc4cac95b0eb7e576e7942d6c19f',
		shaalg => '256'
	},
	unicode_ucd_scripts_13_0_0 => {
		url => 'https://www.unicode.org/Public/13.0.0/ucd/Scripts.txt',
		fname => 'Scripts-13.0.0.txt',
		sha1 => '9a5ed1ec9b5f0d7147e9371ad792ab39203611af7637cff2aa4a5c663b172cde',
		shaalg => '256'
	},
	unicode_ucd_normalization_13_0_0 => {
		url => 'https://www.unicode.org/Public/13.0.0/ucd/NormalizationTest.txt',
		fname => 'NormalizationTest-13.0.0.txt',
		sha1 => 'd60ee55dd9169444652e48d337109cc814ecc59a9d3122eedddf7de388f2e01d',
		shaalg => '256'
	},
	unicode_ucd_propvalaliases_13_0_0 => {
		url => 'https://www.unicode.org/Public/13.0.0/ucd/PropertyValueAliases.txt',
		fname => 'PropertyValueAliases-13.0.0.txt',
		sha1 => '6b3902e9268cd843fe65cbdea992108c9528343ec0679f800b96f356bb553e5a',
		shaalg => '256'
	},
	unicode_ucd_unicodedata_12_1_0 => {
		url => 'https://www.unicode.org/Public/12.1.0/ucd/UnicodeData.txt',
		fname => 'UnicodeData-12.1.0.txt',
		sha1 => '93ab1acd8fd9d450463b50ae77eab151a7cda48f98b25b56baed8070f80fc936',
		shaalg => '256'
	},
	unicode_ucd_blocks_12_1_0 => {
		url => 'https://www.unicode.org/Public/12.1.0/ucd/Blocks.txt',
		fname => 'Blocks-12.1.0.txt',
		sha1 => 'a28b205afe8625fffdb6544a5fe14cf02b91493d9900f07820fa2102a17548f7',
		shaalg => '256'
	},
	unicode_ucd_scripts_12_1_0 => {
		url => 'https://www.unicode.org/Public/12.1.0/ucd/Scripts.txt',
		fname => 'Scripts-12.1.0.txt',
		sha1 => 'e6313a8edfd24f36c7a006fbcf1d1b7245b5dd009c6dde80441f0da08b822c43',
		shaalg => '256'
	},
	unicode_ucd_normalization_12_1_0 => {
		url => 'https://www.unicode.org/Public/12.1.0/ucd/NormalizationTest.txt',
		fname => 'NormalizationTest-12.1.0.txt',
		sha1 => '8cabbd6293c88ca05f0b601ade0fd16978ac670a077c0e0f419986ddd33c6941',
		shaalg => '256'
	},
	unicode_ucd_propvalaliases_12_1_0 => {
		url => 'https://www.unicode.org/Public/12.1.0/ucd/PropertyValueAliases.txt',
		fname => 'PropertyValueAliases-12.1.0.txt',
		sha1 => 'efce54f7c715a332c19b3d14c6a0eea30c6cde91caf6ff0d21c755be933736f4',
		shaalg => '256'
	},
	unicode_ucd_unicodedata_11_0_0 => {
		url => 'https://www.unicode.org/Public/11.0.0/ucd/UnicodeData.txt',
		fname => 'UnicodeData-11.0.0.txt',
		sha1 => '4997a3196eb79b4d0d6b8384560f6aeb46a062693f0abd5ba736abbff7976099',
		shaalg => '256'
	},
	unicode_ucd_blocks_11_0_0 => {
		url => 'https://www.unicode.org/Public/11.0.0/ucd/Blocks.txt',
		fname => 'Blocks-11.0.0.txt',
		sha1 => '0b457b66c788a97c8521e265f0b793d4ed911356d39eb61029f9cef8554cd052',
		shaalg => '256'
	},
	unicode_ucd_scripts_11_0_0 => {
		url => 'https://www.unicode.org/Public/11.0.0/ucd/Scripts.txt',
		fname => 'Scripts-11.0.0.txt',
		sha1 => 'e9f3c0aa3c4f892b589c809cf4ae051a39921417cda6fefdbe43717b92db76d5',
		shaalg => '256'
	},
	unicode_ucd_normalization_11_0_0 => {
		url => 'https://www.unicode.org/Public/11.0.0/ucd/NormalizationTest.txt',
		fname => 'NormalizationTest-11.0.0.txt',
		sha1 => '0fdfc17093dd5482f8089cb11dcd936abdba34c4c9c324e5b8a4e5d8f943f6d3',
		shaalg => '256'
	},
	unicode_ucd_propvalaliases_11_0_0 => {
		url => 'https://www.unicode.org/Public/11.0.0/ucd/PropertyValueAliases.txt',
		fname => 'PropertyValueAliases-11.0.0.txt',
		sha1 => '2a9cb9afe6a36a1a73ce2cedb540abd3fbf29f6afcda702d07fcbf561162a17a',
		shaalg => '256'
	},
	unicode_ucd_unicodedata_10_0_0 => {
		url => 'https://www.unicode.org/Public/10.0.0/ucd/UnicodeData.txt',
		fname => 'UnicodeData-10.0.0.txt',
		sha1 => '52423e4d7492167b62f518f68d54db88930abbbff7f11edfcaec8f726498cab1',
		shaalg => '256'
	},
	unicode_ucd_blocks_10_0_0 => {
		url => 'https://www.unicode.org/Public/10.0.0/ucd/Blocks.txt',
		fname => 'Blocks-10.0.0.txt',
		sha1 => '5ae1649a42ed8ae8cb885af79563f00a9ae17e602405a56ed8aca214da14eea7',
		shaalg => '256'
	},
	unicode_ucd_scripts_10_0_0 => {
		url => 'https://www.unicode.org/Public/10.0.0/ucd/Scripts.txt',
		fname => 'Scripts-10.0.0.txt',
		sha1 => 'd02e24e4c516e9090b6bc9c2d2c8f4c89510b6ed8c5e859d0a861b0dc5cf372d',
		shaalg => '256'
	},
	unicode_ucd_normalization_10_0_0 => {
		url => 'https://www.unicode.org/Public/10.0.0/ucd/NormalizationTest.txt',
		fname => 'NormalizationTest-10.0.0.txt',
		sha1 => '05307ec5bcbd3af3f1687b4da5443658bf72644a5cd4106c20d12b0a3a0818c9',
		shaalg => '256'
	},
	unicode_ucd_propvalaliases_10_0_0 => {
		url => 'https://www.unicode.org/Public/10.0.0/ucd/PropertyValueAliases.txt',
		fname => 'PropertyValueAliases-10.0.0.txt',
		sha1 => 'a6b0467c3cc7aa4e57d4e5cc7f6e9562b79cf4426dfe438517c28b368ed3e673',
		shaalg => '256'
	});

	

my %system_jars = (
	json_simple => {
		url => 'https://repo1.maven.org/maven2/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar',
		fname => 'json-simple.jar',
		sha1 => 'c9ad4a0850ab676c5c64461a05ca524cdfff59f1',
	},
	jcstress => {
		url => 'https://builds.shipilev.net/jcstress/jcstress-tests-all-20240222.jar',
		fname => 'jcstress-tests-all-20240222.jar',
		sha1 => '200da75e67689e8a604ec6fe9a6f55b2c000b6ce',
	},
	ant_launcher => {
		url => 'https://repo1.maven.org/maven2/org/apache/ant/ant-launcher/1.8.1/ant-launcher-1.8.1.jar',
		dir => 'apache-ant/lib',
		fname => 'ant-launcher.jar',
		is_system_test => 1
	},
	asm => {
		url => 'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.0/asm-9.0.jar',
		dir => 'asm',
		fname => 'asm.jar',
		sha1 => 'af582ff60bc567c42d931500c3fdc20e0141ddf9',
		is_system_test => 1
	},
	cvsclient => {
		url => 'https://repo1.maven.org/maven2/org/netbeans/lib/cvsclient/20060125/cvsclient-20060125.jar',
		dir => 'cvsclient',
		fname => 'org-netbeans-lib-cvsclient.jar',
		is_system_test => 1
	},
	hamcrest_core => {
		url => 'https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar',
		dir => 'junit',
		fname => 'hamcrest-core.jar',
		is_system_test => 1
	},
	junit => {
		url => 'https://repo1.maven.org/maven2/junit/junit/4.12/junit-4.12.jar',
		dir => 'junit',
		fname => 'junit.jar',
		is_system_test => 1
	},
	log4j_api => {
		url => 'https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.15.0/log4j-api-2.15.0.jar',
		dir => 'log4j',
		fname => 'log4j-api.jar',
		is_system_test => 1
	},
	log4j_core => {
		url => 'https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.15.0/log4j-core-2.15.0.jar',
		dir => 'log4j',
		fname => 'log4j-core.jar',
		is_system_test => 1
	},
	mauve => {
		url => 'https://github.com/adoptium/aqa-triage-data/raw/main/AQAvit/mauve.jar',
		dir => 'mauve',
		fname => 'mauve.jar',
		sha1 => 'f396c75df02c008aff745ebbff234856e0788732',
		is_system_test => 1
	},
	tools => {
		url => 'https://api.adoptium.net/v3/binary/latest/8/ga/linux/x64/jdk/hotspot/normal/adoptium',
		dir => 'tools',
		fname => 'tools.jar',
		is_system_test => 1
	});

my %jars_to_use;
if ($path =~ /system_lib/ || (exists($ENV{"BUILD_TYPE"}) && $ENV{"BUILD_TYPE"} eq "systemtest")) {
	print "System Test jars will be downloaded.\n";
	%jars_to_use = %system_jars;
} else {
	print "System Test jars will not be downloaded.\n";
	%jars_to_use = %base;
}
my @dependencies = split(',', $dependencyList);
# Put all dependent jars hash to array to prepare downloading
my @jars_info;
foreach my $dependency (keys %jars_to_use) {
	foreach my $i (@dependencies) {
		if ($i eq "all" || $dependency eq $i) {
			push(@jars_info, $jars_to_use{$dependency});
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
		my $sha1 = $jars_info[$i]{sha1};
		my $dir = $jars_info[$i]{dir} // "";
		my $full_dir_path = File::Spec->catdir($path, $dir);
		if (exists($ENV{"BUILD_TYPE"}) && $ENV{"BUILD_TYPE"} eq "systemtest") {
			$full_dir_path = File::Spec->catdir($path, "systemtest_prereqs" , $dir);
			if ($fn eq "tools.jar") {
				toolsJarDownloader("$full_dir_path", "$url");
				next;
			}
		}
		my $url_testDependency = $testDependencyUrl;
		my $third_party_url = $url;

		if (!-d $full_dir_path) {
			make_path($full_dir_path, {chmod => 0755, verbose => 1}) or die "Failed to create directory: $full_dir_path: $!";
			print "Directory created: $full_dir_path\n";
		}

		my $filename = File::Spec->catfile($full_dir_path, $fn);
		my $shaurl = $jars_info[$i]{shaurl};
		my $shafn = $jars_info[$i]{shafn};

		# if url_testDependency is provided, use url_testDependency and reset $url and $shaurl
		if ($url_testDependency ne "") {
			if (defined $jars_info[$i]{is_system_test} && $jars_info[$i]{is_system_test} == 1) {
				$url_testDependency =~ s/test.getDependency/systemtest.getDependency/;
				$url_testDependency .= "systemtest_prereqs/";
				$url_testDependency .= $jars_info[$i]{dir};
				$url_testDependency .= '/' unless $url_testDependency =~ /\/$/;
			}

			$url = "$url_testDependency/$jars_info[$i]{fname}";

			if (defined $shaurl && $shaurl ne '') {
				$shaurl = "$url_testDependency/$shafn";
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
			if (defined $shafn && $shafn ne '') {
				$shafn = $path . $sep . $shafn;
				# if the sha file exists, parse the file and get the expected sha
				if (-e $shafn) {
					$expectedsha = getShaFromFile($shafn, $fn);
				}
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

		my $ignoreChecksum = (!defined $sha1 || $sha1 eq '') && (!defined $shaurl || $shaurl eq '');
		# download the dependent third party jar

		if ($ignoreChecksum && -e $filename) {
			print "$filename exists, not downloading.\n";
			next;
		}
		my $download_success = 0;
		my $sha_verified = 0;
		my $has_testDependency_url = ($url_testDependency ne "" && $url ne $third_party_url);

		# Try download from URL (testDependency or third-party)
		eval {
			downloadFile($url, $filename);
			$download_success = 1;
		};

		# If download succeeded, verify SHA
		if ($download_success && !$ignoreChecksum) {
			my $expectedsha_check = $expectedsha;
			if (!$expectedsha_check && $shaurl) {
				eval {
					downloadFile($shaurl, $shafn);
					$expectedsha_check = getShaFromFile($shafn, $fn);
				};
			}

			if ($expectedsha_check) {
				$sha = Digest::SHA->new($shaalg);
				$sha->addfile($filename);
				$digest = $sha->hexdigest;
				if ($digest eq $expectedsha_check) {
					$sha_verified = 1;
					print "SHA verification passed for $filename\n";
				} elsif ($has_testDependency_url) {
					print "Warning: SHA mismatch for $filename from testDependency URL\n";
					print "Expected: $expectedsha_check, Got: $digest\n";
					$download_success = 0;  # Mark as failed to trigger fallback
					unlink $filename;  # Delete bad file
				}
			}
		}

		# Fall back to third-party URL if testDependency URL failed or had SHA mismatch
		if (!$download_success) {
			print "Falling back to third-party URL: $third_party_url\n";
			eval {
				downloadFile($third_party_url, $filename);
				$download_success = 1;
			};
			if (!$download_success) {
				print "Error: Failed to download $filename from third-party URL $third_party_url\n";
				exit 1;
			}
			$sha_verified = 0;  # Reset flag - must verify SHA for third-party download
		}

		# Verify SHA for third-party download or if not yet verified
		# If shaurl is provided, download the sha file to get the expected checksum
		# as the dependent third party jar may have been newly downloaded
		if (!$ignoreChecksum && !$sha_verified) {
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
		} elsif ($ignoreChecksum) {
			print "Checksum verification skipped for $filename\n";
		}
	}
	print "downloaded dependent third party jars successfully\n";
} else {
	die "ERROR: task unsatisfied!\n";
}

# The tools jar is stored within another jar (a JDK) which is accessed indirectly via an api.
# This subroutine will access the api, download the outer jar, and extract the tools.jar.
sub toolsJarDownloader {
	my ( $dir, $url ) = @_;
	print "Checksum verification skipped for systemtest_prereqs/tools/tools.jar \n";
	print "downloading $url \n";
	qx{_ENCODE_FILE_NEW=BINARY curl -LfsS --create-dirs -o "$dir/jdk8/jdk8.tar.gz" $url 2>&1};
	qx{tar --directory "$dir/jdk8" -xzf "$dir/jdk8/jdk8.tar.gz" --strip-components 1};
	qx{cp "$dir/jdk8/lib/tools.jar" "$dir"};
	qx{rm -rf "$dir/jdk8"};
	print "file downloaded to $dir/tools.jar \n";
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

	my $returnCode = 99;
	my $download_attempts = 0;
	while ($returnCode != 0 && $download_attempts < 10) {
		$download_attempts++;
		print "download attempt $download_attempts for $url\n";
		# .txt SHA files are in ISO8859-1
		# note _ENCODE_FILE_NEW flag is set for zos
		if ('.txt' eq substr $filename, -length('.txt')) {
			$output = qx{_ENCODE_FILE_NEW=ISO8859-1 curl $curlOpts -k -o $filename $url 2>&1};
		} elsif ('.jar' eq substr $filename, -length('.jar')) {
			$output = qx{_ENCODE_FILE_NEW=BINARY curl $curlOpts -k -o $filename $url 2>&1};
		} else {
			$output = qx{_ENCODE_FILE_NEW=UNTAGGED curl $curlOpts -k -o $filename $url 2>&1};
		}
		$returnCode = $?;
		last if $returnCode == 0;
	}

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
