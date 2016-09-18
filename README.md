# gradle-dependencies-to-pom
Experimental tool to parse gradle files and generate a pom

Basically, it parses gradle files, looking for anything that would be considered a dependency and then creates a maven pom file.

# Why?

Developers don't always have the luxury of running software builds while internet connected. This helps you get the process going to use Maven to sync all necessary dependences to maven Local

## Usage

git clone ....
mvn install
java -jar target/GradleSyncer-1.0.0-SNAPSHOT-jar-with-dependencies.jar (Path To Gradle Project)