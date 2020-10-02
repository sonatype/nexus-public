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
 * Utility methods for basic "detection" of artifact kind in M2 repository.
 */
public class M2ArtifactRecognizer
{
  /**
   * Is this item M2 Checksum?
   */
  public static boolean isChecksum(String path) {
    return path.endsWith(".sha1") || path.endsWith(".sha256") || path.endsWith(".sha512") || path.endsWith(".md5");
  }

  /**
   * Is this item M2 POM?
   */
  public static boolean isPom(String path) {
    return path.endsWith(".pom") || path.endsWith(".pom.sha1") || path.endsWith(".pom.md5");
  }

  /**
   * Is this item M2 Snapshot?
   */
  public static boolean isSnapshot(String path) {
    return path.indexOf("SNAPSHOT") != -1;
  }

  /**
   * Is this item M2 metadata?
   */
  public static boolean isMetadata(String path) {
    return path.endsWith("maven-metadata.xml") || path.endsWith("maven-metadata.xml.sha1") ||
        path.endsWith("maven-metadata.xml.sha256") || path.endsWith("maven-metadata.xml.sha512") ||
        path.endsWith("maven-metadata.xml.md5");
  }

  public static boolean isSignature(String path) {
    return path.endsWith(".asc");
  }
}
