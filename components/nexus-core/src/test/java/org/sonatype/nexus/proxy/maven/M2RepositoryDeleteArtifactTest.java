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
package org.sonatype.nexus.proxy.maven;

import java.io.File;
import java.net.URL;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.LocalStatus;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.contains;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.doesNotContain;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.exists;

/**
 * @since 2.5
 */
public class M2RepositoryDeleteArtifactTest
    extends AbstractMavenRepoContentTests
{
  protected ResourceStoreRequest newExternalRequest(final String path) {
    return new ResourceStoreRequest(path).setExternal(true);
  }

  /**
   * NEXUS-2834: Verify that maven metadata is recreated after artifacts are deleted.
   */
  @Test
  public void mavenMetadataIsRegeneratedWhenItemIsRemoved()
      throws Exception
  {
    fillInRepo();
    repositoryRegistry.getRepository("central").setLocalStatus(LocalStatus.OUT_OF_SERVICE);
    nexusConfiguration().saveConfiguration();

    releases.recreateMavenMetadata(new ResourceStoreRequest("/"));
    snapshots.recreateMavenMetadata(new ResourceStoreRequest("/"));

    final File releasesRoot = new File(new URL(releases.getLocalUrl()).toURI());
    final File snapshotsRoot = new File(new URL(snapshots.getLocalUrl()).toURI());

    assertThat(
        new File(releasesRoot, "org/sonatype/nexus/nexus-indexer/maven-metadata.xml"),
        contains("<version>1.0-beta-4</version>")
    );
    assertThat(
        new File(snapshotsRoot, "org/sonatype/nexus/nexus-indexer/maven-metadata.xml"),
        contains(
            "<version>1.0-beta-4-SNAPSHOT</version>",
            "<version>1.0-beta-5-SNAPSHOT</version>"
        )
    );
    assertThat(
        new File(snapshotsRoot, "org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/maven-metadata.xml"),
        contains("<version>1.0-beta-4-SNAPSHOT</version>")
    );
    assertThat(
        new File(snapshotsRoot, "org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/maven-metadata.xml"),
        contains("<value>1.0-beta-5-20080731.150252-163</value>")
    );
    assertThat(
        new File(snapshotsRoot, "org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/maven-metadata.xml"),
        doesNotContain("<value>1.0-beta-5-20080730.002543-149</value>")
    );

    releases.deleteItem(true, newExternalRequest(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4"
    ));
    snapshots.deleteItem(true, newExternalRequest(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT"
    ));
    snapshots.deleteItem(true, newExternalRequest(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.jar"
    ));
    snapshots.deleteItem(true, newExternalRequest(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.pom"
    ));

    assertThat(
        new File(releasesRoot, "org/sonatype/nexus/nexus-indexer/maven-metadata.xml"),
        not(exists())
    );
    assertThat(
        new File(snapshotsRoot, "org/sonatype/nexus/nexus-indexer/maven-metadata.xml"),
        doesNotContain("<version>1.0-beta-4-SNAPSHOT</version>")
    );
    assertThat(
        new File(snapshotsRoot, "org/sonatype/nexus/nexus-indexer/maven-metadata.xml"),
        contains("<version>1.0-beta-5-SNAPSHOT</version>")
    );
    assertThat(
        new File(snapshotsRoot, "org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/maven-metadata.xml"),
        not(exists())
    );
    assertThat(
        new File(snapshotsRoot, "org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/maven-metadata.xml"),
        doesNotContain("<value>1.0-beta-5-20080731.150252-163</value>")
    );
    assertThat(
        new File(snapshotsRoot, "org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/maven-metadata.xml"),
        contains("<value>1.0-beta-5-20080730.002543-149</value>")
    );
  }

}
