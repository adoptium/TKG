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

REPO_DIR=""
OUTPUT_FILE="../testenv.properties"
REPO_NAME=""
REPO_SHA=""

usage ()
{
	echo 'This script use git command to get sha in the provided REPO_DIR HEAD and write the info into the OUTPUT_FILE'
	echo 'Usage : '
	echo '                --repo_dir: local git repo dir'
	echo '                --output_file: the file to write the sha info to. Default is to ../SHAs.txt'
	echo '                --repo_name: git repo name. It will be used as a parameter name in testenv.properties'
	echo '                --repo_sha: git repo sha. If this value is not provided, use git command to get the repo sha. Otherwise, use the provided sha.'
	echo '                USE_TESTENV_PROPERTIES: env variable. If set to false, regenerate testenv.properties to capture the repo and sha. Otherwise, do nothing. The existing testenv.properties file will be used.'
	echo '                JDK_IMPL: env variable. Only used for openjdk tests. If JDK_IMPL = openj9, append OPENJ9 in REPO_NAME for openjdk repo . i.e., JDK11_OPENJ9. Otherwise, REPO_NAME is unchanged.'
}

parseCommandLineArgs()
{
	while [[ $# -gt 0 ]] && [[ ."$1" = .-* ]] ; do
		opt="$1";
		shift;
		case "$opt" in
			"--repo_dir" | "-d" )
				REPO_DIR="$1"; shift;;

			"--output_file" | "-o" )
				OUTPUT_FILE="$1"; shift;;

			"--repo_name" | "-n" )
				REPO_NAME="$1"; shift;;

			"--repo_sha" | "-s" )
				REPO_SHA="$1"; shift;;

			"--help" | "-h" )
				usage; exit 0;;

			*) echo >&2 "Invalid option: ${opt}"; echo "This option was unrecognized."; usage; exit 1;
		esac
	done
	if [ -z "$REPO_DIR" ] || [ -z "$OUTPUT_FILE" ] || [ ! -d "$REPO_DIR" ]; then
		echo "Error, please see the usage and also check if $REPO_DIR is existing"
		usage
		exit 1
	fi
}

getTestenvProperties()
{
	if [[ "$USE_TESTENV_PROPERTIES" != true  ]]; then
		echo "Check sha in $REPO_DIR and store the info in $OUTPUT_FILE"
		if [ ! -e ${OUTPUT_FILE} ]; then
			echo "touch $OUTPUT_FILE"
			touch $OUTPUT_FILE
		fi

		cd $REPO_DIR

		# If the SHA was not passed in, get it from
		# the repository directly
		if [ -z "$REPO_SHA"]; then
			REPO_SHA="$(git rev-parse HEAD)"
		fi

		# For openjdk repo, append OPENJ9 in REPO_NAME if JDK_IMPL = openj9. i.e., JDK11_OPENJ9
		# Otherwise, REPO_NAME is unchanged. i.e., JDK11
		if [ $REPO_DIR == "openjdk-jdk" ] && [ $JDK_IMPL == "openj9" ]; then
			REPO_NAME="${REPO_NAME}_OPENJ9"
		fi

		branch="${REPO_NAME}_BRANCH"
		# for openj9, we need to use OPENJ9_SHA for now, not OPENJ9_BRANCH
		if [ $REPO_NAME == "OPENJ9" ]; then
			branch="${REPO_NAME}_SHA"
		fi

		# append the info into $OUTPUT_FILE
		{
			echo "${REPO_NAME}_REPO=$(git remote show origin -n | awk '/Fetch URL:/{print $3}')";
			echo "${branch}=${REPO_SHA}";
		}  2>&1 | tee -a $OUTPUT_FILE
	else
		echo "USE_TESTENV_PROPERTIES was set, not writing to testenv.properties"
	fi
}

parseCommandLineArgs "$@"
getTestenvProperties
