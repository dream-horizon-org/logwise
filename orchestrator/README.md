# log-central-orchestrator

## Running Application

### Run migrations
* Bootstrap migrations: `mvn migrations:bootstrap`
* Run migrations: `mvn migrations:up`

### Start application
* Point project in IntelliJ to use Java 11 from project settings
* Create Intellij Run Configuration of type `Application`
* Add following VM options: `-Dapp.environment=local`
* Add following environment variables:
  ```shell
  ```

### Running Tests

* Add following environment variable: `ENV_NAME=test`
* Run `mvn clean verify`

## Code formatting

* Run `mvn com.spotify.fmt:fmt-maven-plugin:2.20:format` to auto format the code


## To Encrypt Pem File

* Set ENCRYPTION_IV and ENCRYPTION_KEY in environment variables
* Run `mvn compile exec:java -Dexec.mainClass="com.dream11.logcentralorchestrator.util.Encryption" -Dexec.args="<pem-file-path>"` from project root directory
