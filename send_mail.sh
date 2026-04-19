#!/usr/bin/env bash

set -e

FROM="$1"
TO="$2"
SUBJECT="$3"
BODY="$4"
HOST="${5:-127.0.0.1}"
PORT="${6:-2525}"

if [[ -z "$FROM" || -z "$TO" || -z "$SUBJECT" || -z "$BODY" ]]; then
  echo "Usage: $0 <from> <to> <subject> <body> [host] [port]"
  exit 1
fi

printf "EHLO localhost\r\nMAIL FROM:<%s>\r\nRCPT TO:<%s>\r\nDATA\r\nFrom: %s\r\nTo: %s\r\nSubject: %s\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: 8bit\r\n\r\n%s\r\n.\r\nQUIT\r\n" \
  "$FROM" "$TO" "$FROM" "$TO" "$SUBJECT" "$BODY" \
  | nc "$HOST" "$PORT"