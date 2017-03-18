package com.williamheng.monzocrawler;

import com.williamheng.monzocrawler.crawler.MonzoCrawlerOrchestrator;
import com.williamheng.monzocrawler.model.Matrix;
import com.williamheng.monzocrawler.model.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.client.Client;
import java.net.MalformedURLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MonzoCrawlerApplication {

    public static int DEFAULT_NUMBER_OF_CRAWLERS = 4;

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption(numberOfWorkersOption());
        options.addOption(helpOption());

        CommandLineParser parser = new DefaultParser();

        try {

            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) printHelp(options);

            // Getting the URL to crawl
            String[] arguments = cmd.getArgs();
            if (arguments.length <= 0) {
                log.error("Need to provide URL for the crawler to work");
                System.exit(0);
            }

            String url = arguments[0];
            int numberOfWorkers = numberOfWorkers(cmd);
            Client client = JerseyClientBuilder.createClient();
            BlockingQueue<Resource> queue = new LinkedBlockingQueue<>();
            MonzoCrawlerOrchestrator monzoCrawlerOrchestrator = new MonzoCrawlerOrchestrator(client, url, numberOfWorkers, queue);
            Future<Matrix> matrixFuture = monzoCrawlerOrchestrator.initCrawlOperation();

            log.info("Crawling URL {} with {} workers", url, numberOfWorkers);
            matrixFuture.get();

            monzoCrawlerOrchestrator.shutdown();
            log.info("Done.");

        } catch (ParseException e) {
            log.debug("Error creating command line parser", e);
        } catch (IllegalArgumentException e) {
            log.error("Number of workers provided is invalid");
        } catch (MalformedURLException e) {
            log.error("URL given is malformed. Please do not include a trailing slash. Example:\nhttp://google.com");
        } catch (InterruptedException | ExecutionException e) {
            log.error("Crawling operation is interrupted");
        }
    }

    private static int numberOfWorkers(CommandLine cmd) {
        int numberOfWorkers = DEFAULT_NUMBER_OF_CRAWLERS;
        if (cmd.hasOption("w")) {
            int workersInput = Integer.parseInt(cmd.getOptionValue("w"));
            if (workersInput <= 0) throw new IllegalArgumentException("Invalid number of workers");
            numberOfWorkers = workersInput;
        }
        return numberOfWorkers;
    }

    private static void printHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("Monzo Webcrawler", options);
        System.exit(0);
    }

    private static Option numberOfWorkersOption() {
        return Option.builder()
                .argName("numberOfWorkers")
                .hasArg(true)
                .longOpt("workers")
                .desc("The number of crawler workers to instantiate")
                .build();
    }

    private static Option helpOption() {
        return Option.builder()
                .longOpt("help")
                .desc("Print command line options")
                .build();
    }

}
