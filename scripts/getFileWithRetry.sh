#!/usr/bin/env bash
#
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

FILE=""
COMMAND=""

parseCommandLineArgs()
{
	while [[ $# -gt 0 ]] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;
		case "$opt" in
			"--file" | "-f" )
				FILE="$1"; shift;;

			"--command" | "-c" )
				COMMAND="$1"; shift;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done
}

getFileWithRetry()
{
	set +e
	count=0
	rt_code=-1
	# when the command is not found (code 127), do not retry
	while [ "$rt_code" != 0 ] && [ "$rt_code" != 127 ] && [ "$count" -le 5 ]
	do
		if [ "$count" -gt 0 ]; then
			sleep_time=300
			echo "error code: $rt_code. Sleep $sleep_time secs, then retry $count..."
			sleep $sleep_time

			echo "check for $FILE. If found, the file will be removed."
			if [ -e "$FILE" ]; then
				echo "remove $FILE before retry..."
				rm -r $FILE
			fi
		fi
		echo "$COMMAND"
		eval "$COMMAND"
		rt_code=$?
		count=$(( $count + 1 ))
	done
	set -e
	if [ $rt_code != 0 ]; then
		exit 1
	fi
}

parseCommandLineArgs "$@"
getFileWithRetry

