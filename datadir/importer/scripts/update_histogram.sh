### NOTE: this script will need a postgresql client to be already installed
###
### For example : postgresql-client-9.6

##################################
# Set PSQL variables with defaults
##################################

export PGHOST=${PGHOST-localhost}
export PGPORT=${PGPORT-5432}
export PGDATABASE=${PGDATABASE-database}
export PGUSER=${PGUSER-username}
export PGPASSWORD=${PGPASSWORD-password}

##################################
# Actual query
##################################
TABLE_NAME=${2:-metopa}
# Remove namespace if the full name of the layer has been passed, instead of the table name
# This requires the layer name and the table name to be identical
TABLE_NAME=$(echo $TABLE_NAME | sed 's/^.*://')

psql -c "Insert into ${TABLE_NAME}_histogram  select time, st_setsrid(st_extent(the_geom)::geometry, 4326) as the_geom from ${TABLE_NAME} where time = '$1' group by time ON CONFLICT (time) DO UPDATE SET the_geom = EXCLUDED.the_geom"
