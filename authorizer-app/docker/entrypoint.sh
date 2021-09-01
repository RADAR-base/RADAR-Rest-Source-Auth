#!/bin/sh

ROOT_DIR=/usr/share/nginx/html
# Replace env vars in JavaScript files
echo "Replacing env vars in JS"
# So far, the main.*.js is the only file where the app is compiled into
for file in $ROOT_DIR/main.*.js* $ROOT_DIR/index.html
do
  echo "Processing $file ...";

  sed -i 's|/\?BASE_HREF|'${BASE_HREF}'|g' $ROOT_DIR/index.html
  sed -i 's|BACKEND_BASE_URL|'${BACKEND_BASE_URL}'|g' $file
  sed -i 's|VALIDATE|'${VALIDATE}'|g' $file
  sed -i 's|AUTH_GRANT_TYPE|'${AUTH_GRANT_TYPE}'|g' $file
  sed -i 's|AUTH_CLIENT_ID|'${AUTH_CLIENT_ID}'|g' $file
  sed -i 's|AUTH_CLIENT_SECRET|'${AUTH_CLIENT_SECRET}'|g' $file
  sed -i 's|AUTH_CALLBACK_URL|'${AUTH_CALLBACK_URL}'|g' $file
  sed -i 's|AUTH_URI|'${AUTH_URI}'|g' $file
  sed -i 's|RADAR_BASE_URL|'${RADAR_BASE_URL}'|g' $file
done

echo "Static files ready"
exec "$@"
