package com.williamheng.monzocrawler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

@Slf4j
public class MonzoCrawlerApplication {

    public static void main(String[] args) {

        Options options = new Options();

        CommandLineParser parser = new DefaultParser();

        try {

            CommandLine cmd = parser.parse(options, args);

            String[] arguments = cmd.getArgs();
            if (arguments.length <= 0) {
                log.error("Need to provide URL for the crawler to work");
            }



        } catch (ParseException e) {
            log.debug("Error creating command line parser", e);
        }
    }

}
