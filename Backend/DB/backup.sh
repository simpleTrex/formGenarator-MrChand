wget https://fastdl.mongodb.org/tools/db/mongodb-database-tools-ubuntu2204-x86_64-100.9.4.deb
mongodump --host=localhost --port=27017 --db=app
#mongorestore --host=localhost --port=27017 --db=app dump/app/
