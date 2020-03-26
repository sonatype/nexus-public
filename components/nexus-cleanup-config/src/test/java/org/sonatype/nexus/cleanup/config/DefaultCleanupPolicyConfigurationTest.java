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
package org.sonatype.nexus.cleanup.config;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_DOWNLOADED_KEY;

public class DefaultCleanupPolicyConfigurationTest
    extends TestSupport
{
  @Test
  public void allFieldsExceptReleasePrereleaseAreEnabled() throws Exception {
    DefaultCleanupPolicyConfiguration underTest = new DefaultCleanupPolicyConfiguration();

    assertThat(underTest.getConfiguration().get(LAST_BLOB_UPDATED_KEY), is(equalTo(true)));
    assertThat(underTest.getConfiguration().get(LAST_DOWNLOADED_KEY), is(equalTo(true)));
    assertThat(underTest.getConfiguration().get(IS_PRERELEASE_KEY), is(equalTo(false)));
  }
}
