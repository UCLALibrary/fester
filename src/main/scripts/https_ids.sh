#!/usr/bin/env bash
set -euo pipefail

# Script ENVs
S3_BUCKET="prod-iiif-fester-source"
BUCKET_TYPE="collections"
HOST_UUID="b1dbe4a0-443c-479f-bf0a-25c352df0d8f"

# Output usage information
usage() {
  echo "Usage: $(basename "$0") ark:/21198/z1p08jwd" >&2
  exit 2
}

# Convert an ID to an S3 path using our path conventions
id_to_s3_path() {
  local -a tokens id_tokens
  local token s3_path

  # Parse the ID tokens from script argument
  IFS='/' read -r -a tokens <<< "$1"

  # Filter out empties caused by two delimiters together
  id_tokens=()
  for token in "${tokens[@]}"; do
    [[ -n "$token" ]] && id_tokens+=("$token")
  done

  # Reconstruct the S3 bucket path
  IFS='/' s3_path="${id_tokens[*]}"
  printf '%s' "$s3_path"
}


# Require exactly one, non-empty arg
[[ $# -eq 1 ]] || usage
[[ -n "$1" ]] || usage

# Check that the necessary dependencies are installed
command -v aws >/dev/null || { echo "Error: aws CLI not found in PATH" >&2; exit 127; }

printf 'Checking: %s\n' "$1"

# Build the S3 path
s3_path="$(id_to_s3_path "$1")"

# Build an array of all the files in the S3 bucket directory by relying on S3's expected output format
mapfile -t files < <(
  aws s3 ls "s3://${S3_BUCKET}/${BUCKET_TYPE}/${s3_path}" | awk '{ $1=$2=$3=""; sub(/^ +/,""); print }'
)

# Create a tmp directory to use as a scratch space and clean it up automatically
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

# Download the files to be updated, update them, and re-upload the files
for file_name in "${files[@]}"; do
  # Before copying, trim the last directory in the S3 path since we don't seem to have used it
  aws s3 cp "s3://${S3_BUCKET}/${BUCKET_TYPE}/${s3_path%/*}/${file_name}" "${tmpdir}/${file_name}" \
    --only-show-errors --no-progress

  if sed --version >/dev/null 2>&1; then
    # GNU sed (Linux/Windows via WSL)
    sed -i "s|http://${HOST_UUID}|https://${HOST_UUID}|g" "${tmpdir}/${file_name}"
  else
    # BSD sed (macOS)
    sed -i '' "s|http://${HOST_UUID}|https://${HOST_UUID}|g" "${tmpdir}/${file_name}"
  fi

  # Destructive write back into S3
  aws s3 cp "${tmpdir}/${file_name}" "s3://${S3_BUCKET}/${BUCKET_TYPE}/${s3_path%/*}/${file_name}" \
    --only-show-errors --no-progress
done

echo "  Succeeded updating IDs in $1"
