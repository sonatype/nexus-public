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

import junit.framework.TestCase;

public class MavenArtifactRecognizerTest
    extends TestCase
{

  public void testIsPom() {
    assertEquals(true, M2ArtifactRecognizer.isPom("aaa.pom"));
    assertEquals(true, M2ArtifactRecognizer.isPom("zxc-1-2-3.pom"));
    assertEquals(false, M2ArtifactRecognizer.isPom("aaa.jar"));
    assertEquals(false, M2ArtifactRecognizer.isPom("aaa.pom-a"));
  }

  public void testIsSnapshot1() {
    // NEXUS-3148
    assertEquals(true, M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0SNAPSHOT/aid-1.0SNAPSHOT.jar"));

    assertEquals(true, M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0-SNAPSHOT/aid-1.0-SNAPSHOT.jar"));
    assertEquals(true, M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0-SNAPSHOT/aid-1.0-SNAPSHOT.pom"));
    assertEquals(true, M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0-SNAPSHOT/aid-1.2.3-.pom"));
    assertEquals(false, M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0/xsd-SNAPsHOT.jar"));
    assertEquals(false, M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0/xsd-SNAPHOT.pom"));
    assertEquals(false, M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0/a/b/c/xsd-1.2.3NAPSHOT.pom"));
    assertEquals(false, M2ArtifactRecognizer.isSnapshot("/javax/mail/mail/1.4/mail-1.4.jar"));
  }

  public void testIsSnapshot2() {
    assertEquals(
        true,
        M2ArtifactRecognizer.isSnapshot(
            "/org/somewhere/appassembler-maven-plugin/1.0-SNAPSHOT/appassembler-maven-plugin-1.0-20060714.142547-1.pom"));
    assertEquals(
        false,
        M2ArtifactRecognizer.isSnapshot(
            "/org/somewhere/appassembler-maven-plugin/1.0/appassembler-maven-plugin-1.0-20060714.142547-1.pom"));
  }

  public void testIsMetadata() {
    assertEquals(true, M2ArtifactRecognizer.isMetadata("maven-metadata.xml"));
    assertEquals(false, M2ArtifactRecognizer.isMetadata("aven-metadata.xml"));
    assertEquals(false, M2ArtifactRecognizer.isMetadata("/javax/mail/mail/1.4/mail-1.4.jar"));
  }

}
