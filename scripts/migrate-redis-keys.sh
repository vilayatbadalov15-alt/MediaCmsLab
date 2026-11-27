#!/bin/sh
# Run on the previous standalone Redis with application writers stopped.
set -eu
keys=$(mktemp)
trap 'rm -f "$keys"' EXIT
redis-cli -e --scan --pattern 'analytics:*' > "$keys"
while IFS= read -r old; do
    case "$old" in
        analytics:views:content:*|analytics:top:views:*|analytics:dau:*) ;;
        *) continue ;;
    esac
    new="{analytics}:${old#analytics:}"
    # A daily key may expire between SCAN and RENAME. Preserve TTL and refuse collisions.
    redis-cli -e EVAL '
        if redis.call("EXISTS", KEYS[1]) == 0 then return 0 end
        if redis.call("RENAMENX", KEYS[1], KEYS[2]) == 0 then
            return redis.error_reply("destination already exists: " .. KEYS[2])
        end
        return 1' 2 "$old" "$new" > /dev/null
done < "$keys"
