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
package org.sonatype.nexus.testsuite.yum;

import org.sonatype.nexus.client.core.subsystem.repository.Repository;

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;
import static org.sonatype.nexus.yum.client.MetadataType.PRIMARY_XML;

/**
 * ITs related to versioned REST resource.
 *
 * @since 3.0
 */
public class YumVersionedIT
    extends YumITSupport
{

  public YumVersionedIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void shouldGenerateVersionedRepoForVersion()
      throws Exception
  {
    final Repository repository = givenRepositoryWithOneRpm();

    final String content = repodata().getMetadata(repository.id(), "1.0", PRIMARY_XML, String.class);
    assertThat(content, containsString("test-artifact"));
  }

  @Test
  public void shouldGenerateVersionedRepoForAlias()
      throws Exception
  {
    final Repository repository = givenRepositoryWithOneRpm();

    yum().createOrUpdateAlias(repository.id(), "alias", "1.0");
    final String content = repodata().getMetadata(repository.id(), "alias", PRIMARY_XML, String.class);
    assertThat(content, containsString("test-artifact"));
  }

  @Test
  public void shouldGenerateIndexHtml()
      throws Exception
  {
    final Repository repository = givenRepositoryWithOneRpm();

    final String content = repodata().getIndex(repository.id(), "1.0");
    assertThat(content, containsString("<a href=\"repodata/\">repodata/</a>"));
  }

  @Test
  public void shouldGenerateIndexHtmlForRepodata()
      throws Exception
  {
    final Repository repository = givenRepositoryWithOneRpm();

    final String content = repodata().getIndex(repository.id(), "1.0", "repodata");
    assertThat(content, containsString("<a href=\"repomd.xml\">repomd.xml</a>"));
  }

  private Repository givenRepositoryWithOneRpm()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    content().upload(
        repositoryLocation(repository.id(), "group/artifact/1.0/artifact-1.0.rpm"),
        testData.resolveFile("/rpms/test-artifact-1.2.3-1.noarch.rpm")
    );

    waitForNexusToSettleDown();

    return repository;
  }
}
