Simple crawler
==============

![Simple crawler crawling Google](https://github.com/william8th/crawler/blob/develop/crawler.png)

A simple web crawler that crawls only a single domain. Example graph shown above is the result of crawling Google's home page.

# Dependencies
- Maven
- Java

# Getting started
- Run `mvn package` to obtain a runnable JAR
- Run `java -jar target/monzo-webcrawler-1.0-SNAPSHOT.jar http://somesite.com`
- Note that you should not include an ending forward slash at the end of the URL
- An output file named `output.html` will be produced

## Some extra options
```
usage: Monzo Webcrawler
    --external                    Adds external links to the output
    --help                        Print command line options
    --idle-time <idleTime>        The amount of time in whole seconds that
                                  a crawler would sit idle waiting for
                                  tasks
    --workers <numberOfWorkers>   The number of crawler workers to
                                  instantiate
```
Use `java -jar target/monzo-webcrawler-1.0-SNAPSHOT.jar --help` to see the different options

## Credits
Author: William Heng

Special thanks to Mike Bostock and Joerg Baach for their examples on how to use d3.js to output the visually stunning graphs
