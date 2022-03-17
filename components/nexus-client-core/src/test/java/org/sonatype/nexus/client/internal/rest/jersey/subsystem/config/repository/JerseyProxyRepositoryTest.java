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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.config.repository;

import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyProxyRepository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Validate the configuration helpers are properly updating the underlying configuration
 */
public class JerseyProxyRepositoryTest
    extends TestSupport
{

  private RepositoryProxyResource configuration = new RepositoryProxyResource();

  @Test
  public void noSettings() {
    createJerseyProxyRepository("repoId");

    assertThat(configuration.getRemoteStorage(), nullValue());
  }

  @Test
  public void connectionSettings() {
    createJerseyProxyRepository("repoId").withRemoteConnectionSettings(5, 8, "query", "userAgent");

    assertThat(configuration.getRemoteStorage(), notNullValue());
    assertThat(configuration.getRemoteStorage().getConnectionSettings(), notNullValue());
    assertThat(configuration.getRemoteStorage().getConnectionSettings().getConnectionTimeout(), is(5));
    assertThat(configuration.getRemoteStorage().getConnectionSettings().getRetrievalRetryCount(), is(8));
    assertThat(configuration.getRemoteStorage().getConnectionSettings().getQueryString(), is("query"));
    assertThat(configuration.getRemoteStorage().getConnectionSettings().getUserAgentString(), is("userAgent"));
  }

  @Test
  public void usernameAuthSettings() {
    createJerseyProxyRepository("repoId").withUsernameAuthentication("username", "password");

    assertThat(configuration.getRemoteStorage(), notNullValue());
    assertThat(configuration.getRemoteStorage().getAuthentication(), notNullValue());
    assertThat(configuration.getRemoteStorage().getAuthentication().getUsername(), is("username"));
    assertThat(configuration.getRemoteStorage().getAuthentication().getPassword(), is("password"));
  }

  @Test
  public void ntlmAuthSettings() {
    createJerseyProxyRepository("repoId").withNtlmAuthentication("ntlmusername", "ntlmpassword", "ntlmhost", "ntlmdomain");

    assertThat(configuration.getRemoteStorage(), notNullValue());
    assertThat(configuration.getRemoteStorage().getAuthentication(), notNullValue());
    assertThat(configuration.getRemoteStorage().getAuthentication().getUsername(), is("ntlmusername"));
    assertThat(configuration.getRemoteStorage().getAuthentication().getPassword(), is("ntlmpassword"));
    assertThat(configuration.getRemoteStorage().getAuthentication().getNtlmHost(), is("ntlmhost"));
    assertThat(configuration.getRemoteStorage().getAuthentication().getNtlmDomain(), is("ntlmdomain"));
  }

  private JerseyProxyRepository createJerseyProxyRepository(String repositoryId) {
    return new JerseyProxyRepository(mock(JerseyNexusClient.class), repositoryId)
    {
      @Override
      protected RepositoryProxyResource settings() {
        return configuration;
      }
    };
  }

}
