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
package org.sonatype.nexus.testsuite.config.nexus3427;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RemoteConnectionSettings;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.test.utils.ResponseMatchers.isSuccessfulCode;
import static org.sonatype.nexus.test.utils.SettingsMessageUtil.getCurrentSettings;
import static org.sonatype.nexus.test.utils.SettingsMessageUtil.save;

public class Nexus3427GlobalSettingsIT
    extends AbstractNexusIntegrationTest
{

  @Test
  public void remoteConnectionSettingsPersistence()
      throws Exception
  {
    final GlobalConfigurationResource gcr1 = getCurrentSettings();
    final RemoteConnectionSettings rcs1 = gcr1.getGlobalConnectionSettings();

    rcs1.setConnectionTimeout(100);
    rcs1.setQueryString("foo=bar");
    rcs1.setRetrievalRetryCount(10);
    rcs1.setUserAgentString("foo");

    assertThat(save(gcr1).getCode(), isSuccessfulCode());

    final GlobalConfigurationResource gcr2 = getCurrentSettings();
    final RemoteConnectionSettings rcs2 = gcr2.getGlobalConnectionSettings();

    assertThat(rcs2.getConnectionTimeout(), is(equalTo(rcs1.getConnectionTimeout())));
    assertThat(rcs2.getQueryString(), is(equalTo(rcs1.getQueryString())));
    assertThat(rcs2.getRetrievalRetryCount(), is(equalTo(rcs1.getRetrievalRetryCount())));
    assertThat(rcs2.getUserAgentString(), is(equalTo(rcs1.getUserAgentString())));

    // NEXUS-3427: ensure that query string/user agent can be reset
    rcs2.setQueryString(null);
    rcs2.setUserAgentString(null);

    assertThat(save(gcr2).getCode(), isSuccessfulCode());

    final GlobalConfigurationResource gcr3 = getCurrentSettings();
    final RemoteConnectionSettings rcs3 = gcr3.getGlobalConnectionSettings();

    assertThat(rcs3.getConnectionTimeout(), is(equalTo(rcs2.getConnectionTimeout())));
    assertThat(rcs3.getQueryString(), is(equalTo(rcs2.getQueryString())));
    assertThat(rcs3.getRetrievalRetryCount(), is(equalTo(rcs2.getRetrievalRetryCount())));
    assertThat(rcs3.getUserAgentString(), is(equalTo(rcs2.getUserAgentString())));
  }

}
