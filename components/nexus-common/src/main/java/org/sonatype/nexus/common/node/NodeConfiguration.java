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
package org.sonatype.nexus.common.node;

import java.io.Serializable;
import java.util.Objects;

/**
 * Node configuration
 *
 * @since 3.6
 */
public class NodeConfiguration
    implements Serializable
{

  private static final long serialVersionUID = 5687759567911666915L;

  /**
   * UUID identifying cluster node
   */
  private String id;

  /**
   * Admin-specified node identifier
   */
  private String friendlyNodeName;

  public NodeConfiguration() {
    // default constructor
  }

  public NodeConfiguration(final String id, final String friendlyNodeName) {
    this.id = id;
    this.friendlyNodeName = friendlyNodeName;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getFriendlyNodeName() {
    return friendlyNodeName;
  }

  public void setFriendlyNodeName(final String friendlyNodeName) {
    this.friendlyNodeName = friendlyNodeName;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NodeConfiguration other = (NodeConfiguration) obj;
    return Objects.equals(this.getId(), other.getId())
        && Objects.equals(this.getFriendlyNodeName(), other.getFriendlyNodeName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getFriendlyNodeName());
  }
}
