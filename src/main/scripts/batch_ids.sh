
#!/usr/bin/env bash
set -euo pipefail

##
# This little wrapper script runs the `https_id.sh` script in batch mode.
##

S3_BUCKET="prod-iiif-fester-source"
RECORD_TYPE="collections"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

aws s3api list-objects-v2 --bucket "$S3_BUCKET" --prefix "$RECORD_TYPE/" --query 'Contents[].Key' --output text \
| tr '\t' '\n' \
| while IFS= read -r key; do
    # Extract just the ARK from the S3 key, which is what the other script expects
    key="${key#$RECORD_TYPE/}"
    key="${key%.json}"

    # Run the ARK against the HTTPS ID cleanup script; skip keys that are not ARKs
    [[ "$key" == ark* ]] || continue
    "$SCRIPT_DIR/https_ids.sh" "$key"
  done
