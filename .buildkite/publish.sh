#!/usr/bin/env bash

set -euv

echo $GPG_KEY | base64 --decode | gpg --batch --import

gpg --passphrase $GPG_PASSPHRASE --batch --yes -a -b LICENSE

if [[ "$BUILDKITE_BRANCH" == "develop" ]] || [[ "$BUILDKITE_BRANCH" == "ETCM-165-publish" ]]; then

    # Publish the -SNAPSHOT version.
    sbt publish-all

elif [[ "$BUILDKITE_BRANCH" == "master" ]]; then

    # Remove the -SNAPSHOT from the version file, then publish and release.
    sed -i 's/-SNAPSHOT//' version.sbt
    sbt "; publish-all; release-all"

else

  echo "Skipping the publish step."

fi
