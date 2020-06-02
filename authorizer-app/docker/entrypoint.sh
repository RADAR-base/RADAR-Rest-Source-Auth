#!/bin/sh

ROOT_DIR=/usr/share/nginx/html
QUOTE='"'
# Replace env vars in JavaScript files
echo "Replacing env vars in JS"
for file in $ROOT_DIR/main.*.js* $ROOT_DIR/index.html $ROOT_DIR/polyfills.*.js* $ROOT_DIR/runtime.*.js*
do
  echo "Processing $file ...";

  sed -i 's|"BASE_HREF"|'${QUOTE}${BASE_HREF}${QUOTE}'|g' $file
  sed -i 's|"BACKEND_BASE_URL"|'${QUOTE}${BACKEND_BASE_URL}${QUOTE}'|g' $file
  sed -i 's|"VALIDATE"|'${QUOTE}${VALIDATE}${QUOTE}'|g' $file
  sed -i 's|"AUTH_GRANT_TYPE"|'${QUOTE}${AUTH_GRANT_TYPE}${QUOTE}'|g' $file
  sed -i 's|"AUTH_CLIENT_ID"|'${QUOTE}${AUTH_CLIENT_ID}${QUOTE}'|g' $file
  sed -i 's|"AUTH_CLIENT_SECRET"|'${QUOTE}${AUTH_CLIENT_SECRET}${QUOTE}'|g' $file
  sed -i 's|"AUTH_CALLBACK_URL"|'${QUOTE}${AUTH_CALLBACK_URL}${QUOTE}'|g' $file
  sed -i 's|"AUTH_URI"|'${QUOTE}${AUTH_URI}${QUOTE}'|g' $file
done

echo "Static files ready"
exec "$@"
