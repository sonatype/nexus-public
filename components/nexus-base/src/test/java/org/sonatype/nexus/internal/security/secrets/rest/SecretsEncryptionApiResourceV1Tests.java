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
package org.sonatype.nexus.internal.security.secrets.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.secrets.MissingKeyException;
import org.sonatype.nexus.crypto.secrets.ReEncryptService;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

public class SecretsEncryptionApiResourceV1Tests
    extends TestSupport
{
  @Mock
  private ReEncryptService reEncryptService;

  private SecretsEncryptionApiResourceV1 underTest;

  @Before
  public void setup() {
    underTest = new SecretsEncryptionApiResourceV1(reEncryptService);
  }

  @Test
  public void testTaskCreated() {
    ReEncryptionRequestApiXO request = new ReEncryptionRequestApiXO("test-key", null);
    Response response = underTest.reEncrypt(request);
    assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
  }

  @Test
  public void testBadRequest() {
    doThrow(new MissingKeyException("key invalid")).when(reEncryptService).submitReEncryption(anyString(), anyString());
    ReEncryptionRequestApiXO request = new ReEncryptionRequestApiXO("test-key", "test@mail.com");
    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class, () -> underTest.reEncrypt(request));

    assertThat(exception.getResponse().getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    ValidationErrorXO error = (ValidationErrorXO) exception.getResponse().getEntity();
    assertThat(error.getMessage()).isEqualTo("key invalid");
  }
}
