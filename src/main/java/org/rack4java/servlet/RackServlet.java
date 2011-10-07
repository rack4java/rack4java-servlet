package org.rack4java.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rack4java.Rack;
import org.rack4java.RackResponse;
import org.rack4java.utils.ClassHelper;
import org.rack4java.utils.LiteralMap;

@SuppressWarnings("serial") 
public class RackServlet extends HttpServlet {
	private static final Map<String, Object> commonEnvironment = new LiteralMap<String, Object>(
		    Rack.RACK_VERSION, Arrays.asList(0, 2),
		    Rack.RACK_ERRORS, System.err,
		    Rack.RACK_MULTITHREAD, true,
		    Rack.RACK_MULTIPROCESS, true,
		    Rack.RACK_RUN_ONCE, false
		);
	
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
            writeResponse(resp, response);
        } catch (Exception e) {
            RackServlet.throwAsError(e);
        }
    }

    private void writeResponse(HttpServletResponse resp, RackResponse response) throws IOException {
        resp.setStatus(response.getStatus());
        for (String key : response.getHeaders().keySet()) {
            resp.setHeader(key, response.getHeaders().get(key));
        }
        resp.getOutputStream().write(response.getBytes());
    }

    private Map<String, Object> getEnvironment(HttpServletRequest req) throws IOException {
        Map<String, Object> environment = new HashMap<String, Object>();
        environment.putAll(commonEnvironment);
        
        environment.put(Rack.REQUEST_METHOD, req.getMethod());
        environment.put(Rack.PATH_INFO, req.getPathInfo());
        environment.put(Rack.QUERY_STRING, req.getQueryString());
        environment.put(Rack.SERVER_NAME, req.getServerName());
        environment.put(Rack.SERVER_PORT, req.getServerPort());
        environment.put(Rack.SCRIPT_NAME, req.getServletPath());
        
        @SuppressWarnings("unchecked") Enumeration<String> headers = req.getHeaderNames();
        while (headers.hasMoreElements()) {
        	String header = headers.nextElement();
        	environment.put(Rack.HTTP_ + header, req.getHeader(header));
        }
        
        environment.put(Rack.RACK_URL_SCHEME, req.getScheme());
        environment.put(Rack.RACK_INPUT, req.getInputStream());
        
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
