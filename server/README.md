Loom example
---

To run:

1. Build server
    `mvn clean package`
2. Start server using `./loom.sh` or `./noloom.sh` (requires correct location of java)
3. Build clients (`mvn clean package` from both `client-slow` and `client-quick` modules)
4. Run the clients from their module dir (`java -jar target/client-*.jar`)
5. See the output (reporst req/s, number of success, timed out and errors)

To run with reactive server:

use the same steps as above, just start from `server-reactive`
and instead of scripts start the server using
`java -jar target/server-reactive*.jar`
   