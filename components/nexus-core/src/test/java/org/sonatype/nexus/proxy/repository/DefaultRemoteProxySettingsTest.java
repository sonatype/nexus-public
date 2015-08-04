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
package org.sonatype.nexus.proxy.repository;

import java.net.MalformedURLException;
import java.net.URL;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.proxy.repository.DefaultRemoteProxySettings.getRemoteHttpProxySettingsFor;

public class DefaultRemoteProxySettingsTest
    extends TestSupport
{

  @Test
  public void httpUrlWhenHttpProxyEnabled()
      throws MalformedURLException
  {
    final RemoteProxySettings rps = mock(RemoteProxySettings.class);
    final RemoteHttpProxySettings rHttpPS = mock(RemoteHttpProxySettings.class);
    when(rps.getHttpProxySettings()).thenReturn(rHttpPS);
    when(rHttpPS.isEnabled()).thenReturn(true);

    assertThat(
        getRemoteHttpProxySettingsFor(new URL("http://localhost:80/test"), rps), is(rHttpPS)
    );
  }

  @Test
  public void httpUrlWhenHttpsProxyEnabled()
      throws MalformedURLException
  {
    final RemoteProxySettings rps = mock(RemoteProxySettings.class);
    final RemoteHttpProxySettings rHttpsPS = mock(RemoteHttpProxySettings.class);
    when(rps.getHttpsProxySettings()).thenReturn(rHttpsPS);
    when(rHttpsPS.isEnabled()).thenReturn(true);

    assertThat(
        getRemoteHttpProxySettingsFor(new URL("http://localhost:80/test"), rps), is(nullValue())
    );
  }

  @Test
  public void httpUrlWhenHttpAndHttpsProxyEnabled()
      throws MalformedURLException
  {
    final RemoteProxySettings rps = mock(RemoteProxySettings.class);
    final RemoteHttpProxySettings rHttpPS = mock(RemoteHttpProxySettings.class);
    final RemoteHttpProxySettings rHttpsPS = mock(RemoteHttpProxySettings.class);
    when(rps.getHttpProxySettings()).thenReturn(rHttpPS);
    when(rps.getHttpsProxySettings()).thenReturn(rHttpsPS);
    when(rHttpPS.isEnabled()).thenReturn(true);
    when(rHttpsPS.isEnabled()).thenReturn(true);

    assertThat(
        getRemoteHttpProxySettingsFor(new URL("http://localhost:80/test"), rps), is(rHttpPS)
    );
  }

  @Test
  public void httpsUrlWhenHttpProxyEnabled()
      throws MalformedURLException
  {
    final RemoteProxySettings rps = mock(RemoteProxySettings.class);
    final RemoteHttpProxySettings rHttpPS = mock(RemoteHttpProxySettings.class);
    when(rps.getHttpProxySettings()).thenReturn(rHttpPS);
    when(rHttpPS.isEnabled()).thenReturn(true);

    assertThat(
        getRemoteHttpProxySettingsFor(new URL("https://localhost:80/test"), rps), is(rHttpPS)
    );
  }

  @Test
  public void httpsUrlWhenHttpsProxyEnabled()
      throws MalformedURLException
  {
    final RemoteProxySettings rps = mock(RemoteProxySettings.class);
    final RemoteHttpProxySettings rHttpsPS = mock(RemoteHttpProxySettings.class);
    when(rps.getHttpsProxySettings()).thenReturn(rHttpsPS);
    when(rHttpsPS.isEnabled()).thenReturn(true);

    assertThat(
        getRemoteHttpProxySettingsFor(new URL("http://localhost:80/test"), rps), is(nullValue())
    );
  }

  @Test
  public void httpsUrlWhenHttpAndHttpsProxyEnabled()
      throws MalformedURLException
  {
    final RemoteProxySettings rps = mock(RemoteProxySettings.class);
    final RemoteHttpProxySettings rHttpPS = mock(RemoteHttpProxySettings.class);
    final RemoteHttpProxySettings rHttpsPS = mock(RemoteHttpProxySettings.class);
    when(rps.getHttpProxySettings()).thenReturn(rHttpPS);
    when(rps.getHttpsProxySettings()).thenReturn(rHttpsPS);
    when(rHttpPS.isEnabled()).thenReturn(true);
    when(rHttpsPS.isEnabled()).thenReturn(true);

    assertThat(
        getRemoteHttpProxySettingsFor(new URL("https://localhost:80/test"), rps), is(rHttpsPS)
    );
  }

  @Test
  public void ftpUrlWhenHttpProxyEnabled()
      throws MalformedURLException
  {
    final RemoteProxySettings rps = mock(RemoteProxySettings.class);
    final RemoteHttpProxySettings rHttpPS = mock(RemoteHttpProxySettings.class);
    when(rps.getHttpProxySettings()).thenReturn(rHttpPS);
    when(rHttpPS.isEnabled()).thenReturn(true);

    assertThat(
        getRemoteHttpProxySettingsFor(new URL("ftp://localhost:80/test"), rps), is(nullValue())
    );
  }

  @Test
  public void ftpUrlWhenHttpsProxyEnabled()
      throws MalformedURLException
  {
    final RemoteProxySettings rps = mock(RemoteProxySettings.class);
    final RemoteHttpProxySettings rHttpsPS = mock(RemoteHttpProxySettings.class);
    when(rps.getHttpsProxySettings()).thenReturn(rHttpsPS);
    when(rHttpsPS.isEnabled()).thenReturn(true);

    assertThat(
        getRemoteHttpProxySettingsFor(new URL("ftp://localhost:80/test"), rps), is(nullValue())
    );
  }

  @Test
  public void ftpUrlWhenHttpAndHttpsProxyEnabled()
      throws MalformedURLException
  {
    final RemoteProxySettings rps = mock(RemoteProxySettings.class);
    final RemoteHttpProxySettings rHttpPS = mock(RemoteHttpProxySettings.class);
    final RemoteHttpProxySettings rHttpsPS = mock(RemoteHttpProxySettings.class);
    when(rps.getHttpProxySettings()).thenReturn(rHttpPS);
    when(rps.getHttpsProxySettings()).thenReturn(rHttpsPS);
    when(rHttpPS.isEnabled()).thenReturn(true);
    when(rHttpsPS.isEnabled()).thenReturn(true);

    assertThat(
        getRemoteHttpProxySettingsFor(new URL("ftp://localhost:80/test"), rps), is(nullValue())
    );
  }

}
