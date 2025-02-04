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
package org.sonatype.nexus.repository.httpclient.internal;

import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusEvent;
import org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.Config;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.AVAILABLE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.BLOCKED;

/**
 * Tests for {@link HttpClientFacetImpl.Config}.
 */
public class HttpClientFacetImplConfigTest
    extends TestSupport
{
  private Validator validator;

  private EventManager eventManager = mock(EventManager.class);

  @Before
  public void setUp() throws Exception {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  public void authenticationUsernameWithNullPassword() {
    UsernameAuthenticationConfiguration auth = new UsernameAuthenticationConfiguration();
    auth.setUsername("admin");
    auth.setPassword(null);
    HttpClientFacetImpl.Config config = new HttpClientFacetImpl.Config();
    config.authentication = auth;
    Set<ConstraintViolation<Config>> violations = validator.validate(config);
    assertThat(violations.size(), equalTo(1));
    ConstraintViolation<HttpClientFacetImpl.Config> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), equalTo("authentication.password"));
  }

  @Test
  public void authenticationPasswordWithNullUsername() {
    UsernameAuthenticationConfiguration auth = new UsernameAuthenticationConfiguration();
    auth.setUsername("");
    auth.setPassword(mock(Secret.class));
    HttpClientFacetImpl.Config config = new HttpClientFacetImpl.Config();
    config.authentication = auth;
    Set<ConstraintViolation<HttpClientFacetImpl.Config>> violations = validator.validate(config);
    assertThat(violations.size(), equalTo(1));
    ConstraintViolation<HttpClientFacetImpl.Config> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), equalTo("authentication.username"));

  }

  @Test
  public void requiredFieldsMayNotBeWhitespaceOnly() {
    UsernameAuthenticationConfiguration auth = new UsernameAuthenticationConfiguration();
    auth.setUsername("");
    auth.setPassword(null);
    HttpClientFacetImpl.Config config = new HttpClientFacetImpl.Config();
    config.authentication = auth;
    Set<ConstraintViolation<HttpClientFacetImpl.Config>> violations = validator.validate(config);
    assertThat(violations.size(), equalTo(2));
    assertThat(violations.stream().map(v -> v.getPropertyPath().toString()).sorted().toArray(),
        equalTo(new String[]{"authentication.password", "authentication.username"}));
  }

  @Test
  public void fireEventOnRemoteConnectionStatusChanged() throws Exception {
    HttpClientFacetImpl underTest = new HttpClientFacetImpl(mock(HttpClientManager.class), Collections.emptyMap(),
        Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("test-repository");
    underTest.attach(repository);
    underTest.installDependencies(eventManager);
    underTest.onStatusChanged(new RemoteConnectionStatus(AVAILABLE), new RemoteConnectionStatus(BLOCKED));
    verify(eventManager).post(any(RemoteConnectionStatusEvent.class));
  }
}
