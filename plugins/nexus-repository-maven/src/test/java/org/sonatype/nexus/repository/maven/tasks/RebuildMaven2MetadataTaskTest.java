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
package org.sonatype.nexus.repository.maven.tasks;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.ALL_REPOSITORIES;
import static org.sonatype.nexus.repository.maven.tasks.RebuildMaven2MetadataTaskDescriptor.ARTIFACTID_FIELD_ID;
import static org.sonatype.nexus.repository.maven.tasks.RebuildMaven2MetadataTaskDescriptor.BASEVERSION_FIELD_ID;
import static org.sonatype.nexus.repository.maven.tasks.RebuildMaven2MetadataTaskDescriptor.GROUPID_FIELD_ID;

public class RebuildMaven2MetadataTaskTest
    extends TestSupport
{
  private static final Format MAVEN_FORMAT = new Maven2Format();

  private static final Type HOSTED_TYPE = new HostedType();

  private static final String GROUP_ID_VALUE = "group id";

  private static final String ARTIFACT_ID_VALUE = "artifact id";

  private static final String BASE_VERSION_VALUE = "base version";

  @Mock
  private Repository repository;

  @Mock
  private MavenMetadataRebuildFacet rebuildFacet;

  private RebuildMaven2MetadataTask underTest;

  @Before
  public void setUp() throws Exception {
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setId("Rebuild metadata test");
    configuration.setTypeId("Rebuild metadata test");
    configuration.setString(GROUPID_FIELD_ID, GROUP_ID_VALUE);
    configuration.setString(ARTIFACTID_FIELD_ID, ARTIFACT_ID_VALUE);
    configuration.setString(BASEVERSION_FIELD_ID, BASE_VERSION_VALUE);
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, ALL_REPOSITORIES);

    when(repository.facet(MavenMetadataRebuildFacet.class)).thenReturn(rebuildFacet);

    underTest = new RebuildMaven2MetadataTask(HOSTED_TYPE, MAVEN_FORMAT);
    underTest.configure(configuration);
  }

  @Test
  public void testTask() {
    underTest.execute(repository);
    verify(rebuildFacet).rebuildMetadata(GROUP_ID_VALUE, ARTIFACT_ID_VALUE, BASE_VERSION_VALUE, false, false);
  }
}
