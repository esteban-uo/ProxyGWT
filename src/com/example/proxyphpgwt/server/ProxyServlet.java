package com.example.proxyphpgwt.server;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.StatusLine;

public class ProxyServlet extends HttpServlet{
	private static final String targetServer = "http://localhost/ajax/json.php";
	
	@Override
	@SuppressWarnings("unchecked")
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Create new client to perform the proxied request
		HttpClient httpclient = new DefaultHttpClient();
		
		// Determine final URL
		StringBuffer uri = new StringBuffer();
		uri.append(targetServer);
		uri.append(req.getRequestURI());
		
		// Add any supplied query strings
		String queryString = req.getQueryString();
		if (queryString != null){
			uri.append("?" + queryString);
		}
		
		// Get HTTP method
		final String method = req.getMethod();
		// Create new HTTP request container
		HttpRequestBase request = null;
		
		// Get content length
		int contentLength = req.getContentLength();
		// Unknown content length ...
// 		if (contentLength == -1)
// 			throw new ServletException("Cannot handle unknown content length");
		// If we don't have an entity body, things are quite simple
		if (contentLength < 1) {
			request = new HttpRequestBase() {
				public String getMethod() {
					return method;
				}
			};
		}
		else {
			// Prepare request
			HttpEntityEnclosingRequestBase tmpRequest = new HttpEntityEnclosingRequestBase() {
				public String getMethod() {
					return method;
				}
			};
			
			// Transfer entity body from the received request to the new request
			InputStreamEntity entity = new InputStreamEntity(req.getInputStream(), contentLength);
			tmpRequest.setEntity(entity);
			
			request = tmpRequest;
		}
		
		// Set URI
		try {
			request.setURI(new URI(uri.toString()));
		}
		catch (URISyntaxException e) {
			throw new ServletException("URISyntaxException: " + e.getMessage());
		}
		
		// Copy headers from old request to new request
		// @todo not sure how this handles multiple headers with the same name
		Enumeration<String> headers = req.getHeaderNames();
		while (headers.hasMoreElements()) {
			String headerName = headers.nextElement();
			String headerValue = req.getHeader(headerName);
			// Skip Content-Length and Host
			String lowerHeader = headerName.toLowerCase();
			if (lowerHeader.equals("content-length") == false && lowerHeader.equals("host") == false) {
// 				System.out.println(headerName.toLowerCase() + ": " + headerValue);
				request.addHeader(headerName, headerValue);
			}
		}
		
		// Execute the request
		HttpResponse response = httpclient.execute(request);
		
		// Transfer status code to the response
		StatusLine status = response.getStatusLine();
		resp.setStatus(status.getStatusCode());
		// resp.setStatus(status.getStatusCode(), status.getReasonPhrase()); // This seems to be deprecated. Yes status message is "ambigous", but I don't approve
		
		// Transfer headers to the response
		Header[] responseHeaders = response.getAllHeaders();
		for (int i=0 ; i<responseHeaders.length ; i++) {
			Header header = responseHeaders[i];
			resp.addHeader(header.getName(), header.getValue());
		}
		
		// Transfer proxy response entity to the servlet response
		HttpEntity entity = response.getEntity();
		InputStream input = entity.getContent();
		OutputStream output = resp.getOutputStream();
		int b = input.read();
		while (b != -1) {
			output.write(b);
			System.out.println(b);
			b = input.read();
		}
		
		// Clean up
		input.close();
		output.close();
		httpclient.getConnectionManager().shutdown();
	}
}
