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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.apachehttpclient.page.Page;
import org.sonatype.nexus.proxy.maven.routing.internal.AbstractPrioritized;
import org.sonatype.nexus.proxy.maven.routing.internal.ArrayListPrefixSource;
import org.sonatype.nexus.util.PathUtils;

import com.google.common.base.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract class for {@link Scraper} implementations.
 *
 * @author cstamas
 */
public abstract class AbstractScraper
    extends AbstractPrioritized
    implements Scraper
{
  /**
   * Detection outcome, drives subsequent action after detecting what remote is.
   */
  public static enum RemoteDetectionOutcome
  {
    /**
     * Remote not recognized, this scraper cannot do anything with it.
     */
    UNRECOGNIZED,

    /**
     * Recognized and we are sure it can and should be scraped.
     */
    RECOGNIZED_SHOULD_BE_SCRAPED,

    /**
     * Recognized and we are sure it should not be scraped.
     */
    RECOGNIZED_SHOULD_NOT_BE_SCRAPED;
  }

  /**
   * Result, carrying outcome, remote server it was tested for and a message.
   *
   * @author cstamas
   */
  public static class RemoteDetectionResult
  {
    private final RemoteDetectionOutcome remoteDetectionOutcome;

    private final String remoteDetectedServer;

    private final String message;

    /**
     * Constructor.
     */
    public RemoteDetectionResult(final RemoteDetectionOutcome remoteDetectionOutcome,
                                 final String remoteDetectedServer, final String message)
    {
      this.remoteDetectionOutcome = checkNotNull(remoteDetectionOutcome);
      this.remoteDetectedServer = checkNotNull(remoteDetectedServer);
      this.message = checkNotNull(message);
    }

    /**
     * The outcome of detection.
     *
     * @return outcome.
     */
    public RemoteDetectionOutcome getRemoteDetectionOutcome() {
      return remoteDetectionOutcome;
    }

    /**
     * Server name detection was done against.
     *
     * @return server name.
     */
    public String getRemoteDetectedServer() {
      return remoteDetectedServer;
    }

    /**
     * Message from detection.
     *
     * @return message.
     */
    public String getMessage() {
      return message;
    }
  }

  private final String id;

  protected AbstractScraper(final int priority, final String id) {
    super(priority);
    this.id = checkNotNull(id);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void scrape(final ScrapeContext context, final Page page) {
    final RemoteDetectionResult detectionResult = detectRemoteRepository(context, page);
    switch (detectionResult.getRemoteDetectionOutcome()) {
      case RECOGNIZED_SHOULD_BE_SCRAPED:
        log.debug("Remote repository of {} on URL={} recognized as {}, scraping it...",
            context.getProxyRepository(), context.getRemoteRepositoryRootUrl(), detectionResult.getRemoteDetectedServer());
        try {
          final List<String> entries = diveIn(context, page);
          if (!context.isStopped()) {
            if (entries != null) {
              // recognized and scraped with result
              final ArrayListPrefixSource prefixSource = new ArrayListPrefixSource(entries);
              context.stop(prefixSource,
                  "Remote recognized as " + detectionResult.getRemoteDetectedServer() + " (harvested "
                      + String.valueOf(entries.size()) + " entries, " + context.getScrapeDepth()
                      + " levels deep).");
            }
            else {
              // safety net (subclass should stop it here: recognized but something went wrong)
              context.stop("Remote recognized as " + detectionResult.getRemoteDetectedServer()
                  + ", but could not scrape it (see logs).");
            }
          }
        }
        catch (IOException e) {
          // remote recognized, but IOEx happened during "dive": stop it and report scrape as unsuccessful
          log.debug(
              "Remote repository of {} recognized as {}, but scrape failed: {}",
                  context.getProxyRepository(), detectionResult.getRemoteDetectedServer(), e.toString());
          context.stop("Remote recognized as " + detectionResult.getRemoteDetectedServer()
              + ", but scrape failed:" + e.getMessage());
        }
        break;

      case RECOGNIZED_SHOULD_NOT_BE_SCRAPED:
        log.debug("Remote repository of {} on URL={} recognized as {}, but not scraping it: {}",
            context.getProxyRepository(), context.getRemoteRepositoryRootUrl(), detectionResult.getRemoteDetectedServer(),
            detectionResult.getMessage());
        context.stop("Remote recognized as " + detectionResult.getRemoteDetectedServer()
            + ", but not scraping it: " + detectionResult.getMessage());
        break;

      default:
        // not recognized, just continue with next Scraper
        log.debug("Remote repository of {} on URL={} not recognized as {}, skipping it.",
            context.getProxyRepository(), context.getRemoteRepositoryRootUrl(), detectionResult.getRemoteDetectedServer());
        break;
    }
  }

  // ==

  protected abstract RemoteDetectionResult detectRemoteRepository(final ScrapeContext context, final Page page);

  protected abstract List<String> diveIn(final ScrapeContext context, final Page page)
      throws IOException;

  // ==

  protected String getRemoteUrlForRepositoryPath(final ScrapeContext context, final List<String> pathElements) {
    // explanation: Nexus "repository paths" are always absolute, using "/" as separators and starting with "/"
    // but, the repo remote URL comes from Nexus config, and Nexus always "normalizes" the URL and it always ends
    // with "/"
    String sp = PathUtils.pathFrom(pathElements, URLENCODE);
    while (sp.startsWith("/")) {
      sp = sp.substring(1);
    }
    return context.getRemoteRepositoryRootUrl() + sp;
  }

  // ==

  private static final UrlEncode URLENCODE = new UrlEncode();

  private static final class UrlEncode
      implements Function<String, String>
  {
    @Override
    public String apply(@Nullable String input) {
      try {
        // See
        // http://en.wikipedia.org/wiki/Percent-encoding
        return URLEncoder.encode(input, "UTF-8").replace("+", "%20");
      }
      catch (UnsupportedEncodingException e) {
        // Platform not supporting UTF-8? Unlikely.
        throw new IllegalStateException("WAT?", e);
      }
    }
  }
}
