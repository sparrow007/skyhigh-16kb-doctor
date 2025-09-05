# Clone or create the repository
if [ -d ../skyhigh-16kb-doctor ]; then
  cd ../skyhigh-16kb-doctor
else
  mkdir ../skyhigh-16kb-doctor && cd ../skyhigh-16kb-doctor
fi

# Copy all files as shown below into their respective locations
./gradlew init --dsl kotlin --type kotlin-gradle-plugin --project-name plugin

# Build the plugin
./gradlew -p plugin build

# Run tests
./gradlew -p plugin test

# Test with sample app
#cd app
#./gradlew skyhighDoctor
#
## Publish to local repository for testing
#./gradlew -p plugin-build publishToMavenLocal