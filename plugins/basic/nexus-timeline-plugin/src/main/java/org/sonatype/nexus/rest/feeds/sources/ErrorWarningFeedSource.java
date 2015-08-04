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
package org.sonatype.nexus.rest.feeds.sources;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.log.LogManager;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import org.restlet.data.MediaType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Show Nexus ERROR and WARN log lines from last log file.
 *
 * @author cstamas
 */
@Named(ErrorWarningFeedSource.CHANNEL_KEY)
@Singleton
public class ErrorWarningFeedSource
    extends AbstractFeedSource
{
  public static final String CHANNEL_KEY = "errorWarning";

  private static final List<String> LOGFILENAMES_TO_SCAN = Arrays.asList("nexus.log", "nexus.log.1");

  private final LogManager logManager;

  @Inject
  public ErrorWarningFeedSource(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  public String getFeedKey() {
    return CHANNEL_KEY;
  }

  @Override
  public String getTitle() {
    return "Errors and Warnings";
  }

  @Override
  public String getDescription() {
    return "Errors and Warnings from Nexus log";
  }

  public String getFeedName() {
    return getDescription();
  }

  public SyndFeed getFeed(final Integer from, final Integer count, final Map<String, String> params)
      throws IOException
  {
    final SyndFeedImpl feed = new SyndFeedImpl();
    feed.setTitle(getTitle());
    feed.setDescription(getDescription());
    feed.setAuthor(getNexusAuthor());
    feed.setPublishedDate(new Date());
    final Iterable<String> logFilenamesToScan = getLogFilenamesToScan(params);
    // default same as org.sonatype.nexus.feeds.DefaultFeedRecorder.DEFAULT_PAGE_SIZE
    int remainingCount = count != null ? count : 40;
    final List<SyndEntry> entries = Lists.newArrayList();
    for (String logFileName : logFilenamesToScan) {
      final File logFile = logManager.getLogFile(logFileName);
      if (logFile == null) {
        // not found the file. This is either as nexus.log does not exists
        // or not yet rolled over, so nexus.log.1 not found
        // In any case, we can stop safely, as either way
        // we have nothing to scan. Worst case is that we
        // run as WAR or user completely customized logging configuration.
        break;
      }
      final List<SyndEntry> logFileEntries = extractEntriesFromLogfile(logFile, remainingCount);
      remainingCount -= logFileEntries.size();
      entries.addAll(logFileEntries);
    }
    feed.setEntries(entries);
    return feed;
  }

  // ==

  /**
   * Returns the filenames listed (comma separated list) in {@code fts} feed query parameter, or the default value
   * {@link #LOGFILENAMES_TO_SCAN}.
   *
   * @param params the map containing feed query parameters.
   */
  protected Iterable<String> getLogFilenamesToScan(final Map<String, String> params) {
    // 'fts' is a comma delimited list of filenames (example: "nexus.log,nexus.log.1")
    if (params.containsKey("fts")) {
      final String files = params.get("fts");
      if (!Strings.isNullOrEmpty(files)) {
        return Splitter.on(',').omitEmptyStrings().split(files);
      }
    }
    return LOGFILENAMES_TO_SCAN;
  }

  /**
   * Builds the "author" field from Nexus version.
   */
  protected String getNexusAuthor() {
    return "Nexus " + getApplicationStatusSource().getSystemStatus().getVersion();
  }

  /**
   * Extracts ERROR and WARN log lines from given log file. It returns ordered list (newest 1st, oldest last) of
   * found
   * log lines, and that list is maximized to have {@code entriesToExtract} entries.
   *
   * @param logFile          the log file to scan.
   * @param entriesToExtract The number how much "newest" entries should be collected.
   */
  protected List<SyndEntry> extractEntriesFromLogfile(final File logFile, final int entriesToExtract)
      throws IOException
  {
    final List<SyndEntry> entries = Lists.newArrayList();
    Closer closer = Closer.create();
    try {
      final BufferedReader reader =
          Files.newReader(logFile, Charset.forName("UTF-8"));
      String logLine = reader.readLine();
      while (logLine != null) {
        if (logLine.contains(" WARN ") || logLine.contains(" ERROR ")) {
          final SyndEntry entry = new SyndEntryImpl();
          entry.setPublishedDate(new Date()); // FIXME: item.getEventDate();
          entry.setAuthor(getNexusAuthor());
          entry.setLink("/");

          if (logLine.contains(" ERROR ")) {
            entry.setTitle("Error");
          }
          else if (logLine.contains(" WARN ")) {
            entry.setTitle("Warning");
          }

          final StringBuilder contentValue = new StringBuilder();
          contentValue.append(logLine);

          // FIXME: Grab following stacktrace if any in log
          // if ( StringUtils.isNotEmpty( item.getStackTrace() ) )
          // {
          // // we need <br/> and &nbsp; to display stack trace on RSS
          // String stackTrace = item.getStackTrace().replace(
          // (String) System.getProperties().get( "line.separator" ),
          // "<br/>" );
          // stackTrace = stackTrace.replace( "\t", "&nbsp;&nbsp;&nbsp;&nbsp;" );
          // contentValue.append( "<br/>" ).append( stackTrace );
          // }

          SyndContent content = new SyndContentImpl();
          content.setType(MediaType.TEXT_PLAIN.toString());
          content.setValue(contentValue.toString());
          entry.setDescription(content);

          entries.add(entry);
          if (entries.size() > entriesToExtract) {
            entries.remove(0);
          }
        }
        logLine = reader.readLine();
      }
    }
    catch (Throwable e) {
      throw closer.rethrow(e);
    }
    finally {
      closer.close();
    }
    return Lists.reverse(entries);
  }
}