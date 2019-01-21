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
package org.sonatype.nexus.repository.maven.internal.validation;

import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.InvalidContentException;

import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.io.IOUtils.toInputStream;

public class MavenMetadataContentValidatorTest
    extends TestSupport
{
  private static final String VALID_PATH = "group/artifact/maven-metadata.xml";

  private static final String VALID_PATH_GROUP_ONLY = "group/maven-metadata.xml";

  private static final String VALID_SNAPSHOT_PATH = "group/artifact/1.0-SNAPSHOT/maven-metadata.xml";

  private static final String INVALID_PATH = "differentGroup/differentArtifact/maven-metadata.xml";

  private MavenMetadataContentValidator underTest;

  @Before
  public void setup() throws Exception {
    underTest = new MavenMetadataContentValidator();
  }

  @Test(expected = InvalidContentException.class)
  public void throwInvalidContentWhenMetadataEmpty() {
    InputStream mavenMetadata = toInputStream("");

    underTest.validate(VALID_PATH, mavenMetadata);
  }

  @Test(expected = InvalidContentException.class)
  public void throwInvalidContentWhenMetadataNotMetadata() {
    InputStream mavenMetadata = toInputStream("This is not metadata");

    underTest.validate(VALID_PATH, mavenMetadata);
  }

  @Test(expected = InvalidContentException.class)
  public void throwInvalidContentWhenMetadataDoesNotMatchPath() {
    InputStream mavenMetadata = toInputStream("<metadata>\n" +
        "  <groupId>group</groupId>\n" +
        "  <artifactId>artifact</artifactId>\n" +
        "</metadata>\n");

    underTest.validate(INVALID_PATH, mavenMetadata);
  }

  @Test
  public void doNotValidateWhenGroupNotFound() {
    InputStream mavenMetadata = toInputStream("<metadata>\n" +
        "  <artifactId>artifact</artifactId>\n" +
        "</metadata>\n");

    underTest.validate(INVALID_PATH, mavenMetadata);
  }

  @Test
  public void doNotValidateWhenGroupEmpty() {
    InputStream mavenMetadata = toInputStream("<metadata>\n" +
        "  <groupId></groupId>\n" +
        "  <artifactId>artifact</artifactId>\n" +
        "</metadata>\n");

    underTest.validate(INVALID_PATH, mavenMetadata);
  }

  @Test(expected = InvalidContentException.class)
  public void throwInvalidContentWhenArtifactNotFound() {
    InputStream mavenMetadata = toInputStream("<metadata>\n" +
        "  <groupId>group</groupId>\n" +
        "</metadata>\n");

    underTest.validate(VALID_PATH, mavenMetadata);
  }
  
  @Test(expected = InvalidContentException.class)
  public void throwInvalidContentWhenArtifactEmpty() {
    InputStream mavenMetadata = toInputStream("<metadata>\n" +
        "  <groupId>group</groupId>\n" +
        "  <artifactId></artifactId>\n" +
        "</metadata>\n");

    underTest.validate(VALID_PATH, mavenMetadata);
  }

  @Test
  public void noExceptionWhenValidContentAndMatchesPath() {
    InputStream mavenMetadata = toInputStream("<metadata>\n" +
        "  <groupId>group</groupId>\n" +
        "  <artifactId>artifact</artifactId>\n" +
        "</metadata>\n");

    underTest.validate(VALID_PATH, mavenMetadata);
  }

  @Test
  public void noExceptionWhenValidContentAndMatchesPathForSnapshotMetadata() {
    InputStream mavenMetadata = toInputStream("<metadata>\n" +
        "  <groupId>group</groupId>\n" +
        "  <artifactId>artifact</artifactId>\n" +
        "  <version>1.0-SNAPSHOT</version>\n" +
        "</metadata>\n");

    underTest.validate(VALID_SNAPSHOT_PATH, mavenMetadata);
  }

  @Test
  public void noExceptionWhenValidContentAndMatchesPathWithReleasedVersion() {
    InputStream mavenMetadata = toInputStream("<metadata>\n" +
        "  <groupId>group</groupId>\n" +
        "  <artifactId>artifact</artifactId>\n" +
        "  <version>1.0</version>\n" +
        "</metadata>\n");

    underTest.validate(VALID_PATH, mavenMetadata);
  }

  @Test
  public void noExceptionWhenGroupOnlyWithCorrectPath() {
    InputStream mavenMetadata = toInputStream("<metadata>\n" +
        "  <groupId>group</groupId>\n" +
        "</metadata>\n");

    underTest.validate(VALID_PATH_GROUP_ONLY, mavenMetadata);
  }
}
