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
package org.sonatype.nexus.content.internal;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.content.csp.ContentSecurityPolicy;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageEOFException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.RemoteStorageTransportOverloadedException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.nexus.web.Constants;
import org.sonatype.nexus.web.ErrorStatusException;
import org.sonatype.nexus.web.ErrorStatusRuntimeException;
import org.sonatype.nexus.web.RemoteIPFinder;
import org.sonatype.nexus.web.WebUtils;
import org.sonatype.nexus.web.internal.ErrorPageFilter;
import org.sonatype.sisu.goodies.common.Throwables2;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.ByteStreams.limit;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.http.HttpHeaders.ACCEPT;

/**
 * Provides access to repositories contents.
 *
 * @since 2.8
 */
@Singleton
@Named
public class ContentServlet
    extends HttpServlet
{
  /**
   * HTTP query parameter to mark request as "describe" request.
   */
  private static final String REQ_QP_DESCRIBE_PARAMETER = "describe";

  /**
   * HTTP query parameter to mark request as "describe" request.
   */
  private static final String REQ_QP_FORCE_PARAMETER = "force";

  /**
   * HTTP query parameter value for {@link #REQ_QP_FORCE_PARAMETER} to force local content. See {@link
   * RequestContext#isRequestLocalOnly()}.
   */
  private static final String REQ_QP_FORCE_LOCAL_VALUE = "local";

  /**
   * HTTP query parameter value for {@link #REQ_QP_FORCE_PARAMETER} to force remote content. See {@link
   * RequestContext#isRequestRemoteOnly()}.
   */
  private static final String REQ_QP_FORCE_REMOTE_VALUE = "remote";

  /**
   * HTTP query parameter value for {@link #REQ_QP_FORCE_PARAMETER} to force expiration of content. See {@link
   * RequestContext#isRequestAsExpired()}.
   */
  private static final String REQ_QP_FORCE_EXPIRED_VALUE = "expired";

  /**
   * A flag setting what should be done if request path retrieval gets a {@link StorageLinkItem} here. If {@code true},
   * this servlet dereference the link (using {@link RepositoryRouter#dereferenceLink(StorageLinkItem)} method), and
   * send proper content to client (ie. client will be "unaware" it actually stepped on link, and content it gets is
   * coming from different path than asked, or even different repository). If {@code false}, a link
   * item will actually send {@link HttpServletResponse#SC_FOUND}, and client might (or might not) follow
   * the redirection to get the linked item.
   */
  private static final boolean DEREFERENCE_LINKS = SystemPropertiesHelper.getBoolean(
      ContentServlet.class.getName() + ".DEREFERENCE_LINKS", true);

  /**
   * Stopwatch that is started when {@link ResourceStoreRequest} is created and stopped when request processing returns
   * from {@link RepositoryRouter}.
   *
   * Keep in sync with describe template, which references the same value by name directly.
   */
  private static final String STOPWATCH_KEY = ContentServlet.class.getName() + ".stopwatch";

  private final Logger logger = LoggerFactory.getLogger(ContentServlet.class);

  private final NexusConfiguration nexusConfiguration;

  private final RepositoryRouter repositoryRouter;

  private final ContentRenderer contentRenderer;

  private final WebUtils webUtils;

  private final ContentSecurityPolicy contentSecurityPolicy;

  @Inject
  public ContentServlet(final NexusConfiguration nexusConfiguration,
                        final RepositoryRouter repositoryRouter,
                        final ContentRenderer contentRenderer,
                        final WebUtils webUtils,
                        final ContentSecurityPolicy contentSecurityPolicy)
  {
    this.nexusConfiguration = checkNotNull(nexusConfiguration);
    this.repositoryRouter = checkNotNull(repositoryRouter);
    this.contentRenderer = checkNotNull(contentRenderer);
    this.webUtils = checkNotNull(webUtils);
    this.contentSecurityPolicy = checkNotNull(contentSecurityPolicy);
    logger.debug("dereferenceLinks={}", DEREFERENCE_LINKS);
  }

  /**
   * Creates a {@link ResourceStoreRequest} out from a {@link HttpServletRequest}.
   */
  protected ResourceStoreRequest getResourceStoreRequest(final HttpServletRequest request) {
    String resourceStorePath = request.getPathInfo();
    if (Strings.isNullOrEmpty(resourceStorePath)) {
      resourceStorePath = "/";
    }
    final ResourceStoreRequest result = new ResourceStoreRequest(resourceStorePath);
    result.getRequestContext().put(STOPWATCH_KEY, new Stopwatch().start());

    // stuff in the user id if we have it in request
    final Subject subject = SecurityUtils.getSubject();
    if (subject != null && subject.getPrincipal() != null) {
      result.getRequestContext().put(AccessManager.REQUEST_USER, subject.getPrincipal().toString());
    }
    result.getRequestContext().put(AccessManager.REQUEST_AGENT, request.getHeader("user-agent"));

    // honor the localOnly, remoteOnly and asExpired (but remoteOnly and asExpired only for non-anon users)
    // as those two actually makes Nexus perform a remote request
    result.setRequestLocalOnly(isLocal(request, resourceStorePath));
    if (!Objects.equals(nexusConfiguration.getAnonymousUsername(),
        result.getRequestContext().get(AccessManager.REQUEST_USER))) {
      result.setRequestRemoteOnly(REQ_QP_FORCE_REMOTE_VALUE.equals(request.getParameter(REQ_QP_FORCE_PARAMETER)));
      result.setRequestAsExpired(REQ_QP_FORCE_EXPIRED_VALUE.equals(request.getParameter(REQ_QP_FORCE_PARAMETER)));
    }
    result.setExternal(true);

    // honor the describe
    if (isDescribeRequest(request)) {
      result.setDescribe(true);
    }

    // honor if-modified-since
    final long ifModifiedSince = request.getDateHeader("if-modified-since");
    if (ifModifiedSince > -1) {
      result.setIfModifiedSince(ifModifiedSince);
    }

    // honor if-none-match
    String ifNoneMatch = request.getHeader("if-none-match");
    if (!Strings.isNullOrEmpty(ifNoneMatch)) {
      // shave off quotes if needed (RFC specifies quotes as must)
      if (ifNoneMatch.startsWith("\"") && ifNoneMatch.endsWith("\"")) {
        ifNoneMatch = ifNoneMatch.substring(1, ifNoneMatch.length() - 1);
      }
      // we have the ETag here, shaved from quotes
      // still, WHAT we have here is basically what client sent (should be what Nx sent once to client, and client
      // cached it), see method doGetFile that will basically handle the if-none-match condition.
      result.setIfNoneMatch(ifNoneMatch);
    }

    // stuff in the originating remote address
    result.getRequestContext().put(AccessManager.REQUEST_REMOTE_ADDRESS, RemoteIPFinder.findIP(request));

    // this is HTTPS, get the cert and stuff it too for later
    if (request.isSecure()) {
      result.getRequestContext().put(AccessManager.REQUEST_CONFIDENTIAL, Boolean.TRUE);
      final Object certArray = request.getAttribute("javax.servlet.request.X509Certificate");
      if (certArray != null) {
        final List<X509Certificate> certs = Arrays.asList((X509Certificate[]) certArray);
        if (!certs.isEmpty()) {
          result.getRequestContext().put(AccessManager.REQUEST_CERTIFICATES, certs);
        }
      }
    }

    // put the accept request-header field
    result.getRequestContext().setRequestAcceptHeader(request.getHeader(ACCEPT));

    // put the incoming URLs
    final StringBuffer sb = request.getRequestURL();
    if (request.getQueryString() != null) {
      sb.append("?").append(request.getQueryString());
    }
    result.setRequestUrl(sb.toString());
    return result;
  }

  /**
   * Request is "local" (should tackle local storage only, not generate any remote request at any cause) if client asks
   * for it, or, a request is made for a collection (path ends with slash).
   */
  protected boolean isLocal(final HttpServletRequest request, final String resourceStorePath) {
    // check do we need local only access
    boolean isLocal = REQ_QP_FORCE_LOCAL_VALUE.equals(request.getParameter(REQ_QP_FORCE_PARAMETER));
    if (!Strings.isNullOrEmpty(resourceStorePath)) {
      // overriding isLocal is we know it will be a collection
      isLocal = isLocal || resourceStorePath.endsWith(RepositoryItemUid.PATH_SEPARATOR);
    }
    return isLocal;
  }

  protected boolean isDescribeRequest(final HttpServletRequest request) {
    return request.getParameterMap().containsKey(REQ_QP_DESCRIBE_PARAMETER);
  }

  /**
   * This method converts various exceptions into {@link ErrorStatusException} preparing those to be shown
   * by {@link ErrorPageFilter}. Still, there are some special case (see access denied handling and IO exception
   * handling) where only a request attribute is set, signaling for security filters that a challenge is needed to
   * elevate permissions.
   */
  private void handleException(final HttpServletRequest request, final Exception exception)
      throws ErrorStatusException, IOException
  {
    logger.trace("Exception", exception);
    int responseCode;

    if (exception instanceof LocalStorageEOFException) {
      // in case client drops connection, this makes not much sense, as he will not
      // receive this response, but we have to end it somehow.
      // but, in case when remote proxy peer drops connection on us regularly
      // this makes sense
      responseCode = SC_NOT_FOUND;
    }
    else if (exception instanceof IllegalArgumentException) {
      responseCode = SC_BAD_REQUEST;
    }
    else if (exception instanceof RemoteStorageTransportOverloadedException) {
      responseCode = SC_SERVICE_UNAVAILABLE;
    }
    else if (exception instanceof RepositoryNotAvailableException) {
      responseCode = SC_SERVICE_UNAVAILABLE;
    }
    else if (exception instanceof IllegalRequestException) {
      responseCode = SC_BAD_REQUEST;
    }
    else if (exception instanceof IllegalOperationException) {
      responseCode = SC_BAD_REQUEST;
    }
    else if (exception instanceof UnsupportedStorageOperationException) {
      responseCode = SC_BAD_REQUEST;
    }
    else if (exception instanceof NoSuchRepositoryException) {
      responseCode = SC_NOT_FOUND;
    }
    else if (exception instanceof NoSuchResourceStoreException) {
      responseCode = SC_NOT_FOUND;
    }
    else if (exception instanceof ItemNotFoundException) {
      responseCode = SC_NOT_FOUND;
    }
    else if (exception instanceof AccessDeniedException) {
      request.setAttribute(Constants.ATTR_KEY_REQUEST_IS_AUTHZ_REJECTED, Boolean.TRUE);
      // Note: we must ensure response is not committed, hence, no error page is rendered
      // this attribute above will cause filter to either 403 if
      // current user is non anonymous, or 401 and challenge if user is anonymous
      return;
    }
    else if (exception instanceof IOException) {
      // log and rethrow IOException, as it is handled in special way, see the ErrorPageFilter
      if (logger.isDebugEnabled()) {
        logger.warn("{} {}", exception.toString(), requestDetails(request), exception);
      }
      else if (logger.isWarnEnabled()) {
        logger.warn("{} {}", Throwables2.explain(exception), requestDetails(request));
      }
      throw (IOException) exception;
    }
    else if (exception instanceof ErrorStatusRuntimeException) {
      ErrorStatusRuntimeException ex = (ErrorStatusRuntimeException) exception;
      throw new ErrorStatusException(ex.getResponseCode(), ex.getReasonPhrase(), ex.getMessage(), ex);
    }
    else {
      responseCode = SC_INTERNAL_SERVER_ERROR;
      if (logger.isWarnEnabled()) {
        logger.warn("{} {}", exception.getMessage(), requestDetails(request), exception);
      }
    }

    throw new ErrorStatusException(responseCode, null, exception.getMessage());
  }

  /**
   * @return basic client request information for logging during exceptions ( NEXUS-6526 )
   */
  private String requestDetails(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder();
    // getRemoteAddr respects x-forwarded-for if enabled and avoids potential DNS lookups
    sb.append("[client=").append(request.getRemoteAddr());
    sb.append(",ua=").append(request.getHeader("User-Agent"));
    sb.append(",req=").append(request.getMethod()).append(' ').append(request.getRequestURL().toString());
    sb.append(']');
    return sb.toString();
  }

  // service
  @Override
  protected void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    response.setHeader("Accept-Ranges", "bytes");
    contentSecurityPolicy.apply(request, response);

    final String method = request.getMethod();
    switch (method) {
      case "GET":
      case "HEAD":
        doGet(request, response);
        break;

      case "PUT":
      case "POST":
        doPut(request, response);
        break;

      case "DELETE":
        doDelete(request, response);
        break;

      case "OPTIONS":
        doOptions(request, response);
        break;

      case "TRACE":
        doTrace(request, response);
        break;

      default:
        throw new ErrorStatusException(SC_METHOD_NOT_ALLOWED, null, "Method not supported: " + method);
    }
  }

  // GET

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    final ResourceStoreRequest rsr = getResourceStoreRequest(request);
    try {
      try {
        StorageItem item = repositoryRouter.retrieveItem(rsr);
        if (item instanceof StorageLinkItem) {
          final StorageLinkItem link = (StorageLinkItem) item;
          if (DEREFERENCE_LINKS) {
            item = dereferenceLink(link);
          }
          else {
            webUtils.sendTemporaryRedirect(response, getLinkTargetUrl(link));
            return;
          }
        }
        ((Stopwatch) rsr.getRequestContext().get(STOPWATCH_KEY)).stop();
        if (isDescribeRequest(request)) {
          doGetDescribe(request, response, rsr, item, null);
        }
        else if (item instanceof StorageFileItem) {
          doGetFile(request, response, (StorageFileItem) item);
        }
        else if (item instanceof StorageCollectionItem) {
          doGetCollection(request, response, (StorageCollectionItem) item);
        }
        else {
          // this should never happen, but still
          throw new ServletException("Item type " + item.getClass() + " unsupported!");
        }
      }
      catch (ItemNotFoundException e) {
        ((Stopwatch) rsr.getRequestContext().get(STOPWATCH_KEY)).stop();
        if (isDescribeRequest(request)) {
          doGetDescribe(request, response, rsr, null, e);
        }
        else {
          throw e;
        }
      }
    }
    catch (Exception e) {
      handleException(request, e);
    }
  }

  /**
   * Dereferences the passed in link completely (following link-to-links too) as long as non-link item is found as
   * target. This method will detect cycles, and will fail if such link constellation is found. If any target during
   * dereference is not found, the usual {@link ItemNotFoundException} will be thrown (by the method used to
   * dereference).
   */
  protected StorageItem dereferenceLink(final StorageLinkItem link) throws Exception {
    final List<String> hops = Lists.newArrayList();
    StorageLinkItem currentLink = link;
    while (true) {
      final String hop = currentLink.getRepositoryItemUid().getKey();
      if (!hops.contains(hop)) {
        hops.add(hop);
        final StorageItem item = repositoryRouter.dereferenceLink(currentLink);
        if (!(item instanceof StorageLinkItem)) {
          return item;
        }
        else {
          currentLink = (StorageLinkItem) item;
        }
      }
      else {
        // cycle detected, current link already processed
        throw new ItemNotFoundException(ItemNotFoundException.reasonFor(link.getResourceStoreRequest(),
            link.getRepositoryItemUid().getRepository(),
            "Link item %s introduced a cycle while referencing it, cycle is %s", link.getRepositoryItemUid(), hops));
      }
    }
  }

  /**
   * Creates absolute URL (as String) of the passed link's target.
   *
   * To be used in "Location" header of the redirect message, for example.
   */
  protected String getLinkTargetUrl(final StorageLinkItem link) {
    final RepositoryItemUid targetUid = link.getTarget();
    return BaseUrlHolder.get() + "/content/repositories/" + targetUid.getRepository().getId() + targetUid.getPath();
  }

  /**
   * Handles a file response, all the conditional request cases, and eventually the content serving of the file item.
   */
  protected void doGetFile(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final StorageFileItem file)
      throws ServletException, IOException
  {
    // ETag, in "shaved" form of {SHA1{e5c244520e897865709c730433f8b0c44ef271f1}} (without quotes)
    // or null if file does not have SHA1 (like Virtual) or generated items (as their SHA1 would correspond to template,
    // not to actual generated content).
    final String etag;
    if (!file.isContentGenerated() && !file.isVirtual()
        && file.getRepositoryItemAttributes().containsKey(StorageFileItem.DIGEST_SHA1_KEY)) {
      etag = "{SHA1{" + file.getRepositoryItemAttributes().get(StorageFileItem.DIGEST_SHA1_KEY) + "}}";
      // tag header ETag: "{SHA1{e5c244520e897865709c730433f8b0c44ef271f1}}", quotes are must by RFC
      response.setHeader("ETag", "\"" + etag + "\"");
    }
    else {
      etag = null;
    }

    // handle conditional GETs only for "static" content, actual content stored, not generated
    if (!file.isContentGenerated() && file.getResourceStoreRequest().getIfModifiedSince() != 0
        && file.getModified() <= file.getResourceStoreRequest().getIfModifiedSince()) {
      // this is a conditional GET using time-stamp
      response.setStatus(SC_NOT_MODIFIED);
      return;
    }
    else if (!file.isContentGenerated() && file.getResourceStoreRequest().getIfNoneMatch() != null && etag != null
        && file.getResourceStoreRequest().getIfNoneMatch().equals(etag)) {
      // this is a conditional GET using ETag
      response.setStatus(SC_NOT_MODIFIED);
      return;
    }

    response.setHeader("Content-Type", file.getMimeType());
    response.setDateHeader("Last-Modified", file.getModified());

    // content-length, if known
    if (file.getLength() != ContentLocator.UNKNOWN_LENGTH) {
      // Note: response.setContentLength Servlet API method uses ints (max 2GB file)!
      // TODO: apparently, some Servlet containers follow serlvet API and assume
      // contents can have 2GB max, so even this workaround below in inherently unsafe.
      // Jetty is checked, and supports this (uses long internally), but unsure for other containers
      response.setHeader("Content-Length", String.valueOf(file.getLength()));
    }

    final List<Range<Long>> ranges = getRequestedRanges(request, file.getLength());

    // pour the content, but only if needed (this method will be called even for HEAD reqs, but with content tossed
    // away), so be conservative as getting input stream involves locking etc, is expensive
    final boolean contentNeeded = "GET".equalsIgnoreCase(request.getMethod());
    if (ranges.isEmpty()) {
      if (contentNeeded) {
        webUtils.sendContent(file.getInputStream(), response);
      }
    }
    else if (ranges.size() > 1) {
      throw new ErrorStatusException(SC_NOT_IMPLEMENTED, "Not Implemented",
          "Multiple ranges not yet supported.");
    }
    else {
      final Range<Long> range = ranges.get(0);
      if (!isRequestedRangeSatisfiable(file, range)) {
        response.setStatus(SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        response.setHeader("Content-Length", "0");
        response.setHeader("Content-Range", "bytes */" + file.getLength());
        return;
      }
      final long bodySize = 1 + range.upperEndpoint() - range.lowerEndpoint();
      response.setStatus(SC_PARTIAL_CONTENT);
      response.setHeader("Content-Length", String.valueOf(bodySize));
      response.setHeader("Content-Range",
          "bytes " + range.lowerEndpoint() + "-" + range.upperEndpoint() + "/" + file.getLength());
      if (contentNeeded) {
        try (final InputStream in = file.getInputStream()) {
          in.skip(range.lowerEndpoint());
          webUtils.sendContent(limit(in, bodySize), response);
        }
      }
    }
  }

  /**
   * Handles collection response, either redirects (to same URL but appended with slash, if request does not end with
   * slash), or renders the "index page" out of collection entries.
   */
  protected void doGetCollection(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final StorageCollectionItem coll)
      throws Exception
  {
    if (!coll.getResourceStoreRequest().getRequestUrl().endsWith("/")) {
      response.setStatus(SC_FOUND);
      response.addHeader("Location", coll.getResourceStoreRequest().getRequestUrl() + "/");
      return;
    }
    // last-modified
    response.setDateHeader("Last-Modified", coll.getModified());
    if ("HEAD".equalsIgnoreCase(request.getMethod())) {
      // do not perform coll.list(), very expensive, just give what we already know
      return;
    }
    // send no cache headers, as any of these responses should not be cached, ever
    webUtils.addNoCacheResponseHeaders(response);
    // perform fairly expensive operation of fetching children from Nx
    final Collection<StorageItem> children = coll.list();
    // render the page
    contentRenderer.renderCollection(request, response, coll, children);
  }

  /**
   * Describe response, giving out meta-information about request, found item (if any) and so on.
   */
  protected void doGetDescribe(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final ResourceStoreRequest rsr,
                               final StorageItem item,
                               final Exception e)
      throws IOException
  {
    // send no cache headers, as any of these responses should not be cached, ever
    webUtils.addNoCacheResponseHeaders(response);
    contentRenderer.renderRequestDescription(request, response, rsr, item, e);
  }

  // PUT

  @Override
  protected void doPut(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    final ResourceStoreRequest rsr = getResourceStoreRequest(request);
    try {
      repositoryRouter.storeItem(rsr, request.getInputStream(), null);
      ((Stopwatch) rsr.getRequestContext().get(STOPWATCH_KEY)).stop();
      response.setStatus(SC_CREATED);
    }
    catch (Exception e) {
      ((Stopwatch) rsr.getRequestContext().get(STOPWATCH_KEY)).stop();
      handleException(request, e);
    }
  }

  // DELETE

  @Override
  protected void doDelete(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    final ResourceStoreRequest rsr = getResourceStoreRequest(request);
    try {
      repositoryRouter.deleteItem(rsr);
      response.setStatus(SC_NO_CONTENT);
      ((Stopwatch) rsr.getRequestContext().get(STOPWATCH_KEY)).stop();
    }
    catch (Exception e) {
      ((Stopwatch) rsr.getRequestContext().get(STOPWATCH_KEY)).stop();
      handleException(request, e);
    }
  }

  // ==

  /**
   * Parses the "Range" header of the HTTP request and builds up a list of {@link Range}. If no range header found, or
   * any problem occurred during parsing it (ie. is malformed), empty collection is returned.
   *
   * @return list of {@link Range}, never {@code null}.
   */
  protected List<Range<Long>> getRequestedRanges(final HttpServletRequest request, final long contentLength) {
    // TODO: Current limitation: only one Range of bytes supported in forms of "-X", "X-Y" (where X<Y) and "X-".
    final String rangeHeader = request.getHeader("Range");
    if (!Strings.isNullOrEmpty(rangeHeader)) {
      try {
        if (rangeHeader.startsWith("bytes=") && rangeHeader.length() > 6 && !rangeHeader.contains(",")) {
          // Range: bytes=500-999 (from 500th byte to 999th)
          // Range: bytes=500- (from 500th byte to the end)
          // Range: bytes=-999 (from 0th byte to the 999th byte, not by RFC but widely supported)
          final String rangeValue = rangeHeader.substring(6, rangeHeader.length());
          if (rangeValue.startsWith("-")) {
            return Collections.singletonList(Range.closed(0L, Long.parseLong(rangeValue.substring(1))));
          }
          else if (rangeValue.endsWith("-")) {
            return Collections.singletonList(Range.closed(
                Long.parseLong(rangeValue.substring(0, rangeValue.length() - 1)), contentLength - 1));
          }
          else if (rangeValue.contains("-")) {
            final String[] parts = rangeValue.split("-");
            return Collections.singletonList(Range.closed(Long.parseLong(parts[0]), Long.parseLong(parts[1])));
          }
          else {
            logger.info("Malformed HTTP Range value: {}, ignoring it", rangeHeader);
          }
        }
        else {
          logger.info(
              "Nexus does not support non-byte or multiple HTTP Ranges, sending complete content: Range value {}",
              rangeHeader);
        }
      }
      catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.info("Problem parsing Range value: {}, ignoring it", rangeHeader, e);
        }
        else {
          logger.info("Problem parsing Range value: {}, ignoring it", rangeHeader);
        }
      }
    }
    return Collections.emptyList();
  }

  /**
   * Returns {@code true} if the {@link Range} is applicable to file (file full closed range encloses passed in range).
   */
  protected boolean isRequestedRangeSatisfiable(final StorageFileItem file, final Range<Long> range) {
    return Range.closed(0L, file.getLength() - 1).encloses(range);
  }
}
