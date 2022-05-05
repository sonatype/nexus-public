Export npm metadata files from Nexus Repository 2.

**How to build jar file.**

$ run ./nxrm.groovy
Go to ~/lib/support/nexus2-npm-metadata-export.jar

**How to run the tool.**

Tool arguments:

1. -Ddb.url => The orient database url.
2. -Drepository.name => Repository name.
3. -Dexport.directory => Export directory.
4. Optional(default value = true). -Dabbreviated.packageroot => 
   (-Dabbreviated.packageroot is equivalent to -Dabbreviated.packageroot=true)
   The tool supports two export mode. The first one is full export of package roots(-Dabbreviated.packageroot=false) 
   and the second one is abbreviated(-Dabbreviated.packageroot).

**In case of -Ddb.url is an absolute path(/Users/sonatype/db/npm) then the connection will be built in 'plocal' mode.**

Orient 'plocal' is a single connection has to have no connection.
All connections must be shutdown before run.

**Do not recommend to use Orient 'remote' connection.**
Orient 'remote' connection requires below configuration set in Nexus Repository 2 nexus.properties.
nexus.orient.binaryListenerEnabled=true

$ java -jar JAR_NAME \
-Ddb.url=url \
-Drepository.name=name \    
-Dexport.directory=/Dir/

**Example.**

You want to export npm metadata files from Nexus Repository 2 npm repository npm-proxy into /Users/sonatype/exportdir directory.

Your params:
1. jar name = nexus2-npm-metadata-export.jar
2. -Ddb.url=plocal:/Users/sonatype/nexus2/sonatype-work/nexus/db/npm
3. -Drepository.name=npm-proxy
4. -Dexport.directory=/Users/sonatype/exportdir
5. Optional. In case of full export package roots -Dabbreviated.packageroot=false or abbreviated -Dabbreviated.packageroot

$ java -jar nexus2-npm-metadata-export.jar \
            -Ddb.url=plocal:/Users/sonatype/nexus2/sonatype-work/nexus/db/npm \
            -Drepository.name=npm-proxy \
            -Dexport.directory=/Users/sonatype/exportdir