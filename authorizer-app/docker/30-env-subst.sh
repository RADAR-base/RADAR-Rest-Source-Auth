#!/bin/sh

set -e

replace() {
  find="$1" replace="$2" file="$3" tmpfile="$(mktemp)"
  sed -r "s|${find}|${replace}|g" "$file" > "$tmpfile"
  cat "$tmpfile" > "$file"
  rm "$tmpfile"
}

cd /usr/share/nginx/html

BASE_HREF=$(printf "%s" "${BASE_HREF}" | sed 's|/$||' )

case "$BASE_HREF" in
  /*)
    # Do nothing
    ;;
  *)
    BASE_HREF="/$BASE_HREF"
    ;;
esac

# replace BASE_HREF value
replace "(\<base\ href\=\")([^\"]+)" "\\1${BASE_HREF}/" index.html
replace "BASE_HREF" "$BASE_HREF" /etc/nginx/conf.d/default.conf

if [ "$BASE_HREF" = "/" ]; then
  replace " deny all;" " allow all;" /etc/nginx/conf.d/default.conf
fi

# Replace env vars in JavaScript files
echo "Replacing env vars in JS"
# So far, the main.*.js is the only file where the app is compiled into
for file in main.*.js
do
  echo "Processing $file ...";

  replace "BASE_HREF" "${BASE_HREF}" "$file"
  replace "BACKEND_BASE_URL" "${BACKEND_BASE_URL}" "$file"
  replace "VALIDATE" "${VALIDATE}" "$file"
  replace "AUTH_GRANT_TYPE" "${AUTH_GRANT_TYPE}" "$file"
  replace "AUTH_CLIENT_ID" "${AUTH_CLIENT_ID}" "$file"
  replace "AUTH_CLIENT_SECRET" "${AUTH_CLIENT_SECRET}" "$file"
  replace "AUTH_CALLBACK_URL" "${AUTH_CALLBACK_URL}" "$file"
  replace "AUTH_URI" "${AUTH_URI}" "$file"
  replace "RADAR_BASE_URL" "${RADAR_BASE_URL}" "$file"
done

for f in main.*.js index.html; do
  gzip -c "$f" > "$f.gz"
done

echo "Static files ready"
exec "$@"
