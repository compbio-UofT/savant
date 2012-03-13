This release version 1.08 contains binary Jars for JEnsembl: the demonstration Ensembl Java API.

It also contains source and javadoc jars for each module - and it contains binary jars for dependencies.

----------------------------------------------------------------------------------------------------------

ENSEMBL Java API is a sourceforge hosted open-source project at https://sourceforge.net/projects/jensembl/

=========================LICENCE==========================================

Copyright (C) 2010-2011 The Roslin Institute <contact andy.law@roslin.ed.ac.uk>

This file is part of the Ensembl Java API demonstration project developed by the
Bioinformatics Group at The Roslin Institute, The Royal (Dick) School of
Veterinary Studies, University of Edinburgh.

This is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License (version 3) as published by
the Free Software Foundation.

This software is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in this software distribution. If not, see <http://www.gnu.org/licenses/gpl-3.0.html/>.

==========================================================================


The Ensembl Java API is a maven project, and uses the dependency mechanism, but
these binaries allow any Java project to use the compiled libraries.

Java code
_________

Is available by SVN client at https://jensembl.svn.sourceforge.net/svnroot/jensembl/trunk
or on the web at http://jensembl.svn.sourceforge.net/viewvc/jensembl/trunk/

For Maven Users etc ....all the required dependencies for the code are available at

central Central Repository http://repo1.maven.org/maven2
and 
biojava-maven-repo BioJava repository http://www.biojava.org/download/maven/


_________

Demonstration code 

testing and demonstrating the use of these binaries is available at 

https://jensembl.svn.sourceforge.net/svnroot/jensembl/trunk/EnsemblTest [SVN access]
or http://jensembl.svn.sourceforge.net/viewvc/jensembl/trunk/EnsemblTest/ [Web access]

and is included in this zip archive as: ensembl-test-1.08-sources.jar


=========================================================================================================

BINARIES

the individual jar/artifacts are available in 

		release1_08.zip (see below)

however, for convenience all of the jensembl binaries and the dependencies (apart from the configuration module) 
are bundled together in

		jensembl-bundle-no-config-1.08.jar

this binary jar requires the presence of a version of the config jar 

		ensembl-config-1.08.jar

this may be replaced with later versions of the config jar as ensembl releases new database versions, 
as long as no critical alterations to the ensembl schema are mode (requiring updates to the JEnsembl
datamapping code).

alternatively, local configuration files may be loaded in conjunction with the config jar. 

==========================================================================================================

release1_08.zip contents
------------------------

ENSEMBL JAVA API (Binary, source and javadoc jars)
__________________________________________________

ensembl-config-1.08.jar
ensembl-config-1.08-javadoc.jar
ensembl-config-1.08-sources.jar
ensembl-data-access-1.08.jar
ensembl-data-access-1.08-javadoc.jar
ensembl-data-access-1.08-sources.jar
ensembl-data-access-interface-1.08.jar
ensembl-data-access-interface-1.08-javadoc.jar
ensembl-data-access-interface-1.08-sources.jar
ensembl-datamapper-1.08.jar
ensembl-datamapper-1.08-javadoc.jar
ensembl-datamapper-1.08-sources.jar
ensembl-datasource-aware-model-1.08.jar
ensembl-datasource-aware-model-1.08-javadoc.jar
ensembl-datasource-aware-model-1.08-sources.jar
ensembl-model-1.08.jar
ensembl-model-1.08-javadoc.jar
ensembl-model-1.08-sources.jar

(source code available at 
https://jensembl.svn.sourceforge.net/svnroot/jensembl/tags/jensembl_project_1.05)

DEMONSTRATION CODE (source and javadoc jars)
____________________________________________

ensembl-test-1.08-sources.jar


(source code available at 
https://jensembl.svn.sourceforge.net/svnroot/jensembl/tags/jensembl_project_1.08)


BIOJAVA 3 DEPENDENCIES (see http://www.biojava.org/wiki/BioJava3_project )
___________________________________________________________________________

jensembl now uses  release 3.0

biojava3-core-3.0.jar


        <dependency>
            <groupId>org.biojava</groupId>
            <artifactId>biojava3-core</artifactId>
            <version>3.0</version>
        </dependency>

from

biojava-maven-repo BioJava repository http://www.biojava.org/download/maven/

        <repository>
            <id>biojava-maven-repo</id>
            <name>BioJava repository</name>
            <url>http://www.biojava.org/download/maven/</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
            <releases>
                <enabled>true</enabled>
                <checksumPolicy>ignore</checksumPolicy>
            </releases>
        </repository>



GENERAL DEPENDENCIES
____________________
     
commons-dbcp-1.2.2.jar          
google-collections-1.0.jar
commons-pool-1.3.jar            
log4j-1.2.16.jar
mysql-connector-java-5.1.6.jar
mybatis-3.0.2.jar
cglib-2.1_3.jar
asm-1.5.3.jar
  

readme.txt
GPLv3licence.txt         
