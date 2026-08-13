#!/usr/bin/env python3
"""Publish a signed Nova AAB to a Google Play track using the official Publishing API."""

from __future__ import annotations

import argparse
from pathlib import Path

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--package-name", required=True)
    parser.add_argument("--bundle", required=True)
    parser.add_argument("--track", default="alpha")
    parser.add_argument("--priority", type=int, default=2)
    parser.add_argument("--release-name", required=True)
    parser.add_argument("--service-account-json-file", required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    bundle_path = Path(args.bundle).resolve()
    credentials_path = Path(args.service_account_json_file).resolve()

    if not bundle_path.is_file():
        raise SystemExit(f"AAB not found: {bundle_path}")
    if not credentials_path.is_file():
        raise SystemExit(f"Service-account JSON not found: {credentials_path}")
    if args.priority < 0 or args.priority > 5:
        raise SystemExit("In-app update priority must be between 0 and 5.")

    credentials = service_account.Credentials.from_service_account_file(
        credentials_path,
        scopes=[ANDROID_PUBLISHER_SCOPE],
    )
    publisher = build(
        "androidpublisher",
        "v3",
        credentials=credentials,
        cache_discovery=False,
    )

    edit = publisher.edits().insert(
        packageName=args.package_name,
        body={},
    ).execute()
    edit_id = edit["id"]

    print(f"Created Google Play edit {edit_id}")

    upload = publisher.edits().bundles().upload(
        packageName=args.package_name,
        editId=edit_id,
        media_body=MediaFileUpload(
            str(bundle_path),
            mimetype="application/octet-stream",
            resumable=True,
            chunksize=5 * 1024 * 1024,
        ),
    )

    uploaded_bundle = None
    while uploaded_bundle is None:
        status, uploaded_bundle = upload.next_chunk(num_retries=3)
        if status is not None:
            print(f"Upload progress: {int(status.progress() * 100)}%")

    version_code = str(uploaded_bundle["versionCode"])
    print(f"Uploaded AAB versionCode={version_code}")

    publisher.edits().tracks().update(
        packageName=args.package_name,
        editId=edit_id,
        track=args.track,
        body={
            "releases": [
                {
                    "name": args.release_name,
                    "versionCodes": [version_code],
                    "status": "completed",
                    "inAppUpdatePriority": args.priority,
                }
            ]
        },
    ).execute()

    publisher.edits().commit(
        packageName=args.package_name,
        editId=edit_id,
    ).execute()

    print(
        "Published versionCode="
        f"{version_code} to track={args.track} priority={args.priority}"
    )


if __name__ == "__main__":
    main()
