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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.config;

import org.sonatype.nexus.client.core.subsystem.config.RemoteProxy;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettingsDTO;
import org.sonatype.nexus.rest.model.RemoteProxySettingsDTO;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

/**
 * @since 2.6
 */
public class JerseyRemoteProxyTest
    extends TestSupport
{

  private GlobalConfigurationResource configuration = new GlobalConfigurationResource();

  @Test
  public void noSettings() {
    final RemoteProxy underTest = createJerseyHttpProxy();
    final RemoteProxySettingsDTO settings = underTest.settings();

    assertThat(settings, is(notNullValue()));

    settings.setHttpProxySettings(new RemoteHttpProxySettingsDTO());
    settings.getHttpProxySettings().setProxyHostname("bar");

    settings.setHttpsProxySettings(new RemoteHttpProxySettingsDTO());
    settings.getHttpsProxySettings().setProxyHostname("car");

    settings.addNonProxyHost("foo");

    underTest.save();

    assertThat(configuration.getRemoteProxySettings(), is(notNullValue()));

    assertThat(configuration.getRemoteProxySettings().getHttpProxySettings(), is(notNullValue()));
    assertThat(configuration.getRemoteProxySettings().getHttpProxySettings().getProxyHostname(), is("bar"));

    assertThat(configuration.getRemoteProxySettings().getHttpsProxySettings(), is(notNullValue()));
    assertThat(configuration.getRemoteProxySettings().getHttpsProxySettings().getProxyHostname(), is("car"));

    assertThat(configuration.getRemoteProxySettings().getNonProxyHosts(), hasItem("foo"));
  }

  @Test
  public void existingSettings() {
    final RemoteProxySettingsDTO configSettings = new RemoteProxySettingsDTO();
    configSettings.setHttpProxySettings(new RemoteHttpProxySettingsDTO());
    configSettings.getHttpProxySettings().setProxyHostname("foo");
    configSettings.setHttpsProxySettings(new RemoteHttpProxySettingsDTO());
    configSettings.getHttpsProxySettings().setProxyHostname("bar");
    configSettings.addNonProxyHost("car1");
    configuration.setRemoteProxySettings(configSettings);

    final RemoteProxy underTest = createJerseyHttpProxy();
    final RemoteProxySettingsDTO settings = underTest.settings();

    assertThat(settings, is(notNullValue()));
    assertThat(settings.getHttpProxySettings(), is(notNullValue()));
    assertThat(settings.getHttpProxySettings().getProxyHostname(), is("foo"));
    assertThat(settings.getHttpsProxySettings(), is(notNullValue()));
    assertThat(settings.getHttpsProxySettings().getProxyHostname(), is("bar"));
    assertThat(settings.getNonProxyHosts(), hasItem("car1"));

    configSettings.getHttpProxySettings().setProxyHostname("foo1");
    configSettings.getHttpsProxySettings().setProxyHostname("bar1");
    configSettings.addNonProxyHost("car2");
    underTest.save();

    assertThat(configuration.getRemoteProxySettings(), is(notNullValue()));

    assertThat(configuration.getRemoteProxySettings().getHttpProxySettings(), is(notNullValue()));
    assertThat(configuration.getRemoteProxySettings().getHttpProxySettings().getProxyHostname(), is("foo1"));

    assertThat(configuration.getRemoteProxySettings().getHttpsProxySettings(), is(notNullValue()));
    assertThat(configuration.getRemoteProxySettings().getHttpsProxySettings().getProxyHostname(), is("bar1"));

    assertThat(configuration.getRemoteProxySettings().getNonProxyHosts(), hasItem("car1"));
    assertThat(configuration.getRemoteProxySettings().getNonProxyHosts(), hasItem("car2"));
  }

  @Test
  public void reset() {
    final RemoteProxy underTest = createJerseyHttpProxy();
    final RemoteProxySettingsDTO settings = underTest.settings();

    assertThat(settings, is(notNullValue()));

    settings.addNonProxyHost("bar");
    underTest.refresh();

    assertThat(settings.getNonProxyHosts(), not(hasItem("bar")));
  }

  private JerseyRemoteProxy createJerseyHttpProxy() {
    return new JerseyRemoteProxy(mock(JerseyNexusClient.class))
    {
      @Override
      GlobalConfigurationResource getConfiguration() {
        return configuration;
      }

      @Override
      void setConfiguration(final GlobalConfigurationResource configuration) {
        // do nothing
      }

    };
  }

}
