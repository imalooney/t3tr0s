#!/bin/bash

set -ex

deploy_dir=deployed

if [ ! -d "$deploy_dir" ]; then
  git clone git@github.com:t3tr0s/t3tr0s.github.io.git $deploy_dir
fi

# Render publishable singleplayer page
cp config.json config.json.bak
echo '{"minified-client": true, "use-repl": false, "single-player-only": true}' > config.json
SKIP_SERVER=true node server.js
mv config.json.bak config.json

# Inside deploy directory
pushd $deploy_dir

# Delete everything except readme
rm -rf *
git checkout -- README.md

# Copy over build files
cp -r ../public/* .

# Remove intermediate compiler js
rm -rf out

# Commit and push
git add -u
git add .
git commit -m "manual update from publish.sh"
git push origin master

popd

set +e
echo
echo 'Published T3TR0S (singleplayer) to https://t3tr0s.github.io'
echo
