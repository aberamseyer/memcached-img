# memcached-img

A use of memcached that should never actually happen in real life: this program uses memcached to store/retrieve any files it serves to clients (via a simple web server program). 

Meant to be used on a minimum of 3 machines: 1 that will run memcached and perform an extremely simple round-robin request dispatch, and 2+ that will act as the real web servers to handle GET HTTP requests.

compile/run on memcached server first:
```bash
make memcached
```
compile/run on web server machine(s) second:
```bash
make app
```
