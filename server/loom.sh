/Library/Java/JavaVirtualMachines/jdk-18-loom.jdk/Contents/Home/bin/java \
     --enable-preview \
     -Dserver.executor-service.virtual-threads=true \
     -Dserver.executor-service.virtual-enforced=true \
     -jar target/server-1.0.0-SNAPSHOT.jar