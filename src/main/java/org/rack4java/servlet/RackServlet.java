package org.rack4java.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rack4java.Context;
import org.rack4java.Rack;
import org.rack4java.RackResponse;
import org.rack4java.context.FallbackContext;
import org.rack4java.context.MapContext;
import org.rack4java.utils.ClassHelper;

@SuppressWarnings("serial") 
public class RackServlet extends HttpServlet {
	private static final Context<Object> commonEnvironment = new MapContext<Object>()
	    .with(Rack.RACK_VERSION, Arrays.asList(0, 2))
	    .with(Rack.RACK_ERRORS, System.err)
	    .with(Rack.RACK_MULTITHREAD, true)
	    .with(Rack.RACK_MULTIPROCESS, true)
	    .with(Rack.RACK_RUN_ONCE, false);
	
    private Rack rack;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String rackClass = config.getInitParameter("rackClass");

        try {
            rack = ((Rack) ClassHelper.loadClass(rackClass).newInstance());
        } catch (Exception e) {
            throw new ServletException("Cannot load: " + rackClass);
        }
    }

    /**
     * used by servlet container
     */
    public RackServlet() {
    }

    public RackServlet(Rack rack) {
        this.rack = rack;
    }

    private void processCall(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            RackResponse response = rack.call(getEnvironment(req));
            RackResponse.Type type = response.getResponseType();
            // TODO - if type is file, hand it to the server directly, otherwise treat it as a stream
            writeResponse(resp, response);
        } catch (Exception e) {
            RackServlet.throwAsError(e);
        }
    }

    private void writeResponse(HttpServletResponse resp, RackResponse response) throws IOException {
        resp.setStatus(response.getStatus());
        for (Map.Entry<String, String> entry : response.getHeaders()) {
            resp.setHeader(entry.getKey(), entry.getValue());
        }
        resp.getOutputStream().write(response.getBodyAsBytes());
    }

    private Context<Object> getEnvironment(HttpServletRequest req) throws IOException {
    	@SuppressWarnings("unchecked") Context<Object> environment = new FallbackContext<Object>(
    			new MapContext<Object>(),
    			commonEnvironment
    		);
        
        environment.with(Rack.REQUEST_METHOD, req.getMethod());
        environment.with(Rack.PATH_INFO, req.getPathInfo());
        environment.with(Rack.QUERY_STRING, req.getQueryString());
        environment.with(Rack.SERVER_NAME, req.getServerName());
        environment.with(Rack.SERVER_PORT, req.getServerPort());
        environment.with(Rack.SCRIPT_NAME, req.getServletPath());
        
        @SuppressWarnings("unchecked") Enumeration<String> headers = req.getHeaderNames();
        while (headers.hasMoreElements()) {
        	String header = headers.nextElement();
        	environment.with(Rack.HTTP_ + header, req.getHeader(header));
        }
        
        environment.with(Rack.RACK_URL_SCHEME, req.getScheme());
        environment.with(Rack.RACK_INPUT, req.getInputStream());
        
        return environment;
    }


    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        processCall((HttpServletRequest) req, (HttpServletResponse) res);
    }

    public static Error throwAsError(Throwable t) throws Error {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new Error(t);
        }
    }
}
