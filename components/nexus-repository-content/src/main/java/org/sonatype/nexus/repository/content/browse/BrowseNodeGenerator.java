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
package org.sonatype.nexus.repository.content.browse;

import java.util.List;

import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;

import static org.sonatype.nexus.repository.browse.node.BrowsePath.SLASH_CHAR;

/**
 * Defines the browse node layout for assets and their components of the same format.
 *
 * @since 3.24
 */
public interface BrowseNodeGenerator
{
  /**
   * Computes the paths leading to the asset in the browse tree.
   */
  List<BrowsePath> computeAssetPaths(Asset asset);

  /**
   * Computes the paths leading to the asset's component in the browse tree.
   */
  List<BrowsePath> computeComponentPaths(Asset asset);

  /**
   * Finds the last segment of the given path.
   */
  default String lastSegment(final String path) {
    int lastNonSlash = path.length() - 1;
    while (lastNonSlash >= 0 && path.charAt(lastNonSlash) == SLASH_CHAR) {
      lastNonSlash--;
    }
    int precedingSlash = path.lastIndexOf(SLASH_CHAR, lastNonSlash - 1);
    return path.substring(precedingSlash + 1, lastNonSlash + 1);
  }
}
