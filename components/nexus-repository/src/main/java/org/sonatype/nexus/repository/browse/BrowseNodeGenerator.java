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
package org.sonatype.nexus.repository.browse;

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

/**
 * Defines the browse node layout for components and assets of the same format.
 *
 * @since 3.6
 */
public interface BrowseNodeGenerator
{
  List<BrowsePaths> computeAssetPaths(Asset asset, @Nullable Component component);

  List<BrowsePaths> computeComponentPaths(Asset asset, Component component);

  /**
   * @return last segment of the given path string
   *
   * @since 3.7
   */
  default String lastSegment(final String path) {
    int lastNonSlash = path.length() - 1;
    while (lastNonSlash >= 0 && path.charAt(lastNonSlash) == '/') {
      lastNonSlash--;
    }
    int precedingSlash = path.lastIndexOf('/', lastNonSlash - 1);
    return path.substring(precedingSlash + 1, lastNonSlash + 1);
  }
}
