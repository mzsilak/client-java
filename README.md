# Client Skeletons in Java

This project provides client skeletons for the Arrowhead Framework Generation 4.0

These skeletons are to be extended with your own application code.

Contact: hegeduscs'at'aitia.ai

### Project structure

This is a multi module maven project, the [root `pom.xml`](https://github.com/arrowhead-f/client-java/blob/master/pom.xml) lists all the modules and common dependencies.

* **BasicConsumer**: this is a stand-alone plain Java project (not using maven), the only dependencies it needs are in `BasicConsumer/resources` folder
* **client-common**: a common library module for the other maven modules. Contains all the data transfer objects and common dependencies.
* **consumer**: client skeleton which requests a specific `ArrowheadService` from the Orchestrator
* **provider**: client skeleton which registers a specific `ArrowheadService` into the Service Registry and runs a web server where the service is available
* **publisher**: client skeleton which publishes events to the Event Handler core system
* **subscriber**: client skeleton which subscribes to specific event types at the Event Handler core system
