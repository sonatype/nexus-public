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
package org.sonatype.nexus.repository.upgrade;

import java.io.File;
import java.io.FileInputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.IndexSyncService.INDEX_UPGRADE_MARKER;
import static org.sonatype.nexus.repository.upgrade.ElasticSearchIndexUpgradeSupport.NEXUS_LSN;

/**
 * @since 3.14
 */
public abstract class ElasticSearchIndexUpgradeTestSupport
    extends TestSupport
{
  @Mock
  protected ApplicationDirectories appDirs;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File elasticSearchDir;

  private ElasticSearchIndexUpgradeSupport underTest;

  protected abstract ElasticSearchIndexUpgradeSupport getElasticSearchIndexUpgrade();

  @Before
  public void setUp() throws Exception {
    elasticSearchDir = temporaryFolder.newFolder();
    when(appDirs.getWorkDirectory("elasticsearch")).thenReturn(elasticSearchDir);
    underTest = getElasticSearchIndexUpgrade();
  }

  public void assertElasticSearchIndexUpgraded() throws Exception {
    underTest.apply();
    try (FileInputStream in = new FileInputStream(new File(elasticSearchDir, NEXUS_LSN))) {
      assertThat(IOUtils.toByteArray(in), is(INDEX_UPGRADE_MARKER.getBytes(UTF_8)));
    }
  }
}
