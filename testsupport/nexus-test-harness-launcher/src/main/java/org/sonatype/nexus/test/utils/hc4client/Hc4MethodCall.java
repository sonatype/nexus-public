/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.test.utils.hc4client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;

import com.noelios.restlet.http.HttpClientCall;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.util.Series;

public class Hc4MethodCall
    extends HttpClientCall
{
  private volatile Hc4ClientHelper clientHelper;

  private volatile HttpRequestBase httpMethod;

  private volatile HttpResponse httpResponse;

  /**
   * Indicates if the response headers were added.
   */
  private volatile boolean responseHeadersAdded;

  /**
   * Constructor.
   *
   * @param helper     The parent HTTP client helper.
   * @param method     The method name.
   * @param requestUri The request URI.
   * @param hasEntity  Indicates if the call will have an entity to send to the server.
   */
  public Hc4MethodCall(Hc4ClientHelper helper, final String method,
                       String requestUri, boolean hasEntity)
      throws IOException
  {
    super(helper, method, requestUri);
    this.clientHelper = helper;

    if (requestUri.startsWith("http")) {
      if (method.equalsIgnoreCase(Method.GET.getName())) {
        this.httpMethod = new HttpGet(requestUri);
      }
      else if (method.equalsIgnoreCase(Method.POST.getName())) {
        this.httpMethod = new HttpPost(requestUri);
      }
      else if (method.equalsIgnoreCase(Method.PUT.getName())) {
        this.httpMethod = new HttpPut(requestUri);
      }
      else if (method.equalsIgnoreCase(Method.HEAD.getName())) {
        this.httpMethod = new HttpHead(requestUri);
      }
      else if (method.equalsIgnoreCase(Method.DELETE.getName())) {
        this.httpMethod = new HttpDelete(requestUri);
      }
      else if (method.equalsIgnoreCase(Method.CONNECT.getName())) {
        // CONNECT unsupported (and is unused by legacy ITs)
        throw new UnsupportedOperationException("Not implemented");
      }
      else if (method.equalsIgnoreCase(Method.OPTIONS.getName())) {
        this.httpMethod = new HttpOptions(requestUri);
      }
      else if (method.equalsIgnoreCase(Method.TRACE.getName())) {
        this.httpMethod = new HttpTrace(requestUri);
      }
      else {
        // custom HTTP verbs unsupported (and is unused by legacy ITs)
        throw new UnsupportedOperationException("Not implemented");
      }

      this.httpMethod.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, this.clientHelper
          .isFollowRedirects());
      // retry handler setting unsupported (legaacy ITs use default HC4 retry handler)
      this.responseHeadersAdded = false;
      setConfidential(this.httpMethod.getURI().getScheme()
          .equalsIgnoreCase(Protocol.HTTPS.getSchemeName()));
    }
    else {
      throw new IllegalArgumentException(
          "Only HTTP or HTTPS resource URIs are allowed here");
    }
  }

  public HttpRequestBase getHttpMethod() {
    return this.httpMethod;
  }

  public HttpResponse getHttpResponse() {
    return this.httpResponse;
  }

  @Override
  public int getStatusCode() {
    if (httpResponse == null) {
      return -1;
    }
    return getHttpResponse().getStatusLine().getStatusCode();
  }

  @Override
  public String getReasonPhrase() {
    if (httpResponse == null) {
      return "none";
    }
    return getHttpResponse().getStatusLine().getReasonPhrase();
  }

  @Override
  public WritableByteChannel getRequestEntityChannel() {
    return null;
  }

  @Override
  public OutputStream getRequestEntityStream() {
    return null;
  }

  @Override
  public OutputStream getRequestHeadStream() {
    return null;
  }

  @Override
  public ReadableByteChannel getResponseEntityChannel(long size) {
    return null;
  }

  @Override
  public InputStream getResponseEntityStream(long size) {
    InputStream result = null;

    try {
      if (getHttpResponse() != null && getHttpResponse().getEntity() != null) {
        final InputStream responseBodyAsStream = getHttpResponse().getEntity().getContent();
        if (responseBodyAsStream != null) {
          result = new FilterInputStream(responseBodyAsStream)
          {
            @Override
            public void close()
                throws IOException
            {
              super.close();
              getHttpMethod().releaseConnection();
            }
          };
        }
      }
    }
    catch (IOException ioe) {
      this.clientHelper.getLogger()
          .log(Level.WARNING, "An error occurred during the communication with the remote HTTP server.", ioe);
    }

    return result;
  }

  @Override
  public Series<Parameter> getResponseHeaders() {
    final Series<Parameter> result = super.getResponseHeaders();

    if (!this.responseHeadersAdded) {
      for (final Header header : getHttpResponse().getAllHeaders()) {
        result.add(header.getName(), header.getValue());
      }

      this.responseHeadersAdded = true;
    }

    return result;
  }

  @Override
  public String getServerAddress() {
    return getHttpMethod().getURI().getHost();
  }

  @Override
  public Status sendRequest(Request request) {
    Status result = null;

    try {
      final Representation entity = request.getEntity();

      // Set the request headers
      for (final Parameter header : getRequestHeaders()) {
        if ("Content-Length".equalsIgnoreCase(header.getName())) {
          // HC4 is picky about duplicate content-length header
          continue;
        }
        getHttpMethod().setHeader(header.getName(),
            header.getValue());
      }

      // For those method that accept enclosing entities, provide it if there is any
      if ((entity != null)
          && (getHttpMethod() instanceof HttpEntityEnclosingRequestBase)) {
        final HttpEntityEnclosingRequestBase eem = (HttpEntityEnclosingRequestBase) getHttpMethod();
        eem.setEntity(new HttpEntity()
        {
          @Override
          public void writeTo(final OutputStream outstream)
              throws IOException
          {
            entity.write(outstream);
          }

          @Override
          public boolean isStreaming() {
            return entity.isTransient();
          }

          @Override
          public boolean isRepeatable() {
            return !entity.isTransient();
          }

          @Override
          public boolean isChunked() {
            return false;
          }

          @Override
          public Header getContentType() {
            final String contentType = (entity.getMediaType() != null) ? entity
                .getMediaType().toString() : null;
            if (contentType != null) {
              return new BasicHeader("Content-Type", contentType);
            }
            else {
              return null;
            }
          }

          @Override
          public long getContentLength() {
            return entity.getSize();
          }

          @Override
          public Header getContentEncoding() {
            return null;
          }

          @Override
          public InputStream getContent()
              throws IOException, IllegalStateException
          {
            return entity.getStream();
          }

          @Override
          @Deprecated
          public void consumeContent()
              throws IOException
          {
            EntityUtils.consume(this);
          }
        });
      }
      httpResponse = this.clientHelper.getHttpClient().execute(getHttpMethod());
      result = new Status(getStatusCode(), null, getReasonPhrase(), null);
      // If there is no response body, immediately release the connection
      if (getHttpResponse().getEntity() == null) {
        getHttpMethod().releaseConnection();
      }
    }
    catch (IOException ioe) {
      this.clientHelper
          .getLogger()
          .log(
              Level.WARNING,
              "An error occurred during the communication with the remote HTTP server.",
              ioe);
      result = new Status(Status.CONNECTOR_ERROR_COMMUNICATION, ioe);

      // Release the connection
      getHttpMethod().releaseConnection();
    }

    return result;
  }
}
