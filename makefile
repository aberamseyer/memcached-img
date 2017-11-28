CP = -cp "spymemcached-2.10.3.jar:."
DIR = -d ./

all: main

main:
	javac ${CP} src/*.java ${DIR}
clean:
	rm -f *.class
app: clean main
	java ${CP} WebServer 12430
memcached: clean main
	memcached -p 12250 -I 2m -vv &
	java Dispatcher 12430
