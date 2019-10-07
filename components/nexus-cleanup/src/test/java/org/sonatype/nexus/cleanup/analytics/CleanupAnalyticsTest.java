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
package org.sonatype.nexus.cleanup.analytics;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.config.DefaultCleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_DOWNLOADED_KEY;

public class CleanupAnalyticsTest
    extends TestSupport
{
  private static long COUNT = 1L;

  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @InjectMocks
  private CleanupAnalytics cleanupAnalytics;

  @Test
  public void countReturnsTheNumberOfCleanupPolicies() {
    when(cleanupPolicyStorage.count()).thenReturn(COUNT);

    assertThat(cleanupAnalytics.count(), is(COUNT));
  }
}
