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
package org.sonatype.nexus.api.rest.selfhosted.security.apikey;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.api.rest.selfhosted.security.apikey.model.ApiKeysReEncryptionRequestApiXO;
import org.sonatype.nexus.crypto.apikey.ApiKeysReEncryptService;
import org.sonatype.nexus.crypto.secrets.ReEncryptionNotSupportedException;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class ApiKeysEncryptionApiResourceV1Test
{
  @Mock
  private ApiKeysReEncryptService apiKeysReEncryptService;

  @InjectMocks
  private ApiKeysEncryptionApiResourceV1 underTest;

  @Test
  void testEncrypt() {
    ApiKeysReEncryptionRequestApiXO request =
        new ApiKeysReEncryptionRequestApiXO("algorithm", null, null);
    Response response = underTest.reEncrypt(request);
    assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

    request = new ApiKeysReEncryptionRequestApiXO(null, null, null);
    response = underTest.reEncrypt(request);
    assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
  }

  @Test
  void testEncrypt_ReEncryptionIsNotSupported() {
    doThrow(new ReEncryptionNotSupportedException("Re-encryption api key principals is not supported"))
        .when(apiKeysReEncryptService)
        .submitReEncryption(any(), any(), any());

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class,
            () -> underTest.reEncrypt(new ApiKeysReEncryptionRequestApiXO(null, null, null)));

    assertThat(exception.getResponse().getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    ValidationErrorXO error = (ValidationErrorXO) exception.getResponse().getEntity();
    assertThat(error.getMessage()).isEqualTo("Re-encryption api key principals is not supported");
  }

  @Test
  void testEncrypt_ReEncryptionIsRunning() {
    doThrow(new IllegalStateException("Re-encryption principals task is already running")).when(apiKeysReEncryptService)
        .submitReEncryption(any(), any(), any());

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class,
            () -> underTest.reEncrypt(new ApiKeysReEncryptionRequestApiXO(null, null, null)));

    assertThat(exception.getResponse().getStatus()).isEqualTo(Status.CONFLICT.getStatusCode());
    ValidationErrorXO error = (ValidationErrorXO) exception.getResponse().getEntity();
    assertThat(error.getMessage()).isEqualTo("Re-encryption principals task is already running");
  }

  @Test
  void testEncrypt_UnexpectedError() {
    doThrow(new RuntimeException("unexpected error")).when(apiKeysReEncryptService)
        .submitReEncryption(any(), any(), any());
    ApiKeysReEncryptionRequestApiXO request =
        new ApiKeysReEncryptionRequestApiXO(null, null, null);
    assertThrows(RuntimeException.class, () -> underTest.reEncrypt(request));
  }

  @Test
  void testEncryptWithCustomIterationsForDecryption() {
    ApiKeysReEncryptionRequestApiXO request =
        new ApiKeysReEncryptionRequestApiXO("PBKDF2WithHmacSHA1", 5000, "admin@example.com");
    Response response = underTest.reEncrypt(request);
    assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
  }

  @Test
  void testEncryptWithAlgorithmAndIterations() {
    ApiKeysReEncryptionRequestApiXO request =
        new ApiKeysReEncryptionRequestApiXO("PBKDF2WithHmacSHA256", 10000, null);
    Response response = underTest.reEncrypt(request);
    assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
  }
}
