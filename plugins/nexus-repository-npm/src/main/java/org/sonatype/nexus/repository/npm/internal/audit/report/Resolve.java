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
package org.sonatype.nexus.repository.npm.internal.audit.report;

import java.util.List;
import java.util.Objects;

/**
 * Model will be serialized into Json representation for npm audit report.
 *
 * @since 3.24
 */
public class Resolve
{
  private final int id;

  private final transient List<String> pathList;

  private final String path;

  private final boolean dev;

  private final boolean optional;

  private final boolean bundled;

  public Resolve(
      final int id,
      final List<String> pathList,
      final String path,
      final boolean dev,
      final boolean optional,
      final boolean bundled)
  {
    this.id = id;
    this.pathList = pathList;
    this.path = path;
    this.dev = dev;
    this.optional = optional;
    this.bundled = bundled;
  }

  public int getId() {
    return id;
  }

  public List<String> getPathList() {
    return pathList;
  }

  public String getPath() {
    return path;
  }

  public boolean isDev() {
    return dev;
  }

  public boolean isOptional() {
    return optional;
  }

  public boolean isBundled() {
    return bundled;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Resolve resolve = (Resolve) o;
    return id == resolve.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
