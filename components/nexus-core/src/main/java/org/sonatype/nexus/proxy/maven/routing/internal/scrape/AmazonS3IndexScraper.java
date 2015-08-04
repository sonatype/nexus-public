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
package org.sonatype.nexus.proxy.maven.routing.internal.scrape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.apachehttpclient.page.Page;
import org.sonatype.nexus.proxy.maven.routing.internal.task.CancelableUtil;
import org.sonatype.nexus.util.PathUtils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Scraper for remote AmazonS3 hosted repositories.
 *
 * @author cstamas
 */
@Named(AmazonS3IndexScraper.ID)
@Singleton
public class AmazonS3IndexScraper
    extends AbstractScraper
{
  protected static final String ID = "amazons3-index";

  /**
   * Default constructor.
   */
  public AmazonS3IndexScraper() {
    super(4000, ID); // 4th by popularity
  }

  protected String getTargetedServer() {
    return "Amazon S3";
  }

  @Override
  protected RemoteDetectionResult detectRemoteRepository(final ScrapeContext context, final Page page) {
    // we don't care for response code yes, it might be 200, 403 or 404
    if (page.hasHeaderAndEqualsWith("Server", "AmazonS3") && page.hasHeader("x-amz-request-id")) {
      if (isAccessDeniedResponse(page)) {
        // Code 403, S3 bucket is not publicly accessible, nothing we can do
        return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_NOT_BE_SCRAPED,
            getTargetedServer(), "Bucket is not publicly accessible.");
      }
      else {
        // whatever we have (200 or 404), we know this is S3 and want to try
        return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_BE_SCRAPED,
            getTargetedServer(), "Should be scraped.");
      }
    }
    return new RemoteDetectionResult(RemoteDetectionOutcome.UNRECOGNIZED, getTargetedServer(), "Remote is not "
        + getTargetedServer());
  }

  @Override
  protected List<String> diveIn(final ScrapeContext context, final Page page)
      throws IOException
  {
    String prefix = null;
    Page initialPage = page;
    String initialPageUrl = page.getUrl();
    if (initialPage.getHttpResponse().getStatusLine().getStatusCode() != 200) {
      // we probably have the NoSuchKey response from S3, usually when repo root is not in bucket root
      prefix = getKeyFromNoSuchKeyResponse(initialPage);
      if (prefix == null) {
        log.info("Unexpected S3 response from remote of {}, cannot scrape this: {}", context.getProxyRepository(),
            initialPage.getDocument().outerHtml());
        context.stop("Remote recognized as " + getTargetedServer()
            + ", but unexpected response code and response body received (see logs).");
        return null;
      }
      // repo.remoteUrl does not have query parameters...
      initialPageUrl =
          context.getRemoteRepositoryRootUrl().substring(0,
              context.getRemoteRepositoryRootUrl().length() - prefix.length());
      log.debug("Retrying URL {} to scrape remote of {} on URL {}", initialPageUrl, context.getProxyRepository(),
          context.getRemoteRepositoryRootUrl());
      initialPage = Page.getPageFor(context, initialPageUrl + "?prefix=" + prefix);
    }

    final HashSet<String> entries = new HashSet<String>();
    diveIn(context, initialPage, initialPageUrl, prefix, entries);
    return new ArrayList<String>(entries);
  }

  // ==

  protected void diveIn(final ScrapeContext context, final Page firstPage, final String rootUrl, final String prefix,
                        final Set<String> entries)
      throws IOException
  {
    Page page = firstPage;
    boolean truncated;
    do {
      // check for truncation (isTruncated elem, this means we need to "page" the bucket to get all entries)
      truncated = isTruncated(page);

      // cancelation
      CancelableUtil.checkInterruption();

      // response should be 200 OK, if not, give up
      if (page.getHttpResponse().getStatusLine().getStatusCode() != 200) {
        context.stop("Remote recognized as " + getTargetedServer()
            + ", but cannot be scraped (unexpected response status " + page.getHttpResponse().getStatusLine() + ")");
        return;
      }

      final Elements root = page.getDocument().getElementsByTag("ListBucketResult");
      if (root.size() != 1 || !root.get(0).attr("xmlns").equals("http://s3.amazonaws.com/doc/2006-03-01/")) {
        context.stop("Remote recognized as " + getTargetedServer()
            + ", but unexpected response was received (not \"ListBucketResult\").");
        return;
      }

      log.debug("Processing S3 page response from remote of {} got from URL {}", context.getProxyRepository(), page.getUrl());
      String markerElement = null;
      final Elements elements = page.getDocument().getElementsByTag("Contents");
      for (Element element : elements) {
        final Elements keyElements = element.getElementsByTag("Key");
        if (keyElements.isEmpty()) {
          continue; // skip it
        }
        final Elements sizeElements = element.getElementsByTag("Size");
        if (sizeElements.isEmpty()) {
          continue; // skip it
        }
        final String key = keyElements.get(0).text();
        if (key.startsWith(".") || key.contains("/.")) {
          continue; // skip it, it's a ".dot" file
        }
        final long size = Long.parseLong(sizeElements.get(0).text());
        if (size > 0) {
          markerElement = key;
          // fix key with prefix
          final String fixedKey = prefix != null ? key.substring(prefix.length()) : key;
          // we just need prefix from first few path elements (remote scrape depth)
          final String normalizedPath =
              PathUtils.pathFrom(PathUtils.elementsOf(fixedKey), context.getScrapeDepth());
          entries.add(normalizedPath);
        }
      }

      if (truncated) {
        // cancelation
        CancelableUtil.checkInterruption();

        final ArrayList<String> queryParams = new ArrayList<String>();
        if (prefix != null) {
          queryParams.add("prefix=" + prefix);
        }
        if (markerElement != null) {
          queryParams.add("marker=" + markerElement);
        }
        final String url = appendParameters(rootUrl, queryParams);
        page = Page.getPageFor(context, url);
      }
    }
    while (truncated);
  }

  protected String appendParameters(final String baseUrl, List<String> queryParams) {
    final StringBuilder sb = new StringBuilder(baseUrl);
    boolean first = true;
    for (String queryParam : queryParams) {
      if (first) {
        sb.append("?");
        first = false;
      }
      else {
        sb.append("&");
      }
      sb.append(queryParam);
    }
    return sb.toString();
  }

  protected boolean isAccessDeniedResponse(final Page page) {
    // status code must be 403
    // body must contain <Error>
    // child must contain <Code>AccessDenied</Code>
    // but HTTP response code is actually enough
    return page.getHttpResponse().getStatusLine().getStatusCode() == 403;
  }

  protected String getKeyFromNoSuchKeyResponse(final Page page) {
    // status code must be 404
    // body must contain <Error>
    // child must contain <Code>NoSuchKey</Code>
    // and then extract <Key>
    if (page.getHttpResponse().getStatusLine().getStatusCode() == 404) {
      final Elements errorNodes = page.getDocument().getElementsByTag("Error");
      final Elements codeNodes =
          errorNodes.isEmpty() ? new Elements() : errorNodes.get(0).getElementsByTag("Code");
      final Elements keyNodes =
          errorNodes.isEmpty() ? new Elements() : errorNodes.get(0).getElementsByTag("Key");
      // all the conditions we must test
      if (errorNodes.size() == 1 && codeNodes.size() == 1 && "NoSuchKey".equals(codeNodes.get(0).text())
          && keyNodes.size() == 1) {
        return keyNodes.get(0).text();
      }
    }
    return null;
  }

  protected boolean isTruncated(final Page page) {
    final Elements root = page.getDocument().getElementsByTag("ListBucketResult");
    final Elements truncatedNodes =
        root.isEmpty() ? new Elements() : root.get(0).getElementsByTag("IsTruncated");
    if (root.size() == 1 && truncatedNodes.size() == 1 && "true".equals(truncatedNodes.get(0).text())) {
      return true;
    }
    return false;
  }
}
