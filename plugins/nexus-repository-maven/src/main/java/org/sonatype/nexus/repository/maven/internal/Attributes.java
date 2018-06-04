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
package org.sonatype.nexus.repository.maven.internal;

/**
 * Maven format specific attributes.
 *
 * @since 3.0
 */
public final class Attributes
{
  private Attributes() {
    // nop
  }

  /**
   * Artifact groupId, applied to both component and asset.
   */
  public static final String P_GROUP_ID = "groupId";

  /**
   * Artifact artifactId, applied to both component and asset.
   */
  public static final String P_ARTIFACT_ID = "artifactId";

  /**
   * Artifact version, applied to both component and asset.
   */
  public static final String P_VERSION = "version";

  /**
   * Artifact base version, applied to both component and asset.
   */
  public static final String P_BASE_VERSION = "baseVersion";

  /**
   * Artifact classifier, applied to asset only.
   */
  public static final String P_CLASSIFIER = "classifier";

  /**
   * File extension, applied to asset only.
   */
  public static final String P_EXTENSION = "extension";

  /**
   * Pulled out of POM, if exists. Applied to component only. If not present, component POM is not present or not
   * readable/corrupted.
   */
  public static final String P_PACKAGING = "packaging";

  /**
   * Pulled out of POM, if exists. Applied to component only. If not present but {@link #P_PACKAGING} is present, POM
   * might not have it defined.
   */
  public static final String P_POM_NAME = "pom_name";

  /**
   * Pulled out of POM, if exists. Applied to component only. If not present but {@link #P_PACKAGING} is present, POM
   * might not have it defined.
   */
  public static final String P_POM_DESCRIPTION = "pom_description";

  /**
   * Enum for asset kinds regarding repository layout. This enum <strong>should not</strong> contain details like
   * "archetype catalog" or "index files", and should be pretty much complete already.
   */
  public enum AssetKind
  {
    /**
     * Represents an artifact, such as POM, JAR or sources-jar.
     */
    ARTIFACT,

    /**
     * Represents some subordinate for {@link #ARTIFACT}, like hashes (sha1, md5) or signatures are.
     */
    ARTIFACT_SUBORDINATE,

    /**
     * Represents repository metadata.
     */
    REPOSITORY_METADATA,

    /**
     * Represents a full or incremental index for the repository.
     */
    REPOSITORY_INDEX,
    /**
     * Represents some other resource not belonging to Maven2 repository layout.
     */
    OTHER
  }
}
