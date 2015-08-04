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
package org.sonatype.nexus.plugins.p2.repository.internal;

/**
 * Generic attributes describing an P2 artifact.
 *
 * @author msoftch
 */
public class GenericP2Artifact
{

  /**
   * The artifact id.
   */
  private String id;

  /**
   * The artifact version.
   */
  private String version;

  /**
   * The artifact type.
   */
  private P2ArtifactType type;

  /**
   * Initializing constructor.
   *
   * @param id      The artifact id
   * @param version The artifact version
   * @param type    The artifact type
   */
  public GenericP2Artifact(String id, String version, P2ArtifactType type) {
    this.id = id;
    this.version = version;
    this.type = type;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @return the type
   */
  public P2ArtifactType getType() {
    return type;
  }

}