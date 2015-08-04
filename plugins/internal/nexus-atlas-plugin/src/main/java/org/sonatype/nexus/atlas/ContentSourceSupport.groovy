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
package org.sonatype.nexus.atlas

import org.sonatype.nexus.atlas.SupportBundle.ContentSource
import org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority
import org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type
import org.sonatype.sisu.goodies.common.ComponentSupport

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority.DEFAULT

/**
 * Support for {@link ContentSource} implementations.
 *
 * @since 2.7
 */
abstract class ContentSourceSupport
    extends ComponentSupport
    implements ContentSource
{
  public static final String PASSWORD_TOKEN = '****'

  public static final String EMAIL_TOKEN = 'user@domain'

  private final Type type

  private final String path

  Priority priority = DEFAULT

  ContentSourceSupport(final Type type, final String path) {
    this.type = checkNotNull(type)
    this.path = checkNotNull(path)
  }

  @Override
  Type getType() {
    return type
  }

  @Override
  String getPath() {
    return path
  }

  /**
   * Compare by priority order.
   */
  @Override
  int compareTo(final ContentSource obj) {
    priority.order <=> obj.priority.order
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