#!/bin/sh

ROOT_DIR=/usr/share/nginx/html

# Replace env vars in JavaScript files
echo "Replacing env vars in JS"
for file in $ROOT_DIR/js/app.*.js* $ROOT_DIR/index.html $ROOT_DIR/precache-manifest*.js;
do
  echo "Processing $file ...";

  sed -i 's|/\?BASE_HREF|'${BASE_HREF}'|g' $file
  sed -i 's|BACKEND_BASE_URL|'${BACKEND_BASE_URL}'|g' $file
  sed -i 's|VALIDATE|'${VALIDATE}'|g' $file
  sed -i 's|AUTH_GRANT_TYPE|'${AUTH_GRANT_TYPE}'|g' $file
  sed -i 's|AUTH_CLIENT_ID|'${AUTH_CLIENT_ID}'|g' $file
  sed -i 's|AUTH_CLIENT_SECRET|'${AUTH_CLIENT_SECRET}'|g' $file
  sed -i 's|AUTH_CALLBACK_URL|'${AUTH_CALLBACK_URL}'|g' $file
  sed -i 's|AUTH_URI|'${AUTH_URI}'|g' $file
done

echo "Static files ready"
exec "$@"
