ANOTACOES

para fazer dump do banco:

docker exec -t postgres_local pg_dump -U nicolas -d local -F p > db_dump.sql

para restaurar arquivo de dump do banco:

cat db_dump.sql | podman exec -i postgres_local psql -U nicolas -d local
podman exec -i postgres_local psql -U nicolas -d terminaldeconsulta -f /path/to/db_dump_tc.sql
podman exec -i postgres_local psql -U nicolas -d terminaldeconsulta < db_dump_tc.sql

para se conectar ao banco por psql

PGPASSWORD="mypassword" psql -h localhost -p 5432 -U nicolas -d postgres
psql -U arrumaai -d hibernate_db -h 127.0.0.1 -p 5432 -f backup_1.sql
