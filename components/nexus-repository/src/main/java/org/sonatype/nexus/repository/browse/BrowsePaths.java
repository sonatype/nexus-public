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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Used to denote a single nodes name and path used for security checks.  As well allow browse paths to be different
 * from request paths, this is used to map a request path to a browse node name.  Suppose for browsing format 'foo'
 * we change all folders named 'bar' to 'foo' for display purposes, i.e.
 *
 * requestPath - org/foo/bar/some.file
 * browsePath - org/foo/foo/some.file
 *
 * so the BrowsePaths objects to define the above path would be like so (browsePath -> requestPath)
 * org -> org/
 * org/foo -> org/foo/
 * org/foo/foo -> org/foo/bar/
 * org/foo/foo/some.file -> org/foo/bar/some.file
 *
 * @since 3.18
 */
public class BrowsePaths
{
  private String browsePath;

  private String requestPath;

  public BrowsePaths(String browsePath, String requestPath) {
    this.browsePath = browsePath;
    this.requestPath = requestPath;
  }

  public String getBrowsePath() {
    return browsePath;
  }

  public String getRequestPath() {
    return requestPath;
  }

  public void setBrowsePath(final String browsePath) {
    this.browsePath = browsePath;
  }

  public void setRequestPath(final String requestPath) {
    this.requestPath = requestPath;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof BrowsePaths)) {
      return false;
    }

    return Objects.equals(browsePath, ((BrowsePaths) obj).getBrowsePath()) && Objects
        .equals(requestPath, ((BrowsePaths) obj).getRequestPath());
  }

  @Override
  public int hashCode() {
    return Objects.hash(browsePath, requestPath);
  }

  public static List<BrowsePaths> fromPaths(List<String> paths, boolean trailingSlash) {
    List<BrowsePaths> results = new ArrayList<>();

    StringBuilder requestPath = new StringBuilder();
    for (int i = 0 ; i < paths.size() ; i++) {
      requestPath.append(paths.get(i));
      if (trailingSlash || i < paths.size() - 1) {
        requestPath.append("/");
      }
      results.add(new BrowsePaths(paths.get(i), requestPath.toString()));
    }

    return results;
  }

  public static void appendPath(List<BrowsePaths> browsePaths, String path) {
    browsePaths.add(new BrowsePaths(path, browsePaths.get(browsePaths.size() - 1).getRequestPath() + path));
  }

  public static void appendPath(List<BrowsePaths> browsePaths, String path, String requestPath) {
    browsePaths.add(new BrowsePaths(path, requestPath));
  }
}
