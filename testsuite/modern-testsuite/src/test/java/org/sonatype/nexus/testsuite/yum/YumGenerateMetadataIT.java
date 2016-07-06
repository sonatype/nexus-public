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

import java.util.Map;

import org.sonatype.nexus.client.core.subsystem.repository.Repository;

import com.google.common.collect.Maps;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;
import static org.sonatype.nexus.yum.client.MetadataType.PRIMARY_XML;

/**
 * ITs related to generating metadata.
 *
 * @since 3.0
 */
public class YumGenerateMetadataIT
    extends YumITSupport
{

  public YumGenerateMetadataIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void addRpm()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    content().upload(
        repositoryLocation(repository.id(), "test/test-artifact/0.0.1/test-artifact-0.0.1.rpm"),
        testData().resolveFile("/rpms/test-artifact-1.2.3-1.noarch.rpm")
    );

    waitForNexusToSettleDown();

    final String primaryXml = repodata().getMetadata(repository.id(), PRIMARY_XML, String.class);
    assertThat(primaryXml, containsString("test-artifact"));
  }

  /**
   * @since 3.0.3
   */
  @Test
  public void addRpmWithUpperCaseExtension()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    content().upload(
        repositoryLocation(repository.id(), "test/test-artifact/0.0.1/test-artifact-0.0.1.RPM"),
        testData().resolveFile("/rpms/test-artifact-1.2.3-1.noarch.rpm")
    );

    waitForNexusToSettleDown();

    final String primaryXml = repodata().getMetadata(repository.id(), PRIMARY_XML, String.class);
    assertThat(primaryXml, containsString("test-artifact"));
  }

  @Test
  @Ignore("Ignoring test formerly quarantined on CI")
  public void removeRpm()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    content().upload(
        repositoryLocation(repository.id(), "test/test-artifact/0.0.1/test-artifact-0.0.1.rpm"),
        testData().resolveFile("/rpms/test-artifact-1.2.3-1.noarch.rpm")
    );

    waitForNexusToSettleDown();

    content().delete(
        repositoryLocation(repository.id(), "test/test-artifact/0.0.1/test-artifact-0.0.1.rpm")
    );

    waitForNexusToSettleDown();

    final String primaryXml = repodata().getMetadata(repository.id(), PRIMARY_XML, String.class);
    assertThat(primaryXml, not(containsString("test-artifact")));
  }

  @Test
  @Ignore("Ignoring test formerly quarantined on CI")
  public void removeDirWithRpm()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    content().upload(
        repositoryLocation(repository.id(), "test/test-artifact/0.0.1/test-artifact-0.0.1.rpm"),
        testData().resolveFile("/rpms/test-artifact-1.2.3-1.noarch.rpm")
    );

    waitForNexusToSettleDown();

    content().delete(
        repositoryLocation(repository.id(), "test/test-artifact/0.0.1")
    );

    waitForNexusToSettleDown();

    final String primaryXml = repodata().getMetadata(repository.id(), PRIMARY_XML, String.class);
    assertThat(primaryXml, not(containsString("test-artifact")));
  }

  /**
   * Verify that files under ".nexus/*" (hidden files) does not get indexed.
   *
   * @since 3.0.3
   */
  @Test
  public void addRpmUnderDotNexus()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    content().upload(
        repositoryLocation(repository.id(), "test/test-rpm/5.6.7/test-rpm-5.6.7.rpm"),
        testData.resolveFile("/rpms/test-rpm-5.6.7-1.noarch.rpm")
    );

    waitForNexusToSettleDown();

    {
      final String primaryXml = repodata().getMetadata(repository.id(), PRIMARY_XML, String.class);
      assertThat(primaryXml, containsString("test-rpm"));
    }

    content().upload(
        repositoryLocation(repository.id(), ".nexus/test/test-artifact/0.0.1/test-artifact-0.0.1.rpm"),
        testData().resolveFile("/rpms/test-artifact-1.2.3-1.noarch.rpm")
    );

    waitForNexusToSettleDown();

    {
      final String primaryXml = repodata().getMetadata(repository.id(), PRIMARY_XML, String.class);
      assertThat(primaryXml, not(containsString("test-artifact")));
      assertThat(primaryXml, containsString("test-rpm"));
    }
  }

  /**
   * Verify that files under ".nexus/*" (hidden files) does not get indexed after full re-indexing.
   *
   * @since 3.0.3
   */
  @Test
  public void removeRpmAndRegenerate()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    content().upload(
        repositoryLocation(repository.id(), "test/test-rpm/5.6.7/test-rpm-5.6.7.rpm"),
        testData.resolveFile("/rpms/test-rpm-5.6.7-1.noarch.rpm")
    );
    content().upload(
        repositoryLocation(repository.id(), "test/test-artifact/0.0.1/test-artifact-0.0.1.rpm"),
        testData().resolveFile("/rpms/test-artifact-1.2.3-1.noarch.rpm")
    );

    waitForNexusToSettleDown();

    {
      final String primaryXml = repodata().getMetadata(repository.id(), PRIMARY_XML, String.class);
      assertThat(primaryXml, containsString("test-rpm"));
      assertThat(primaryXml, containsString("test-artifact"));
    }

    content().delete(
        repositoryLocation(repository.id(), "test/test-artifact/0.0.1/test-artifact-0.0.1.rpm")
    );

    waitForNexusToSettleDown();

    {
      final String primaryXml = repodata().getMetadata(repository.id(), PRIMARY_XML, String.class);
      assertThat(primaryXml, not(containsString("test-artifact")));
    }

    final Map<String, String> properties = Maps.newHashMap();
    properties.put("repoId", repository.id());
    properties.put("forceFullScan", Boolean.TRUE.toString());

    scheduler().run("GenerateMetadataTask", properties);

    {
      final String primaryXml = repodata().getMetadata(repository.id(), PRIMARY_XML, String.class);
      assertThat(primaryXml, not(containsString("test-artifact")));
    }
  }

}
