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
package org.sonatype.nexus.proxy.maven.routing;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Punlishing status of a repository.
 *
 * @author cstamas
 * @since 2.4
 */
public class PublishingStatus
{
  /**
   * Status enumeration.
   */
  public static enum PStatus
  {
    /**
     * Prefix list is published okay for given repository.
     */
    PUBLISHED,

    /**
     * Prefix list is not published for given repository.
     */
    NOT_PUBLISHED;
  }

  private final PStatus status;

  private final String lastPublishedMessage;

  private final long lastPublishedTimestamp;

  private final String lastPublishedFilePath;

  /**
   * Constructor.
   */
  public PublishingStatus(final PStatus status, final String lastPublishedMessage,
                          final long lastPublishedTimestamp, final String lastPublishedFilePath)
  {
    this.status = checkNotNull(status);
    this.lastPublishedMessage = checkNotNull(lastPublishedMessage);
    this.lastPublishedTimestamp = lastPublishedTimestamp;
    this.lastPublishedFilePath = lastPublishedFilePath;
  }

  /**
   * Publishing status.
   *
   * @return publishing status.
   */
  public PStatus getStatus() {
    return status;
  }

  /**
   * Returns the publishing message.
   *
   * @return the message.
   */
  public String getLastPublishedMessage() {
    return lastPublishedMessage;
  }

  /**
   * Time stamp (milliseconds) of the last published prefix list, or -1 if not published.
   *
   * @return time stamp (milliseconds) of the last published prefix list, or -1 if not published.
   */
  public long getLastPublishedTimestamp() {
    if (getStatus() == PStatus.PUBLISHED) {
      return lastPublishedTimestamp;
    }
    else {
      return -1;
    }
  }

  /**
   * Repository path of the published prefix list file, or, {@code null} if not published.
   *
   * @return repository path of the published prefix list file, or, {@code null} if not published.
   */
  public String getLastPublishedFilePath() {
    if (getStatus() == PStatus.PUBLISHED) {
      return lastPublishedFilePath;
    }
    else {
      return null;
    }
  }
}
