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
package org.sonatype.nexus.security.jwt.rest;

import java.util.Optional;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.jwt.SecretStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JwtSecretApiResourceTest
    extends TestSupport
{
  @Mock
  private SecretStore secretStore;

  @Mock
  private EventManager eventManager;

  private JwtSecretApiResourceV1 underTest;

  @Before
  public void setup() throws Exception {
    underTest = new JwtSecretApiResourceV1(secretStore, eventManager);
  }

  @Test
  public void getSecretNotNull() {
    when(secretStore.getSecret()).thenReturn(Optional.of("secret"));

    Response response = underTest.getSecret();

    assertThat(response.getStatus(), is(200));
    Object entity = response.getEntity();
    assertThat(((String) entity), is("secret"));
  }

  @Test
  public void updateSecret() {
    underTest.updateSecret("secret");

    verify(secretStore).setSecret("secret");
  }
}
