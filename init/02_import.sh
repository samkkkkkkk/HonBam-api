#!/bin/bash
set -euo pipefail

echo "[mysql init] CSV import start"

DB="${MYSQL_DATABASE:-honbam}"
CSV="/var/lib/mysql-files/cocktail_recipe.csv"

if [ ! -f "$CSV" ]; then
  echo "[mysql init] CSV not found at $CSV — skip"
  exit 0
fi

# CSV가 CRLF면 '\r\n'로, LF면 '\n'로 맞춰라. (먼저 시도: '\r\n')
mysql --local-infile=1 -u root -p"$MYSQL_ROOT_PASSWORD" "$DB" -e "
LOAD DATA LOCAL INFILE '${CSV}'
INTO TABLE tbl_recipe
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' ESCAPED BY ''
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(@csv_id, cocktail_img, cocktail_name, @recipe_raw, @detail_raw)
SET data_id = NULL,
    recipe = NULLIF(@recipe_raw,''),
    recipe_detail = NULLIF(@detail_raw,'');
"

echo "[mysql init] CSV import done"
