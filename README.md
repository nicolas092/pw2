ANOTACOES

para fazer dump do banco:

docker exec -t postgres_local pg_dump -U nicolas -d local -F p > db_dump.sql

para restaurar arquivo de dump do banco:

cat db_dump.sql | podman exec -i postgres_local psql -U nicolas -d local

para se conectar ao banco por psql

PGPASSWORD="mypassword" psql -h localhost -p 5432 -U nicolas -d postgres
