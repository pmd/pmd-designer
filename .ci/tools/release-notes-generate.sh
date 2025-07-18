#!/bin/bash
set -e

MILESTONE_NAME="next"
echo "Generating release notes (fixed issues, pull requests, dependencies) for milestone '${MILESTONE_NAME}'..."


BASEDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

read -s -p "Enter GitHub Token: " GITHUB_TOKEN
echo

CURL_API_HEADER=(--header "X-GitHub-Api-Version: 2022-11-28")
CURL_AUTH_HEADER=()
if [ -n "$GITHUB_TOKEN" ]; then
  echo "Will use env var GITHUB_TOKEN for github REST API"
  CURL_AUTH_HEADER=(--header "Authorization: Bearer $GITHUB_TOKEN")
fi

# determine current milestone
MILESTONE_JSON=$(curl "${CURL_API_HEADER[@]}" "${CURL_AUTH_HEADER[@]}" -s "https://api.github.com/repos/pmd/pmd-designer/milestones?state=open&direction=desc&per_page=10&page=1"|jq ".[] | select(.title == \"$MILESTONE_NAME\")")
#DEBUG ONLY
#MILESTONE_JSON='{"number":80,"closed_issues":40}'
#DEBUG ONLY
MILESTONE=$(echo "$MILESTONE_JSON" | jq .number)

PAGE="1"
HAS_NEXT="true"
ISSUES_JSON=""
while [ "$HAS_NEXT" = "true" ]; do
    echo "Fetching issues for milestone ${MILESTONE} page ${PAGE}..."
    URL="https://api.github.com/repos/pmd/pmd-designer/issues?state=closed&sort=created&direction=asc&per_page=30&page=${PAGE}&milestone=${MILESTONE}"
    RESPONSE="$(curl "${CURL_API_HEADER[@]}" "${CURL_AUTH_HEADER[@]}" -s -w "\nLink: %header{link}" "$URL")"

    #DEBUG ONLY
    #echo "$RESPONSE" > issues-response-${PAGE}.txt
    #RESPONSE="$(cat issues-response-${PAGE}.txt)"
    #DEBUG ONLY

    LINK_HEADER="$(echo "$RESPONSE" | tail -1)"
    BODY="$(echo "$RESPONSE" | head -n -1)"

    #DEBUG ONLY
    #echo "$BODY" > "issues-response-page-${PAGE}.txt"
    #BODY="$(cat "issues-response-page-${PAGE}.txt")"
    #DEBUG ONLY

    COMMA=","
    if [ "$PAGE" -eq 1 ]; then
        COMMA=""
    fi
    ISSUES_JSON="${ISSUES_JSON}${COMMA}${BODY}"

    if [[ $LINK_HEADER == *"; rel=\"next\""* ]]; then
        HAS_NEXT="true"
    else
        HAS_NEXT="false"
    fi
    PAGE=$((PAGE + 1))

    #DEBUG ONLY
    #HAS_NEXT="true"
    #if [ "$PAGE" -gt 2 ]; then break; fi
    #DEBUG ONLY

    # stop after 10 pages
    if [ "$PAGE" -gt 10 ]; then
        echo
        echo
        echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
        echo "!!!!!!!!!!!!!! reached page 10, stopping now !!!!!!!!!!!!!"
        echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
        echo
        echo
        break;
    fi
done


ISSUES_JSON="$(echo "[ $ISSUES_JSON ]" | jq 'flatten(1)')"
echo "Found $(echo "$ISSUES_JSON" | jq length) issues/pull requests"
#DEBUG ONLY
#echo "$ISSUES_JSON" > issues-all.txt
#DEBUG ONLY

FIXED_ISSUES_JSON="$(echo "$ISSUES_JSON" | jq 'map(select(has("pull_request") | not))')"
FIXED_ISSUES="$(echo "$FIXED_ISSUES_JSON" | jq --raw-output '.[] | "* [#\(.number)](https://github.com/pmd/pmd-designer/issues/\(.number)): \(.title | gsub("@"; "@<!-- -->") | gsub("\\["; "\\["))"')"
FIXED_ISSUES="**🐛 Fixed issues:**
$FIXED_ISSUES
"

PULL_REQUESTS_JSON="$(echo "$ISSUES_JSON" | jq 'map(select(has("pull_request"))) | map(select(contains({labels: [{name: "dependencies"}]}) | not))')"
PULL_REQUESTS="$(echo "$PULL_REQUESTS_JSON" | jq --raw-output '.[] | "* [#\(.number)](https://github.com/pmd/pmd-designer/pull/\(.number)): \(.title | gsub("@"; "@<!-- -->") | gsub("\\["; "\\[")) - @\(.user.login)"')"

AUTHORS="$(echo "$PULL_REQUESTS_JSON" | jq --raw-output '.[].user.login' | sort | uniq)"
echo "Resolving $(echo "$AUTHORS" | wc -l) author names in pull requests..."
for login in $AUTHORS; do
    USER_JSON="$(curl "${CURL_API_HEADER[@]}" "${CURL_AUTH_HEADER[@]}" -s "https://api.github.com/users/$login")"
    #DEBUG ONLY
    #USER_JSON="{\"login\": \"$login\", \"name\": \"foo $login\"}"
    #DEBUG_ONLY
    USER_NAME="$(echo "$USER_JSON" | jq --raw-output ".name // \"$login\"")"
    search=" - \@$login"
    replacement=" - [$USER_NAME](https://github.com/$login) (@$login)"
    PULL_REQUESTS="${PULL_REQUESTS//${search}/${replacement}}"
done

PULL_REQUESTS="**✨ Merged pull requests:**
$PULL_REQUESTS
"

DEPENDENCY_UPDATES_JSON="$(echo "$ISSUES_JSON" | jq 'map(select(has("pull_request"))) | map(select(contains({labels: [{name: "dependencies"}]})))')"
DEPENDENCY_UPDATES="$(echo "$DEPENDENCY_UPDATES_JSON" | jq --raw-output '.[] | "* [#\(.number)](https://github.com/pmd/pmd-designer/pull/\(.number)): \(.title | gsub("@"; "@<!-- -->") | gsub("\\["; "\\["))"')"
DEPENDENCY_UPDATES_COUNT=$(echo "$DEPENDENCY_UPDATES_JSON" | jq length)
if [ -z "$DEPENDENCY_UPDATES" ]; then
  DEPENDENCY_UPDATES="**📦 Dependency updates:**

No dependency updates.
"
else
  DEPENDENCY_UPDATES="**📦 Dependency updates:**
<details>
<summary>$DEPENDENCY_UPDATES_COUNT updates</summary>

$DEPENDENCY_UPDATES

</details>
"
fi

function insert() {
  local FULL_TEXT="$1"
  local FROM_MARKER="$2"
  local END_MARKER="$3"
  local INSERTION="$4"
  local fromLine
  local endLine
  local headText
  local tailText
  fromLine="$(echo "$FULL_TEXT" | grep -n "$FROM_MARKER" | head -1 | cut -d ":" -f 1)"
  endLine="$(echo "$FULL_TEXT" | grep -n "$END_MARKER" | head -1 | cut -d ":" -f 1)"
  headText="$(echo "$FULL_TEXT" | head -n "$((fromLine - 1))")"
  tailText="$(echo "$FULL_TEXT" | tail -n "+$endLine")"
  echo "$headText

$INSERTION

$tailText"
}

RELEASE_NOTES_FILE="${BASEDIR}/CHANGELOG.md"
echo "Updating $RELEASE_NOTES_FILE now..."

RELEASE_NOTES=$(cat "$RELEASE_NOTES_FILE")
echo "   adding fixed issues"
RELEASE_NOTES="$(insert "$RELEASE_NOTES" "**🐛 Fixed issues:**" "**✨ Merged pull requests:**" "$FIXED_ISSUES")"
echo "   adding pull requests"
RELEASE_NOTES="$(insert "$RELEASE_NOTES" "**✨ Merged pull requests:**" "**📦 Dependency updates:**" "$PULL_REQUESTS")"
echo "   adding dependencies"
RELEASE_NOTES="$(insert "$RELEASE_NOTES" "**📦 Dependency updates:**" "See https://github.com/pmd/pmd-designer/milestone/$MILESTONE" "$DEPENDENCY_UPDATES")"

echo "$RELEASE_NOTES" > "$RELEASE_NOTES_FILE"
