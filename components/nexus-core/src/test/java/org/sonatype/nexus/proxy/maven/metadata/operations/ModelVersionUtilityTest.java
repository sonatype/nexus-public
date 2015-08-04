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
package org.sonatype.nexus.proxy.maven.metadata.operations;

import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility.Version;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ModelVersionUtilityTest
{

  @Test
  public void testEmptyMetadataGetVersion() {
    Metadata metadata = new Metadata();

    assertThat(ModelVersionUtility.getModelVersion(metadata), is(Version.V100));

  }

  @Test
  public void testEmptyMetadataSetV110() {
    Metadata metadata = new Metadata();
    ModelVersionUtility.setModelVersion(metadata, Version.V110);
    assertThat(ModelVersionUtility.getModelVersion(metadata), is(Version.V110));
  }

  @Test
  public void testEmptyMetadataSetV100() {
    Metadata metadata = new Metadata();
    ModelVersionUtility.setModelVersion(metadata, Version.V100);
    assertThat(ModelVersionUtility.getModelVersion(metadata), is(Version.V100));
  }

}
