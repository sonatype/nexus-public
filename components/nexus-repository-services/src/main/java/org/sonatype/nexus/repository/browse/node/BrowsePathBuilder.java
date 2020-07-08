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
package org.sonatype.nexus.repository.browse.node;

import java.util.ArrayList;
import java.util.List;

/**
 * Static methods for building {@link BrowsePath}.
 *
 * @since 3.25.0
 */
public class BrowsePathBuilder
{
  private BrowsePathBuilder() {
    // static utility class
  }

  public static List<BrowsePath> fromPaths(List<String> paths, boolean trailingSlash) {
    List<BrowsePath> results = new ArrayList<>();

    StringBuilder requestPath = new StringBuilder();
    for (int i = 0; i < paths.size(); i++) {
      requestPath.append(paths.get(i));
      if (trailingSlash || i < paths.size() - 1) {
        requestPath.append("/");
      }
      results.add(new BrowsePath(paths.get(i), requestPath.toString()));
    }

    return results;
  }

  public static void appendPath(List<BrowsePath> browsePaths, String path) {
    browsePaths.add(new BrowsePath(path, browsePaths.get(browsePaths.size() - 1).getRequestPath() + path));
  }

  public static void appendPath(List<BrowsePath> browsePaths, String path, String requestPath) {
    browsePaths.add(new BrowsePath(path, requestPath));
  }
}
