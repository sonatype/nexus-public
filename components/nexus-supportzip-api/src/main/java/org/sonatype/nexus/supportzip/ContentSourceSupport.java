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

import org.sonatype.nexus.supportzip.SupportBundle.ContentSource;

import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link ContentSource} implementations.
 *
 * @since 2.7
 */
public abstract class ContentSourceSupport
    implements ContentSource
{
  public static final String PASSWORD_TOKEN = "****";

  public static final String EMAIL_TOKEN = "user@domain";

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Type type;

  private final String path;

  private Priority priority = Priority.DEFAULT;

  /**
   * @since 3.0
   */
  public ContentSourceSupport(final Type type, final String path, final Priority priority) {
    this.type = checkNotNull(type);
    this.path = checkNotNull(path);
    setPriority(priority);
  }

  public ContentSourceSupport(final Type type, final String path) {
    this(type, path, Priority.DEFAULT);
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getPath() {
    return path;
  }

  public Priority getPriority() {
    return priority;
  }

  public void setPriority(final Priority priority) {
    this.priority = checkNotNull(priority);
  }

  /**
   * Compare by priority order.
   */
  @Override
  public int compareTo(final ContentSource obj) {
    return Ints.compare(priority.order, obj.getPriority().order);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "type=" + type +
        ", path='" + path + '\'' +
        ", priority=" + priority +
        '}';
  }
}