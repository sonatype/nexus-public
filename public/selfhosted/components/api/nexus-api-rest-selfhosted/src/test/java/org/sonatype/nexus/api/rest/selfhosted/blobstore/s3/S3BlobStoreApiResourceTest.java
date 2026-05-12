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
package org.sonatype.nexus.api.rest.selfhosted.blobstore.s3;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.sonatype.nexus.api.rest.common.blobstore.model.BlobStoreApiSoftQuota;
import org.sonatype.nexus.api.rest.common.blobstore.s3.S3BlobStoreApiConfigurationMapper;
import org.sonatype.nexus.api.rest.common.blobstore.s3.S3BlobStoreApiModelMapper;
import org.sonatype.nexus.api.rest.common.blobstore.s3.S3BlobStoreApiUpdateValidation;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiBucketConfiguration;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiBucketSecurity;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiModel;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class S3BlobStoreApiResourceTest
{
  private static final String BLOBSTORE_NAME = "test-blobstore";

  private static final String SECRET_ACCESS_KEY_SECRET_ID = "_1";

  private static final String SESSION_TOKEN_SECRET_ID = "_2";

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private S3BlobStoreApiUpdateValidation s3BlobStoreApiUpdateValidation;

  @Mock
  private SecretsFactory secretsFactory;

  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  private BlobStore blobstore;

  @InjectMocks
  private S3BlobStoreApiResource underTest;

  @BeforeEach
  void setup() {
    underTest = new S3BlobStoreApiResource(blobStoreManager, s3BlobStoreApiUpdateValidation, secretsFactory,
        applicationVersion);
  }

  @Test
  void createBlobStore() throws Exception {
    when(blobStoreManager.newConfiguration()).thenReturn(blobStoreConfiguration);
    S3BlobStoreApiModel request = simpleApiModel();
    try (MockedStatic<S3BlobStoreApiModelMapper> modelMapper = mockStatic(S3BlobStoreApiModelMapper.class)) {
      modelMapper.when(
          () -> S3BlobStoreApiModelMapper.map(any(BlobStoreConfiguration.class), any(S3BlobStoreApiModel.class), any(
              ApplicationVersion.class)))
          .thenReturn(blobStoreConfiguration);

      try (Response response = underTest.createBlobStore(request)) {
        verify(s3BlobStoreApiUpdateValidation).validateCreateRequest(request);
        verify(blobStoreManager).create(blobStoreConfiguration);
        assertThat(response.getStatus(), is(CREATED.getStatusCode()));
      }
    }
  }

  @Test
  void updateBlobStoreNoSecretsChanged() throws Exception {
    when(blobStoreManager.newConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreManager.get(BLOBSTORE_NAME)).thenReturn(blobstore);

    mockBlobstoreConfig();
    String passwordPlaceholder = PasswordPlaceholder.get();
    S3BlobStoreApiModel request = spy(authApiModel(passwordPlaceholder, passwordPlaceholder));

    try (MockedStatic<S3BlobStoreApiModelMapper> modelMapper = mockStatic(S3BlobStoreApiModelMapper.class)) {
      modelMapper.when(
          () -> S3BlobStoreApiModelMapper.map(any(BlobStoreConfiguration.class), any(S3BlobStoreApiModel.class),
              any(ApplicationVersion.class)))
          .thenReturn(blobStoreConfiguration);

      underTest.updateBlobStore(request, BLOBSTORE_NAME);

      verify(s3BlobStoreApiUpdateValidation).validateUpdateRequest(request, BLOBSTORE_NAME);
      verify(blobStoreManager).get(BLOBSTORE_NAME);
      verify(blobStoreManager).newConfiguration();
      verify(blobStoreManager).update(blobStoreConfiguration);
      verify(secretsFactory).from(SECRET_ACCESS_KEY_SECRET_ID);
      verify(secretsFactory).from(SESSION_TOKEN_SECRET_ID);
      assertThat(request.getBucketConfiguration().getBucketSecurity().getSecretAccessKey(),
          is("storedSecretAccessKey"));
      assertThat(request.getBucketConfiguration().getBucketSecurity().getSessionToken(), is("storedSessionToken"));
    }
  }

  @Test
  void updateBlobStoreNewSecrets() throws Exception {
    when(blobStoreManager.newConfiguration()).thenReturn(blobStoreConfiguration);
    S3BlobStoreApiModel request = spy(authApiModel("newSecretAccessKey", "newSessionToken"));

    try (MockedStatic<S3BlobStoreApiModelMapper> modelMapper = mockStatic(S3BlobStoreApiModelMapper.class)) {
      modelMapper.when(
          () -> S3BlobStoreApiModelMapper.map(any(BlobStoreConfiguration.class), any(S3BlobStoreApiModel.class),
              any(ApplicationVersion.class)))
          .thenReturn(blobStoreConfiguration);

      underTest.updateBlobStore(request, BLOBSTORE_NAME);

      verify(s3BlobStoreApiUpdateValidation).validateUpdateRequest(request, BLOBSTORE_NAME);
      verify(blobStoreManager).get(BLOBSTORE_NAME);
      verify(blobStoreManager).newConfiguration();
      verify(blobStoreManager).update(blobStoreConfiguration);
      verify(secretsFactory, never()).from(anyString());
      assertThat(request.getBucketConfiguration().getBucketSecurity().getSecretAccessKey(), is("newSecretAccessKey"));
      assertThat(request.getBucketConfiguration().getBucketSecurity().getSessionToken(), is("newSessionToken"));
    }
  }

  @Test
  void getBlobstore() {
    when(blobstore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getType()).thenReturn("s3");
    when(blobStoreManager.get(BLOBSTORE_NAME)).thenReturn(blobstore);

    try (MockedStatic<S3BlobStoreApiConfigurationMapper> configMapper = mockStatic(
        S3BlobStoreApiConfigurationMapper.class)) {
      configMapper.when(
          () -> S3BlobStoreApiConfigurationMapper.map(any(BlobStoreConfiguration.class)))
          .thenReturn(authApiModel("secretAccess", "sessionToken"));

      S3BlobStoreApiModel response = underTest.getBlobStore(BLOBSTORE_NAME);

      verify(blobStoreManager).get(BLOBSTORE_NAME);
      assertThat(response.getBucketConfiguration().getBucketSecurity().getSecretAccessKey(),
          is(PasswordPlaceholder.get()));
      assertThat(response.getBucketConfiguration().getBucketSecurity().getSessionToken(),
          is(PasswordPlaceholder.get()));
    }
  }

  @Test
  void deleteBlobstore() throws Exception {
    when(blobStoreManager.get(anyString())).thenReturn(blobstore);

    try (Response response = underTest.deleteBlobStoreWithEmptyName()) {
      verify(blobStoreManager).get(anyString());
      verify(blobStoreManager).delete(anyString());
      assertThat(response.getStatus(), is(NO_CONTENT.getStatusCode()));
    }
  }

  private S3BlobStoreApiModel simpleApiModel() {

    BlobStoreApiSoftQuota softQuota = mock(BlobStoreApiSoftQuota.class);
    S3BlobStoreApiBucketConfiguration bucketConfiguration = mock(S3BlobStoreApiBucketConfiguration.class);

    return new S3BlobStoreApiModel(BLOBSTORE_NAME, softQuota, bucketConfiguration);
  }

  private S3BlobStoreApiModel authApiModel(final String secretAccessKey, final String sessionToken) {
    BlobStoreApiSoftQuota softQuota = mock(BlobStoreApiSoftQuota.class);

    S3BlobStoreApiBucketSecurity bucketSecurity = new S3BlobStoreApiBucketSecurity("accessKey", secretAccessKey, "role",
        sessionToken);
    S3BlobStoreApiBucketConfiguration bucketConfiguration = new S3BlobStoreApiBucketConfiguration(null, bucketSecurity,
        null, null, null, null, null);

    return new S3BlobStoreApiModel(BLOBSTORE_NAME, softQuota, bucketConfiguration);
  }

  private void mockBlobstoreConfig() {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("s3",
        Map.of("secretAccessKey", SECRET_ACCESS_KEY_SECRET_ID, "sessionToken", SESSION_TOKEN_SECRET_ID));
    when(blobStoreConfiguration.getAttributes()).thenReturn(attributes);
    when(blobstore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    Secret secretAccessKey = mockSecret("storedSecretAccessKey");
    Secret secretSessionToken = mockSecret("storedSessionToken");
    when(secretsFactory.from(SECRET_ACCESS_KEY_SECRET_ID)).thenReturn(secretAccessKey);
    when(secretsFactory.from(SESSION_TOKEN_SECRET_ID)).thenReturn(secretSessionToken);
  }

  private Secret mockSecret(final String realValue) {
    Secret secret = mock(Secret.class);
    when(secret.decrypt()).thenReturn(realValue.toCharArray());

    return secret;
  }
}
