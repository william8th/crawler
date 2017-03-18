package com.williamheng.monzocrawler;

import com.williamheng.monzocrawler.crawler.MonzoCrawlerOrchestrator;
import com.williamheng.monzocrawler.model.Graph;
import com.williamheng.monzocrawler.model.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.client.Client;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MonzoCrawlerApplication {

    public static int DEFAULT_NUMBER_OF_CRAWLERS = 4;
    public static int DEFAULT_IDLE_TIME = 4;

    private static final String WORKERS_OPTION = "workers";
    private static final String IDLE_TIME_OPTION = "idle-time";
    private static final String EXTERNAL_LINKS_OPTION = "external";
    private static final String HELP_OPTION = "help";

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption(numberOfWorkersOption());
        options.addOption(idleTimeOption());
        options.addOption(addExternalLinksOption());
        options.addOption(helpOption());

        CommandLineParser parser = new DefaultParser();

        try {

            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(HELP_OPTION)) printHelp(options);

            // Getting the URL to crawl
            String[] arguments = cmd.getArgs();
            if (arguments.length <= 0) {
                log.error("Need to provide URL for the crawler to work");
                System.exit(0);
            }

            String url = arguments[0];
            int numberOfWorkers = numberOfWorkers(cmd);
            int idleTime = idleTime(cmd);
            boolean addExternalLinks = cmd.hasOption(EXTERNAL_LINKS_OPTION);
            Client client = JerseyClientBuilder.createClient(new ClientConfig().property(ClientProperties.FOLLOW_REDIRECTS, true));
            BlockingQueue<Resource> queue = new LinkedBlockingQueue<>();
            MonzoCrawlerOrchestrator monzoCrawlerOrchestrator = new MonzoCrawlerOrchestrator(client, url, queue, numberOfWorkers, idleTime, addExternalLinks);

            log.info("Crawling URL {} with {} workers", url, numberOfWorkers);
            Future<Graph> graphFuture = monzoCrawlerOrchestrator.initCrawlOperation();

            Graph graph = graphFuture.get();

            InputStream graphHTMLSource = MonzoCrawlerApplication.class.getClassLoader().getResourceAsStream("graph.html");
            String graphHTML = IOUtils.toString(graphHTMLSource, Charset.defaultCharset());
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(Files.newOutputStream(Paths.get("output.html")));
            D3Printer.printGraph(graph, graphHTML, bufferedOutputStream);

            monzoCrawlerOrchestrator.shutdown();
            log.info("Done.");
            log.info("Output can be found in output.html");

        } catch (ParseException e) {
            log.debug("Error creating command line parser", e);
        } catch (IllegalArgumentException e) {
            log.error("Number of workers provided is invalid");
        } catch (MalformedURLException e) {
            log.error("URL given is malformed. Please do not include a trailing slash. Example:\nhttp://google.com");
        } catch (InterruptedException | ExecutionException e) {
            log.error("Crawling operation is interrupted");
        } catch (IOException e) {
            log.error("Application error: failed to load graph HTML");
            log.debug("Failed to load graph HTML", e);
        }
    }

    private static int numberOfWorkers(CommandLine cmd) {
        int numberOfWorkers = DEFAULT_NUMBER_OF_CRAWLERS;
        if (cmd.hasOption(WORKERS_OPTION)) {
            int workersInput = Integer.parseInt(cmd.getOptionValue(WORKERS_OPTION));
            if (workersInput <= 0) throw new IllegalArgumentException("Invalid number of workers");
            numberOfWorkers = workersInput;
        }
        return numberOfWorkers;
    }

    private static int idleTime(CommandLine cmd) {
        int idleTime = DEFAULT_IDLE_TIME;
        if (cmd.hasOption(IDLE_TIME_OPTION)) {
            int idleTimeInput = Integer.parseInt(cmd.getOptionValue(IDLE_TIME_OPTION));
            if (idleTimeInput <= 0) throw new IllegalArgumentException("Invalid idle time");
            idleTime = idleTimeInput;
        }
        return idleTime;
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
                .longOpt(WORKERS_OPTION)
                .desc("The number of crawler workers to instantiate")
                .build();
    }

    private static Option helpOption() {
        return Option.builder()
                .longOpt(HELP_OPTION)
                .desc("Print command line options")
                .build();
    }

    private static Option idleTimeOption() {
        return Option.builder()
                .argName("idleTime")
                .hasArg(true)
                .longOpt(IDLE_TIME_OPTION)
                .desc("The amount of time in whole seconds that a crawler would sit idle waiting for tasks")
                .build();
    }

    private static Option addExternalLinksOption() {
        return Option.builder()
                .longOpt(EXTERNAL_LINKS_OPTION)
                .hasArg(false)
                .desc("Adds external links to the output")
                .build();
    }

}
