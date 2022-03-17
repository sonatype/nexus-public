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

import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.client.core.subsystem.routing.Routing;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;
import static org.sonatype.nexus.yum.client.MetadataType.PRIMARY_XML;

/**
 * ITs related to proxy metadata.
 *
 * @since 2.7.0
 */
public class YumProxyMetadataIT
    extends YumITSupport
{

  public YumProxyMetadataIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Ignore // NEXUS-6184
  @Test
  public void cleanUp() throws Exception {
    final Repository hosted = createYumEnabledRepository(repositoryIdForTest());
    final MavenProxyRepository proxy = createYumEnabledProxyRepository(repositoryIdForTest() + "-proxy", hosted.contentUri());

    // upload to hosted
    content().upload(
        repositoryLocation(hosted.id(), "test/test-artifact/0.0.1/test-artifact-0.0.1.rpm"),
        testData().resolveFile("/rpms/test-artifact-1.2.3-1.noarch.rpm")
    );
    client().getSubsystem(Routing.class).updatePrefixFile(proxy.id());
    waitForNexusToSettleDown();

    // verify proxy got it, and record the primary path
    {
      final String primaryXml = repodata().getMetadata(proxy.id(), PRIMARY_XML, String.class);
      assertThat(primaryXml, containsString("test-artifact"));
    }
    final String primaryXmlPath1 = repodata().getMetadataPath(proxy.id(), PRIMARY_XML);
    assertThat(content().exists(Location.repositoryLocation(proxy.id(), primaryXmlPath1)), is(true));

    // upload to hosted something else, the modifies repomd.xml and all the files around it
    content().upload(
        repositoryLocation(hosted.id(), "test/othertest-artifact/0.0.1/othertest-artifact-0.0.1.rpm"),
        testData().resolveFile("/rpms/test-artifact-1.2.3-1.noarch.rpm")
    );
    client().getSubsystem(Routing.class).updatePrefixFile(proxy.id());
    waitForNexusToSettleDown();

    // verify proxy got it, and old primary path is not existing anymore (as hash included in file name changed)
    {
      final String primaryXml = repodata().getMetadata(proxy.id(), PRIMARY_XML, String.class);
      assertThat(primaryXml, containsString("test-artifact"));
      assertThat(primaryXml, containsString("othertest-artifact"));
    }
    assertThat(content().exists(Location.repositoryLocation(proxy.id(), primaryXmlPath1)), is(false));
    final String primaryXmlPath2 = repodata().getMetadataPath(proxy.id(), PRIMARY_XML);
    assertThat(content().exists(Location.repositoryLocation(proxy.id(), primaryXmlPath2)), is(true));
  }
}
