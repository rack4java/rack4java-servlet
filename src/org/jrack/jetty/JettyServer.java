package org.jrack.jetty;

import org.jrack.JRack;
import org.jrack.utils.ClassUtilities;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyServer {
    protected static final Logger log = LoggerFactory.getLogger(JettyServer.class);
    public static final String DEFAULT_SERVER_ADDRESS = "0.0.0.0";

    public static void start(String host, int port, JRack rack) {
        startJettyServer(host, port, new RackServlet(rack));
        log.info(String.format("Jetty rack listening on: %s:%d", host, port));
    }

    private static void startJettyServer(String host, int port, RackServlet servlet) {
        try {

            SelectChannelConnector connector = new SelectChannelConnector();
            connector.setHost(host);
            connector.setPort(port);
            connector.setThreadPool(new QueuedThreadPool(20));

            Server server = new Server(port);
            server.setConnectors(new Connector[]{connector});

            ServletHandler handler = new ServletHandler();
            handler.addServletWithMapping(new ServletHolder(servlet), "/*");
            server.setHandler(handler);
            server.start();
            server.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * run a simple Jetty servlet and test with:
     * curl http://localhost:8080/echo -d 'hello'
     */
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    	int port = 8080;
    	String rack = "org.jrack.examples.Echo";
    	for (String arg : args) {
    		if (arg.startsWith("-p")) {
    			port = Integer.parseInt(arg.substring(2));
    		} else if (arg.startsWith("-h")) {
    			System.out.println("usage: java org.jrack.jetty.JettyServer -p8080 org.jrack.examples.Echo");
    		} else {
    			rack = arg;
    		}
    	}
        start(DEFAULT_SERVER_ADDRESS, port, (JRack) ClassUtilities.loadClass(rack).newInstance());
    }
}