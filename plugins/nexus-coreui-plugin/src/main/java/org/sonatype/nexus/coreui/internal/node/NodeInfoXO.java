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
package org.sonatype.nexus.coreui.internal.node;

/**
 * For transmitting info about nodes in cluster
 */
public class NodeInfoXO
{
  private String name;

  private Boolean local;

  private String displayName;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Boolean getLocal() {
    return local;
  }

  public void setLocal(final Boolean local) {
    this.local = local;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String toString() {
    return "NodeInfoXO(" +
        "name=" + name +
        ", local=" + local +
        ", displayName=" + displayName +
        ")";
  }
}
