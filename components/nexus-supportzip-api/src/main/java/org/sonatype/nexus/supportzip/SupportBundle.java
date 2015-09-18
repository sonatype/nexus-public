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
package org.sonatype.nexus.supportzip;

import java.io.InputStream;
import java.util.List;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Defines the content sources of a support ZIP file.
 *
 * @since 2.7
 */
public class SupportBundle
{
  /**
   * Source of content for support bundle.
   */
  public static interface ContentSource
      extends Comparable<ContentSource>
  {
    /**
     * Support bundle content source inclusion type.
     */
    enum Type
    {
      SYSINFO,
      THREAD,
      METRICS,
      CONFIG,
      SECURITY,
      LOG,
      JMX
    }

    /**
     * The type of content source.
     */
    Type getType();

    /**
     * The path in the support zip where this content will be saved.
     * Unix-style, must NOT begin or end with '/'
     */
    String getPath();

    /**
     * Support bundle content source inclusion priority.
     */
    enum Priority
    {
      OPTIONAL(999),
      LOW(100),
      DEFAULT(50),
      HIGH(10),
      REQUIRED(0);

      final int order;

      private Priority(final int order) {
        this.order = order;
      }
    }

    /**
     * Priority to determine inclusion order, lower priority sources could get truncated.
     */
    Priority getPriority();

    /**
     * The size of the content in bytes. Valid after {@link #prepare()} has been called.
     */
    long getSize();

    /**
     * Content bytes. Valid after {@link #prepare()} has been called.
     */
    InputStream getContent() throws Exception;

    /**
     * Prepare content.
     */
    void prepare() throws Exception;

    /**
     * Cleanup content.
     */
    void cleanup() throws Exception;
  }

  private final List<ContentSource> sources = Lists.newArrayList();

  /**
   * Returns all configured content sources.
   */
  public List<ContentSource> getSources() {
    return sources;
  }

  /**
   * Add a content source.
   */
  public void add(final ContentSource contentSource) {
    checkNotNull(contentSource);
    sources.add(contentSource);
  }

  /**
   * @see #add(ContentSource)
   */
  public void leftShift(final ContentSource contentSource) {
    add(contentSource);
  }
}