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
package org.sonatype.nexus.repository.maven.internal.cleanup;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.RETAIN_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.RETAIN_SORT_BY_KEY;

public class Maven2CleanupPolicyConfigurationTest
{
  private Maven2CleanupPolicyConfiguration underTest;

  @Before
  public void setUp() {
    underTest = new Maven2CleanupPolicyConfiguration();
  }

  @Test
  public void getConfiguration_returnsNonNullMap() {
    assertThat(underTest.getConfiguration(), is(notNullValue()));
  }

  @Test
  public void getConfiguration_hasSixEntries() {
    assertThat(underTest.getConfiguration().size(), is(6));
  }

  @Test
  public void getConfiguration_containsLastBlobUpdatedEnabled() {
    assertThat(underTest.getConfiguration(), hasEntry(LAST_BLOB_UPDATED_KEY, true));
  }

  @Test
  public void getConfiguration_containsLastDownloadedEnabled() {
    assertThat(underTest.getConfiguration(), hasEntry(LAST_DOWNLOADED_KEY, true));
  }

  @Test
  public void getConfiguration_containsIsPrereleaseEnabled() {
    assertThat(underTest.getConfiguration(), hasEntry(IS_PRERELEASE_KEY, true));
  }

  @Test
  public void getConfiguration_containsRegexEnabled() {
    assertThat(underTest.getConfiguration(), hasEntry(REGEX_KEY, true));
  }

  @Test
  public void getConfiguration_retainDisabledOnCommunityEdition() {
    assertThat(underTest.getConfiguration(), hasEntry(RETAIN_KEY, false));
  }

  @Test
  public void getConfiguration_retainSortByDisabledOnCommunityEdition() {
    assertThat(underTest.getConfiguration(), hasEntry(RETAIN_SORT_BY_KEY, false));
  }
}
