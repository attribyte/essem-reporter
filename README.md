![Essem Logo](essemlogo0.png)

[Essem](https://github.com/attribyte/essem) is a server that accepts, stores, indexes, and graphs metrics as supplied
by [Dropwizard Metrics](http://metrics.dropwizard.io/4.0.0/).
This package provides a [`ScheduledReporter`](https://github.com/dropwizard/metrics/blob/4.0-maintenance/metrics-core/src/main/java/com/codahale/metrics/ScheduledReporter.java)
compatible with Essem, a reservoir and `Timer` based on [HDR Histogram](https://github.com/HdrHistogram/HdrHistogram),
and (experimental) tools for extracting system metrics from Linux systems. 

Requirements
------------

* [JRE/JDK 8+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Protobuf 2.6.1](https://github.com/google/protobuf/releases/tag/v2.6.1)

Build
-----

The build requires maven, e.g. `mvn package`.

License
-------

Copyright 2014-2018 [Attribyte, LLC](https://attribyte.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.