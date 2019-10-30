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

use Cwd;

#Configure makefile dir
my $configuredir="";
if (@ARGV) {
	$configuredir = $ARGV[0];
} else {
	$configuredir = cwd();
}
my $machineConfiguremk = $configuredir . "/machineConfigure.mk";
unlink($machineConfiguremk);
my $platform=`uname`;

my $ZOSR14=3906;
my %capability_Hash;

if ($platform =~ /OS\/390/i) {

	my $hardware=`uname -m`;
	if ($hardware >= $ZOSR14) {
		$capability_Hash{guardedstorage} = 1;
	}
}

elsif ($platform =~ /Linux/i) {
	# virtual infor
	my $KVM_Image=`cat /proc/cpuinfo | grep -i QEMU`;
	my $VMWare_Image=`lspci 2>/dev/null | grep -i VMware` ;
	my $Virt_Sys="";

	if ($KVM_Image ne "" ) {
		 $Virt_Sys="KVM"
	} elsif ($VMWare_Image ne "") {
		$Virt_Sys="VMWare";
	}
	$ENV{hypervisor} = $Virt_Sys;
	$capability_Hash{hypervisor} = $Virt_Sys;
}
else { 
	exit;
	}

if (%capability_Hash) {
	open( my $fhout,  '>>',  $machineConfiguremk )  or die "Cannot create file $machineConfiguremk $!";
	while (my ($key, $value) = each %capability_Hash) {
		my $exportline = "export " . $key . "=" . $value. "\n";
		print $fhout $exportline;
		delete $capability_Hash{$key};
	}
	close $fhout;
}
