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
package org.sonatype.nexus.repository.storage;

import javax.annotation.Nullable;

import static org.sonatype.nexus.repository.storage.StorageFacet.P_GROUP;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_VERSION;

/**
 * Metadata about a software component.
 *
 * @since 3.0
 */
public class Component
    extends MetadataNode<Component>
{
  private String group;

  private String version;

  /**
   * Gets the group or {@code null} if undefined.
   */
  @Nullable
  public String group() {
    return group;
  }

  /**
   * Gets the group or throws a runtime exception if undefined.
   */
  public String requireGroup() {
    return require(group, P_GROUP);
  }

  /**
   * Sets the group to the given value, or {@code null} to un-define it.
   */
  public Component group(final @Nullable String group) {
    this.group = group;
    return this;
  }

  /**
   * Gets the version or {@code null} if undefined.
   */
  @Nullable
  public String version() {
    return version;
  }

  /**
   * Gets the version or throws a runtime exception if undefined.
   */
  public String requireVersion() {
    return require(version, P_VERSION);
  }

  /**
   * Sets the version to the given value, or {@code null} to un-define it.
   */
  public Component version(final @Nullable String version) {
    this.version = version;
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "metadata=" + getEntityMetadata() +
        ", name=" + name() +
        ", version=" + version() +
        ", group=" + group() +
        '}';
  }

}
