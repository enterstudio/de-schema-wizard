[Back to Main](https://github.com/deleidos/de-schema-wizard/#schema-wizard)

# Contribute
To contribute to Schema Wizard, you will have to set up your environment to build and test the software.  A production deployment of Schema Wizard can only be run where Docker can run, but development deployments can be run in either Windows or Unix environments.  However, Schema Wizard relies on some native dependencies, so there are some minor differences between the two.

**Note that the Python executable is named differently in the two environments.**

Windows Developer Requirements (with executable names) -
* Java 8 (java)
* Maven 3 (mvn)
* Python 3.5 (python)
* Pip 8 for Python 3 (pip)
* Jetty 9
               
Linux Developer Requirements
* Java 8 (java)
* Maven 3 (mvn)
* Python 3.5 (python3)
* Pip 8 for Python 3 (pip3)
* Docker (docker)

## Project Structure
Schema Wizard is composed of eight projects: three Java, one Java/JavaScript, and four Python projects. 

### Java Projects
**h2-database**: Configures and starts up the H2 database.


**data-profiler**: The "brains" of the operation. It is composed of data accumulators and profilers and also is the sole interactor of the H2 database.


**data-model-factory**: Composed of the service layer, data analyzers, detectors, parsers, and splitters.  This project handles automated file detection/parsing.  There are two API entrypoints that are meant to offer plugability to detection and parsing.  **com.deleidos.dmf.framework.AbstractMarkSupportAnalyticsDetector** is the entrypoint for any detectors, and **com.deleidos.dmf.framework.AbstractAnalyticsParser** is the entrypoint for additional parser.  If Apache Tika can detect the file type, you need only implement a parser.  Otherwise, you must subclass both of these abstract classes.  Once you write the classes, add the fully resolved class names to the appropriate **data-model-factory/src/main/resources/META-INF/services/** service files.  For the time being, the javadocs in these classes are the reference material to help a developer add to Schema Wizard.


### Java/JavaScript Project
**schema-wizard**: The basis of the web application and communicates with the Java service layer (data-model-factory)


### Python Projects
**interpretation-engine**: Comprised of a service layer and logic to parse and conduct analysis on profiles created by the Schema Wizard data-profiler.


**interpretation-engine-mongodb**: Initializes the Mongo database with initial imports, either reverse-geocoding data or reverse-geocoding data and initial Domains.


**interpretation-engine-sidekick**: Responsible for creating, communicating, and terminating the untrusted container.


**interpretation-engine-untrusted**: Code that runs in an untrusted container to test the validity of a custom Interpretation.


## Building
Schema Wizard uses Apache Maven for build and dependency management. To build the project, navigate to the root project directory and run the command:
               
    mvn clean install
               
This will execute the build and also unit testing of the project based on the Reactor POM of Schema Wizard.

## Testing
Tests are broken into two categories, unit tests and integration tests.

Unit tests are run by default when executing:

    mvn clean install

In order to run integration tests, first start all of the necessary servers locally (H2, MongoDB, Python Interpretation Engine), and then run the respective command for your environment in the root project directory:

    mvn clean install -P integration-tests-windows
	
or
	
    mvn clean install -P integration-tests-unix
	
Developers who would like to use the DEBUG logs may either refer to the generated log files or modify the console output log level in the following log properties files:

	digitaledge_datamodeltoolkit \interpretation-engine\src\main\python\logging.conf
	digitaledge_datamodeltoolkit\schema-wizard\src\main\resources\log4j.properties

## Schema Wizard Deployment

This is a brief description of the deployment scheme that Schema Wizard uses.  The following are the conventional names of the Docker containers that are run.  Each container will be described in terms of the process it runs:
* **sw-h2**: H2 database that stores schema and sample data
* **sw-webapp**: Jetty container running the web application and processing data samples
* **sw-mongodb**: Mongo database that stores interpretation and domain data 
* **sw-ie**: Python web application that provides RESTful access to the Interpretation Engine
* **sw-sidekick**: a Python application that watches untrusted containers and handles untrusted communication
* **r_cache**: a Redis data structure store handles data exchange between untrusted and trusted code
* **sw-untrusted**: a repeated used Python stub that executes arbitrary Python code in an *isolated but insecure* environment
* **shared-volume**: a Docker volume that conveniently shares certain files across containers

After you successfully build the necessary artifacts with Maven, you can build the containers with these commands.  Just set the variables on the first two lines:

	schwiz_build_dir=<your-local-project-directory>
	cd ${schwiz_build_dir}/h2-database/target
    sudo cp ../Dockerfile .
    sudo docker build --force-rm=true --tag sw-h2 .
            
    cd ${schwiz_build_dir}/interpretation-engine-mongodb
    sudo docker build --force-rm=true --tag sw-mongodb .
            
    cd ${schwiz_build_dir}/interpretation-engine
    sudo docker build --force-rm=true --tag sw-ie .
            
    cd ${schwiz_build_dir}/schema-wizard/target
    sudo cp ../Dockerfile .
    sudo docker build --force-rm=true --tag sw-webapp .
            
    cd ${schwiz_build_dir}/interpretation-engine-sidekick
    sudo docker build --force-rm=true --tag sw-ie-sidekick .
            
    cd ${schwiz_build_dir}/interpretation-engine-untrusted
    sudo docker build --force-rm=true --tag sw-ie-untrusted .
    # This tag is used to set the $PULL_TAG environment variable in the sw-ie-sidekick container.

The following Docker commands are executed in order to start Schema Wizard.  If you have a Docker group set up, you may omit the superuser prefix.  These commands name each container the image name without the version:

    sudo docker pull sw-ie-untrusted
    sudo docker run -d -v /usr/local/shared/untrusted/ --name shared-volume python /bin/true
    sudo docker run -d -p 127.0.0.1:6379:6379 --name r_cache redis
    sudo docker run -d -p 127.0.0.1:9123:9123 --name sw-h2 sw-h2
    sudo docker run -d -p 127.0.0.1:27017:27017 --name sw-mongodb sw-mongodb
    sudo docker run -d -p 127.0.0.1:5000:5000 --link sw-mongodb:sw-mongodb --link r_cache:redis --name sw-ie sw-ie
    sudo docker run -d --link r_cache:redis -e "PULL_TAG=sw-ie-untrusted" -e "U_PROFILE=sw-script-profile" --volumes-from shared-volume -v /var/run/docker.sock:/var/run/docker.sock --name sw-sidekick sw-ie-sidekick
    sudo docker run -d -p 80:8080 --link sw-h2:h2-db --link sw-ie:sw-ie --name sw-webapp sw-webapp
               
Alternatively, use the docker-compose method of deployment by navigating to the root project directory and then executing

    sudo docker compose up
               
[//]: # (Links)

