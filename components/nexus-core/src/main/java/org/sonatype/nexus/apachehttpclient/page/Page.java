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
package org.sonatype.nexus.apachehttpclient.page;

import java.io.IOException;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class offers "high level" HTTP request and page processing support using JSoup.
 *
 * @author cstamas
 * @since 2.7
 */
public class Page
{
  private final HttpUriRequest httpUriRequest;

  private final HttpResponse httpResponse;

  private final Document document;

  /**
   * Constructor used by static helper methods in this class.
   *
   * @param httpUriRequest the HTTP request for this page.
   * @param httpResponse   the HTTP response for this page (with consumed body!).
   * @param document       the JSoup document for this page or {@code null} if no body.
   */
  private Page(final HttpUriRequest httpUriRequest, final HttpResponse httpResponse, final Document document) {
    this.httpUriRequest = checkNotNull(httpUriRequest);
    this.httpResponse = checkNotNull(httpResponse);
    this.document = document;
  }

  /**
   * The original URL of the Page.
   */
  public String getUrl() {
    return httpUriRequest.getURI().toString();
  }

  /**
   * The response code of the Page.
   */
  public int getStatusCode() {
    return httpResponse.getStatusLine().getStatusCode();
  }

  /**
   * The HTTP response for this page (response body is consumed!). To check stuff like headers.
   */
  public HttpResponse getHttpResponse() {
    return httpResponse;
  }

  /**
   * The body of the page, parsed by JSoup, or {@code null} if server did not sent any body.
   */
  public Document getDocument() {
    return document;
  }

  // ==

  /**
   * Checks if header with given name is present.
   *
   * @return {@code true} if header with given name is present.
   */
  public boolean hasHeader(final String headerName) {
    return getHttpResponse().getFirstHeader(headerName) != null;
  }

  /**
   * Checks if header with given name is present and start with given value.
   *
   * @return {@code true} if header with given name is present and starts with given value.
   */
  public boolean hasHeaderAndStartsWith(final String headerName, final String value) {
    final Header header = getHttpResponse().getFirstHeader(headerName);
    return header != null && header.getValue() != null && header.getValue().startsWith(value);
  }

  /**
   * Checks if header with given name is present and equals with given value.
   *
   * @return {@code true} if header with given name is present and equals with given value.
   */
  public boolean hasHeaderAndEqualsWith(final String headerName, final String value) {
    final Header header = getHttpResponse().getFirstHeader(headerName);
    return header != null && header.getValue() != null && header.getValue().equals(value);
  }

  // ==

  private static final Logger LOG = LoggerFactory.getLogger(Page.class);

  /**
   * Returns a page for given URL using HTTP GET.
   */
  public static Page getPageFor(final PageContext context, final String url)
      throws IOException
  {
    return buildPageFor(context, new HttpGet(url));
  }

  /**
   * Returns a page for given HTTP request.
   */
  public static Page buildPageFor(final PageContext context, final HttpUriRequest httpUriRequest)
      throws IOException
  {
    checkNotNull(context);
    checkNotNull(httpUriRequest);
    // TODO: detect redirects
    LOG.debug("Executing HTTP {} request against {}", httpUriRequest.getMethod(), httpUriRequest.getURI());
    final HttpResponse response = context.executeHttpRequest(httpUriRequest);
    try {
      if (context.isExpectedResponse(response)) {
        if (response.getEntity() != null) {
          return new Page(httpUriRequest, response,
              Jsoup.parse(response.getEntity().getContent(), null, httpUriRequest.getURI().toString()));
        }
        else {
          // no body
          return new Page(httpUriRequest, response, null);
        }
      }
      else {
        throw new UnexpectedPageResponse(httpUriRequest.getURI().toString(), response.getStatusLine());
      }
    }
    finally {
      EntityUtils.consumeQuietly(response.getEntity());
    }
  }

  /**
   * A context of page requests. Carries the client and performs checks. For more specific work should be extended.
   */
  public static class PageContext
  {
    private final HttpClient httpClient;

    public PageContext(final HttpClient httpClient) {
      this.httpClient = checkNotNull(httpClient);
    }

    protected HttpClient getHttpClient() {
      return httpClient;
    }

    /**
     * Creates a {@link HttpContext} for given HTTP request.
     */
    public HttpContext createHttpContext(final HttpUriRequest httpRequest)
        throws IOException
    {
      return new BasicHttpContext();
    }

    /**
     * Executes a {@link HttpUriRequest} on behalf of this context and returns the {@link HttpResponse} of the request.
     */
    public HttpResponse executeHttpRequest(final HttpUriRequest httpRequest)
        throws IOException
    {
      final HttpContext httpContext = createHttpContext(httpRequest);
      return getHttpClient().execute(httpRequest, httpContext);
    }

    /**
     * Returns {@code true} if response is expected. It might check for response status code, presence of some
     * header, etc. Default implementation checks for status code to be between 200 (inclusive) and 500 (exclusive).
     */
    public boolean isExpectedResponse(final HttpResponse response) {
      return response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 499;
    }
  }

  /**
   * A context of page requests made on behalf of a Repository.
   */
  public static class RepositoryPageContext
      extends PageContext
  {
    private final ProxyRepository proxyRepository;

    public RepositoryPageContext(final HttpClient httpClient, final ProxyRepository proxyRepository) {
      super(httpClient);
      this.proxyRepository = checkNotNull(proxyRepository);
    }

    protected ProxyRepository getProxyRepository() {
      return proxyRepository;
    }

    /**
     * Equips context with repository.
     */
    @Override
    public HttpContext createHttpContext(final HttpUriRequest httpRequest)
        throws IOException
    {
      final HttpContext httpContext = super.createHttpContext(httpRequest);
      httpContext.setAttribute(Hc4Provider.HTTP_CTX_KEY_REPOSITORY, getProxyRepository());
      return httpContext;
    }
  }

  /**
   * Exception thrown when unexpected response code is received from page. Used to distinguish between this case and
   * other "real" IO problems like connectivity or transport problems. Not every caller will want to handle this
   * either.
   */
  @SuppressWarnings("serial")
  public static class UnexpectedPageResponse
      extends IOException
  {
    private final String url;

    private final StatusLine statusLine;

    /**
     * Constructor.
     */
    public UnexpectedPageResponse(final String url, final StatusLine statusLine) {
      super("Unexpected response from remote repository URL " + url + " : " + statusLine);
      this.url = url;
      this.statusLine = statusLine;
    }

    /**
     * The full URL that emitted the response.
     *
     * @return the URL
     */
    public String getUrl() {
      return url;
    }

    /**
     * The status line (code and reason phrase) that was unexpected.
     *
     * @return the status line of response.
     */
    public StatusLine getStatusLine() {
      return statusLine;
    }
  }
}
