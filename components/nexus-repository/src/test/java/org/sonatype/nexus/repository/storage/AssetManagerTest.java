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
package org.sonatype.nexus.repository.storage;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.capability.StorageSettingsCapabilityConfiguration.DEFAULT_LAST_DOWNLOADED;

public class AssetManagerTest
    extends TestSupport
{
  @Mock
  private Asset asset;

  private AssetManager underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new AssetManager();
  }

  @Test
  public void failWhenAssetLastDownloadedIsNotUpdated() {
    underTest.setExpireInSeconds(0);

    when(asset.markAsDownloaded(0)).thenReturn(false);

    assertThat(underTest.maybeUpdateLastDownloaded(asset), is(false));

    verify(asset).markAsDownloaded(0);
  }

  @Test
  public void successWhenAssetLastDownloadedIsUpdated() {
    when(asset.markAsDownloaded(DEFAULT_LAST_DOWNLOADED)).thenReturn(true);

    assertThat(underTest.maybeUpdateLastDownloaded(asset), is(true));

    verify(asset).markAsDownloaded(DEFAULT_LAST_DOWNLOADED);
  }
}
