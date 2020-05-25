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

import java.util.Objects;

/**
 * Used to denote a single nodes name and path used for security checks.  As well allow browse paths to be different
 * from request paths, this is used to map a request path to a browse node name.  Suppose for browsing format 'foo'
 * we change all folders named 'bar' to 'foo' for display purposes, i.e.
 *
 * requestPath - org/foo/bar/some.file
 * browsePath - org/foo/foo/some.file
 *
 * so the BrowsePath objects to define the above path would be like so (browsePath -> requestPath)
 * org -> org/
 * org/foo -> org/foo/
 * org/foo/foo -> org/foo/bar/
 * org/foo/foo/some.file -> org/foo/bar/some.file
 *
 * @since 3.18
 */
public class BrowsePath
{
  private String browsePath; // NOSONAR

  private String requestPath;

  public BrowsePath(String browsePath, String requestPath) {
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
    if (!(obj instanceof BrowsePath)) {
      return false;
    }

    return Objects.equals(browsePath, ((BrowsePath) obj).getBrowsePath()) && Objects
        .equals(requestPath, ((BrowsePath) obj).getRequestPath());
  }

  @Override
  public int hashCode() {
    return Objects.hash(browsePath, requestPath);
  }
}
