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
import java.util.List;

import org.sonatype.nexus.apachehttpclient.page.Page;
import org.sonatype.nexus.apachehttpclient.page.Page.UnexpectedPageResponse;
import org.sonatype.nexus.proxy.maven.routing.internal.task.CancelableUtil;
import org.sonatype.nexus.proxy.walker.ParentOMatic;
import org.sonatype.nexus.proxy.walker.ParentOMatic.Payload;
import org.sonatype.nexus.util.Node;
import org.sonatype.nexus.util.SystemPropertiesHelper;

import com.google.common.base.Throwables;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Scraper for remote Nexus instances that will scrape only if remote is for sure recognized as Nexus instance, and URL
 * points to a hosted repository.
 * <p>
 * Info: Central scrape takes around 3 minutes, and this class issues over 700 requests. This means about 4 HTTP GET
 * requests per second (1req takes about 250ms) is made to fetch index page. If we add a fix pause of 200ms in between
 * requests, this will "throttle" scraping, it would take around 5 minutes instead of 3 minutes for Central sized
 * repository, that is still acceptable, but would lessen pressure on remote server. Later we can design some more
 * smarter way to control throttling of scrape.
 *
 * @author cstamas
 */
public abstract class AbstractGeneratedIndexPageScraper
    extends AbstractScraper
{
  /**
   * Sleep time in millis that scraping thread will sleep between processing the page response (after processing page
   * response and before making another one following a "deeper" link to be more precise). Goal of this sleep is to
   * "throttle" a bit the scrape speed, to not suffocate remote server by index page generations and/or prevent Nexus
   * to be seen as DoS attacker. This throttling sleep time is 200ms by default. Modifying it is possible using
   * System
   * properties using key {@code org.sonatype.nexus.proxy.maven.routing.internal.scrape.Scraper.pageSleepTimeMillis}.
   * An example of setting sleep time to 500 ms:
   *
   * <pre>
   * org.sonatype.nexus.proxy.maven.routing.internal.scrape.Scraper.pageSleepTimeMillis = 500
   * </pre>
   */
  private long pageSleepTimeMillis = SystemPropertiesHelper.getLong(
      Scraper.class.getName() + ".pageSleepTimeMillis", 200);

  protected AbstractGeneratedIndexPageScraper(final int priority, final String id) {
    super(priority, id);
  }

  protected abstract String getTargetedServer();

  @Override
  protected RemoteDetectionResult detectRemoteRepository(final ScrapeContext context, final Page page) {
    // cheap checks first, to quickly eliminate target without doing any remote requests
    if (page.getHttpResponse().getStatusLine().getStatusCode() == 200) {
      final Elements elements = page.getDocument().getElementsByTag("a");
      if (!elements.isEmpty()) {
        // get "template" parent link
        final Element templateParentLink = getParentDirectoryElement(page);
        // get the page parent link (note: usually it's 1st elem, but HTTPD for example has extra links for
        // column
        // sorting
        for (Element element : elements) {
          // if text is same and abs URLs points to same place, we got it
          if (templateParentLink.text().equals(element.text())
              && templateParentLink.absUrl("href").equals(element.absUrl("href"))) {
            return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_BE_SCRAPED,
                getTargetedServer(), "Remote is a generated index page of " + getTargetedServer());
          }
        }
      }
    }

    // um, we were not totally positive, this might be some web server with index page similar to Nexus one
    return new RemoteDetectionResult(RemoteDetectionOutcome.UNRECOGNIZED, getTargetedServer(),
        "Remote is not a generated index page of " + getTargetedServer());
  }

  @Override
  protected List<String> diveIn(final ScrapeContext context, final Page page)
      throws IOException
  {
    // we use the great and all-mighty ParentOMatic
    final ParentOMatic parentOMatic = new ParentOMatic();
    diveIn(context, page, 0, parentOMatic, parentOMatic.getRoot());
    // Special case: scraped with 0 entry, we consider this as an error
    // Remote repo empty? Why are you proxying it? Or worse, some scrape
    // exotic index page and we end up with 0 entries by mistake?
    if (parentOMatic.getRoot().isLeaf()) {
      context.stop("Remote recognized as " + getTargetedServer()
          + ", but scraped 0 entries. This is considered a failure.");
      return null;
    }
    final List<String> entries = parentOMatic.getAllLeafPaths();
    return entries;
  }

  protected void diveIn(final ScrapeContext context, final Page page, final int currentDepth,
                        final ParentOMatic parentOMatic, final Node<Payload> currentNode)
      throws IOException
  {
    // entry protection
    if (currentDepth >= context.getScrapeDepth()) {
      return;
    }
    // cancelation
    CancelableUtil.checkInterruption();
    log.debug("Processing page response from URL {} for repository {}", page.getUrl(), context.getProxyRepository());
    final Elements elements = page.getDocument().getElementsByTag("a");
    final List<String> pathElements = currentNode.getPathElements();
    final String currentPath = currentNode.getPath();
    for (Element element : elements) {
      if (isDeeperRepoLink(context, pathElements, element)) {
        if (element.text().startsWith(".")) {
          // skip hidden paths
          continue;
        }
        final Node<Payload> newSibling = parentOMatic.addPath(currentPath + "/" + element.text());
        if (element.absUrl("href").endsWith("/")) {
          // "cut" recursion preemptively to save remote fetch (and then stop recursion due to depth)
          final int siblingDepth = currentDepth + 1;
          if (siblingDepth < context.getScrapeDepth()) {
            maySleepBeforeSubsequentFetch();
            final String newSiblingEncodedUrl =
                getRemoteUrlForRepositoryPath(context, newSibling.getPathElements()) + "/";
            final Page siblingPage = Page.getPageFor(context, newSiblingEncodedUrl);
            if (siblingPage.getHttpResponse().getStatusLine().getStatusCode() == 200) {
              diveIn(context, siblingPage, siblingDepth, parentOMatic, newSibling);
            }
            else {
              // we do expect strictly 200 here
              throw new UnexpectedPageResponse(page.getUrl(), page.getHttpResponse().getStatusLine());
            }
          }
        }
      }
    }
  }

  protected void maySleepBeforeSubsequentFetch() {
    if (pageSleepTimeMillis > 0) {
      try {
        Thread.sleep(pageSleepTimeMillis);
      }
      catch (InterruptedException e) {
        Throwables.propagate(e);
      }
    }
  }

  protected boolean isDeeperRepoLink(final ScrapeContext context, final List<String> pathElements, final Element aTag) {
    // HTTPD and some others have anchors for sorting, their rel URL start with "?"
    if (aTag.attr("href").startsWith("?")) {
      return false;
    }
    final String linkAbsoluteUrl = aTag.absUrl("href");
    final String currentUrl = getRemoteUrlForRepositoryPath(context, pathElements);
    return linkAbsoluteUrl.startsWith(currentUrl);
  }

  protected abstract Element getParentDirectoryElement(final Page page);
}
