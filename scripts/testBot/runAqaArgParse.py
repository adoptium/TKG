import argparse
import json
import sys

def map_platforms(platforms):
    """ Takes in a list of platforms and translates Grinder platorms to corresponding GitHub-hosted runners.
        This function both modifies and returns the 'platforms' argument.
    """
    
    platform_map = {
        'x86-64_windows': 'windows-latest',
        'x86-64_mac': 'macos-latest',
        'x86-64_linux': 'ubuntu-latest'
    }
    
    for i, platform in enumerate(platforms):
        if platform in platform_map:
            platforms[i] = platform_map[platform]
    
    return platforms

def underscore_targets(targets):
    """ Takes in a list of targets and prefixes them with an underscore if they do not have one.
    """
    result = []
    for target in targets:
        t = target
        if target[0] != '_':
            t = '_' + t
        result.append(t)
    return result

def main():

    # The keyword for this command.
    keyword = 'run aqa'
    keywords = keyword.split()

    # We assume that the first argument is/are the keyword(s).
    # e.g. sys.argv == ['action_argparse.py', 'run', 'aqa', ...]
    raw_args = sys.argv[1 + len(keywords):]

    # Strip leading and trailing whitespace. Remove empty arguments that may result after stripping.
    raw_args = list(filter(lambda empty: empty, map(lambda s: s.strip(), raw_args)))

    parser = argparse.ArgumentParser(prog=keyword, add_help=False)
    # Improvement: Automatically resolve the valid choices for each argument populate them below, rather than hard-coding choices.
    parser.add_argument('--help', '-h', action='store_true')
    parser.add_argument('--sdk_resource', default=['nightly'], choices=['nightly', 'releases', 'customized', 'build-jdk'], nargs='+')
    parser.add_argument('--build_repo', default=['adoptium/build-jdk:master'], nargs='+')
    parser.add_argument('--customized_sdk_url', default=['None'], nargs='+')
    parser.add_argument('--archive_extension', default=['.tar'], choices=['.zip', '.tar', '.7z'], nargs='+')
    parser.add_argument('--build_list', default=['openjdk'], choices=['openjdk', 'functional', 'system', 'perf', 'external'], nargs='+')
    parser.add_argument('--target', default=['_jdk_math'], nargs='+')
    parser.add_argument('--platform', default=['x86-64_linux'], nargs='+')
    parser.add_argument('--jdk_version', default=['8'], nargs='+')
    parser.add_argument('--jdk_impl', default=['openj9'], choices=['hotspot', 'openj9'], nargs='+')

    # Custom repo options which may be enabled/disabled in the `runAqaConfig.json` file.
    with open('main/.github/workflows/runAqaConfig.json') as f:
        config = json.load(f)
        if config['custom_openjdk_testrepo']:
            parser.add_argument('--openjdk_testrepo', default=['adoptium/aqa-tests:master'], nargs='+')
        if config['custom_openj9_repo']:
            parser.add_argument('--openj9_repo', default=['eclipse-openj9/openj9:master'], nargs='+')
        if config['custom_tkg_repo']:
            parser.add_argument('--tkg_repo', default=['adoptium/TKG:master'], nargs='+')

    args = vars(parser.parse_args(raw_args))
    # All args are lists of strings

    # Help was requested. The simplest way to handle this is as if it were an error
    # because stdout is reserved for workflow commands and logs.
    if args['help']:
        # For some unknown reason, the first tilde in this help message should not be escaped,
        # otherwise a syntax error occurs when passed to the GitHub Script.
        help_msg = '''Run AQA GitHub Action Documentation
`\\`\\`
https://github.com/adoptium/aqa-tests/blob/master/doc/RunAqa.md
\\`\\`\\`
Click the above link to view the documentation for the Run AQA GitHub Workflow'''
        sys.stderr.write(help_msg)
        print('::error ::Help command invoked')
        exit(2)
    # Remove help flag from the args because it is not a parameter for the job matrix.
    del args['help']

    # Map grinder platform names to github runner names
    args['platform'] = map_platforms(args['platform'])

    # Underscore the targets if necessary
    args['target'] = underscore_targets(args['target'])

    # Split repo and branch from the `build_repo` arg since it is not handled by the AdoptOpenJDK/build-jdk action.
    build_repo_branch = []
    for br in args['build_repo']:
        spl = br.split(':')
        if len(spl) != 2:
            err_msg = 'Invalid repo+branch suppplied to argument --build_repo. Expecting format <repository>:<branch>'
            print('::error ::{}'.format(err_msg))
            sys.stderr.write(err_msg)
            exit(2)
        repo, branch = spl
        entry = {'repo': repo, 'branch': branch}
        build_repo_branch.append(entry)
    args['build_repo_branch'] = build_repo_branch

    # If 'customized' sdk_resource, then customized_sdk_url and archive_extension must be present.
    if 'customized' in args['sdk_resource'] and args['customized_sdk_url'] == ['None']:
        err_msg = '--customized_sdk_url must be provided if sdk_resource is set to customized.'
        print('::error ::{}'.format(err_msg))
        sys.stderr.write(err_msg)
        exit(2)

    # Output the arguments
    print('::set-output name=build_parameters::{}'.format(json.dumps(args)))
    for key, value in args.items():
        print('::set-output name={}::{}'.format(key, value))

if __name__ == "__main__":
    main()
