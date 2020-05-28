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
package org.sonatype.nexus.repository.npm.internal.audit.parser;

import java.util.Map;
import java.util.Objects;

/**
 * Node for parsing package-lock.json
 *
 * @since 3.24
 */
public class PackageLockNode
{
  private final String version;

  private final boolean dev;

  private final boolean optional;

  private String parentNodeName;

  private final Map<String, PackageLockNode> dependencies;

  public PackageLockNode(
      final String version,
      final boolean dev,
      final boolean optional,
      final String parentNodeName,
      final Map<String, PackageLockNode> dependencies)
  {
    this.version = version;
    this.dev = dev;
    this.optional = optional;
    this.parentNodeName = parentNodeName;
    this.dependencies = dependencies;
  }

  public Map<String, PackageLockNode> getDependencies() {
    return dependencies;
  }

  public String getVersion() {
    return version;
  }

  public boolean isDev() {
    return dev;
  }

  public boolean isOptional() {
    return optional;
  }

  public String getParentNodeName() {
    return parentNodeName;
  }

  public void setParentNodeName(String parentNodeName) {
    this.parentNodeName = parentNodeName;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PackageLockNode that = (PackageLockNode) o;
    return dev == that.dev &&
        optional == that.optional &&
        Objects.equals(version, that.version) &&
        Objects.equals(parentNodeName, that.parentNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, dev, optional, parentNodeName);
  }
}
