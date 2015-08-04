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
package org.sonatype.nexus.proxy.maven.gav;

/**
 * Utility methods for basic "detection" of artifact kind in M1 repository.
 *
 * @deprecated To be removed once Maven1 support is removed.
 */
@Deprecated
public class M1ArtifactRecognizer
{
  /**
   * Is this item M1 Checksum?
   */
  public static boolean isChecksum(String path) {
    return path.endsWith(".sha1") || path.endsWith(".md5");
  }

  /**
   * Is this item M1 POM?
   */
  public static boolean isPom(String path) {
    return path.endsWith(".pom") || path.endsWith(".pom.sha1") || path.endsWith(".pom.md5");
  }

  /**
   * Is this item M1 snapshot?
   */
  public static boolean isSnapshot(String path) {
    return path.indexOf("SNAPSHOT") != -1;
  }

  /**
   * Is this item M1 metadata? There is no such!
   */
  public static boolean isMetadata(String path) {
    return path.endsWith("maven-metadata.xml") || path.endsWith("maven-metadata.xml.sha1")
        || path.endsWith("maven-metadata.xml.md5");
  }

}
