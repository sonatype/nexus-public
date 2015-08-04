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
package org.sonatype.nexus.client.core.subsystem.routing;

/**
 * The routing status for a Maven repository.
 *
 * @author cstamas
 * @since 2.4
 */
public class Status
{
  /**
   * Enum representing the outcome of some (possibly long running) operation.
   */
  public static enum Outcome
  {
    /**
     * Operation resulted with failure.
     */
    FAILED,

    /**
     * Operation has no outcome yet. This means operation still did not run or is still running for the first time.
     */
    UNDECIDED,

    /**
     * Operation resulted with success.
     */
    SUCCEEDED;
  }

  /**
   * The routing discovery status for a Maven Proxy repository.
   */
  public static class DiscoveryStatus
  {
    /**
     * The discovery is enabled or not.
     */
    private final boolean discoveryEnabled;

    /**
     * Update interval in hours.
     */
    private final int discoveryIntervalHours;

    /**
     * The last discovery status, was it successful (1) or not (-1), or still running (0).
     */
    private final Outcome discoveryLastStatus;

    /**
     * The strategy used (once finished).
     */
    private final String discoveryLastStrategy;

    /**
     * The message of discovery (strategy dependant, once finished).
     */
    private final String discoveryLastMessage;

    /**
     * When discovery last run, timestamp (in millis) or -1 if not yet finished.
     */
    private final long discoveryLastRunTimestamp;

    /**
     * Constructor.
     */
    public DiscoveryStatus(final boolean discoveryEnabled, final int discoveryIntervalHours,
                           final Outcome discoveryLastStatus, final String discoveryLastStrategy,
                           final String discoveryLastMessage, final long discoveryLastRunTimestamp)
    {
      this.discoveryEnabled = discoveryEnabled;
      this.discoveryIntervalHours = discoveryIntervalHours;
      this.discoveryLastStatus = discoveryLastStatus;
      this.discoveryLastStrategy = discoveryLastStrategy;
      this.discoveryLastMessage = discoveryLastMessage;
      this.discoveryLastRunTimestamp = discoveryLastRunTimestamp;
    }

    /**
     * Returns enabled state.
     *
     * @return {@code true} if enabled.
     */
    public boolean isDiscoveryEnabled() {
      return discoveryEnabled;
    }

    /**
     * Returns discovery interval in hours.
     *
     * @return discovery interval in hours.
     */
    public int getDiscoveryIntervalHours() {
      return discoveryIntervalHours;
    }

    /**
     * Returns last discovery outcome.
     *
     * @return last discovery outcome.
     */
    public Outcome getDiscoveryLastStatus() {
      return discoveryLastStatus;
    }

    /**
     * Returns last discovery strategy.
     *
     * @return last discovery strategy.
     */
    public String getDiscoveryLastStrategy() {
      return discoveryLastStrategy;
    }

    /**
     * Returns last discovery message.
     *
     * @return last discovery message.
     */
    public String getDiscoveryLastMessage() {
      return discoveryLastMessage;
    }

    /**
     * Returns last discovery run timestamp.
     *
     * @return last discovery run timestamp.
     */
    public long getDiscoveryLastRunTimestamp() {
      return discoveryLastRunTimestamp;
    }
  }

  /**
   * The publishing status, was it published (1), not published (-1) or not processed or still processing (0).
   */
  private final Outcome publishedStatus;

  /**
   * The publishing accompanying message.
   */
  private final String publishedMessage;

  /**
   * The timestamp of last publishing (in millis) or -1 if not published.
   */
  private final long publishedTimestamp;

  /**
   * The URL of the published prefix file or {@code null} if not published.
   */
  private final String publishedUrl;

  /**
   * The discovery status if this status represents a status for Maven Proxy repository, {@code null} otherwise.
   */
  private final DiscoveryStatus discoveryStatus;

  /**
   * Constructor.
   */
  public Status(final Outcome publishedStatus, final String publishedMessage, final long publishedTimestamp,
                final String publishedUrl, final DiscoveryStatus discoveryStatus)
  {
    this.publishedStatus = publishedStatus;
    this.publishedMessage = publishedMessage;
    this.publishedTimestamp = publishedTimestamp;
    this.publishedUrl = publishedUrl;
    this.discoveryStatus = discoveryStatus;
  }

  /**
   * Returns the publish outcome.
   *
   * @return the publish outcome.
   */
  public Outcome getPublishedStatus() {
    return publishedStatus;
  }

  /**
   * Returns the publish message.
   *
   * @return the message.
   */
  public String getPublishedMessage() {
    return publishedMessage;
  }

  /**
   * Returns the timestamp of last publishing.
   *
   * @return the timestamp of last publish or -1 if not published yet.
   */
  public long getPublishedTimestamp() {
    return publishedTimestamp;
  }

  /**
   * Returns the URL of the published prefix file, or {@code null} if not published.
   *
   * @return the prefix file URL if published, or {@code null}.
   */
  public String getPublishedUrl() {
    return publishedUrl;
  }

  /**
   * Returns the {@link DiscoveryStatus} if this status represents a status of a Maven Proxy repository, {@code null}
   * otherwise.
   *
   * @return discovery status if this status belongs to a Maven Proxy repository, {@code null} otherwise.
   */
  public DiscoveryStatus getDiscoveryStatus() {
    return discoveryStatus;
  }
}
