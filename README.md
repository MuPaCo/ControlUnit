# DevOpt Control Unit
The [DevOpt-project](http://www.devopt-projekt.de) aims at an approach for the development and operation of so-called controlled emergent systems.
That means an approach for complex and hierarchical software systems, which are under control of superior instances, but yield new behavior based on their own adaptation.
For that purpose, an emergent and distributed system is understood as a three-layered architecture, which consists of a local (IoT) layer, a control layer, and a DevOps layer.

A control unit in the DevOpt-approach realizes the logical concepts of the control layer in practice.
This repository hosts the code of such a control unit.

## Installation
The control unit requires Java 11 to be installed (Oracle or OpenJDK, while OpenJDK is preferred).
Other Java versions are not recommended and may cause runtime errors.

The executable Java archive `control-node.jar` can be downloaded from the [Releases](https://github.com/MuPaCo/ControlUnit/releases).

## Execution
Start a control unit instance by executing the Java archive as follows:

`java -jar control-node.jar [File]`

The optional `[File]` defines the path to and name of the configuration file to use for setting up the control unit.
The available configuration options and their valid values are described in a [template configuration file](https://github.com/MuPaCo/ControlUnit/blob/master/testdata/setup/description.cfg).
This file also describes the default values, which are used, if the `[File]` argument is not provided or the configuration file does not contain mandatory definitions.

## License
This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

## Acknowledgments
This work is partially supported by the [DevOpt-project](http://www.devopt-projekt.de), funded by the German Ministry of Research and Education (BMBF) under grant 01IS18076A.
