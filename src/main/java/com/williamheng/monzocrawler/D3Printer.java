package com.williamheng.monzocrawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.williamheng.monzocrawler.model.Graph;
import com.williamheng.monzocrawler.model.Vertex;
import com.williamheng.monzocrawler.model.json.JSONGraph;
import com.williamheng.monzocrawler.model.json.JSONLink;
import com.williamheng.monzocrawler.model.json.JSONNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@Slf4j
public class D3Printer {

    // Example graph JSON
    /**
     * var graph = {
     "nodes": [
     {"id": "/", "group": 1},
     {"id": "/page2", "group": 2},
     {"id": "/page3", "group": 3},
     {"id": "/page4", "group": 4}
     ],
     "links": [
     {"source": "/", "target": "/page2", "value": 1},
     {"source": "/", "target": "/page3", "value": 1},
     {"source": "/page2", "target": "/page3", "value": 1},
     {"source": "/page2", "target": "/page4", "value": 1},
     {"source": "/page3", "target": "/page4", "value": 1}
     ]
     };
     */

    public static void printGraph(Graph graph, String graphHTML, OutputStream outputStream) {

        int groupCount = 1;

        Set<String> urlNodes = new HashSet<>();
        ArrayList<JSONNode> nodes = new ArrayList<>();
        ArrayList<JSONLink> links = new ArrayList<>();

        for (Map.Entry<String, Vertex> localNode : graph.getVertices().entrySet()) {

            String sourceURL = localNode.getKey();
            Vertex vertex = localNode.getValue();
            Iterator<String> vertexDependencies = vertex.getAdjacentVertices().iterator();

            urlNodes.add(sourceURL);

            while (vertexDependencies.hasNext()) {

                String dependencyURL = vertexDependencies.next();
                urlNodes.add(dependencyURL);
                links.add(new JSONLink(sourceURL, dependencyURL, 1));

            }
        }

        Iterator<String> urlNodesIterator = urlNodes.iterator();
        while(urlNodesIterator.hasNext()) {
            String nodeURL = urlNodesIterator.next();
            nodes.add(new JSONNode(nodeURL, groupCount));
            groupCount++;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String graphJSON = objectMapper.writeValueAsString(new JSONGraph(nodes, links));
            String graphOutput = graphHTML.replace("{graph_json}", graphJSON);
            outputStream.write(graphOutput.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (JsonProcessingException e) {
            log.error("Failed to output graph in JSON");
            log.debug("JSON output failure", e);
        } catch (IOException e) {
            log.error("Failed to write output graph in HTML");
            log.debug("HTML output failure", e);
        }
    }



}
