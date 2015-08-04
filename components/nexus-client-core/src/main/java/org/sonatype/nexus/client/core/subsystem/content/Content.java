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
package org.sonatype.nexus.client.core.subsystem.content;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.google.common.collect.Range;

/**
 * @since 2.1
 */
public interface Content
{
  enum ForceDirective
  {
    LOCAL, REMOTE, EXPIRED;
  }

  boolean exists(Location location)
      throws IOException;

  boolean existsWith(Location location, ForceDirective directive)
      throws IOException;

  void download(Location location, File target)
      throws IOException;

  void downloadWith(Location location, ForceDirective directive, File target)
      throws IOException;

  /**
   * @since 2.4
   */
  void downloadWith(Location location, ForceDirective directive, OutputStream target)
      throws IOException;

  void upload(Location location, File target)
      throws IOException;

  void delete(Location location)
      throws IOException;

  /**
   * Downloads a range of content from given location.
   *
   * @since 2.7.0
   */
  void downloadRange(Location location, File target, Range<Long> range)
      throws IOException;

  /**
   * Downloads a range of content from given location.
   *
   * @since 2.7.0
   */
  void downloadRange(Location location, OutputStream target, Range<Long> range)
      throws IOException;
}
