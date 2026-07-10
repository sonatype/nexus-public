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
package org.sonatype.nexus.repository.manager.internal;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolation;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link BlobStoreExistenceValidator}.
 */
@ExtendWith(MockitoExtension.class)
public class BlobStoreExistenceValidatorTest
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private ConstraintViolationFactory constraintViolationFactory;

  @Mock
  private Configuration configuration;

  private BlobStoreExistenceValidator underTest;

  @BeforeEach
  public void setUp() {
    underTest = new BlobStoreExistenceValidator(blobStoreManager, constraintViolationFactory);
  }

  @Test
  public void testValidate_withMissingBlobStore() {
    @SuppressWarnings("unchecked")
    ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
    when(constraintViolationFactory.createViolation(any(), any())).thenReturn((ConstraintViolation) mockViolation);
    when(blobStoreManager.get("missing-blobstore")).thenReturn(null);

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> storageAttributes = new HashMap<>();
    storageAttributes.put("blobStoreName", "missing-blobstore");
    attributes.put("storage", storageAttributes);

    when(configuration.getAttributes()).thenReturn(attributes);

    ConstraintViolation<?> result = underTest.validate(configuration);

    assertThat(result, is(notNullValue()));
    verify(constraintViolationFactory).createViolation(
        eq("storage.blobStoreName"),
        eq("No blob store exists with the specified name"));
  }

  @Test
  public void testValidate_withExistingBlobStore() {
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStoreManager.get("existing-blobstore")).thenReturn(blobStore);

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> storageAttributes = new HashMap<>();
    storageAttributes.put("blobStoreName", "existing-blobstore");
    attributes.put("storage", storageAttributes);

    when(configuration.getAttributes()).thenReturn(attributes);

    ConstraintViolation<?> result = underTest.validate(configuration);

    assertThat(result, is(nullValue()));
  }

  @Test
  public void testValidate_withNullAttributes() {
    when(configuration.getAttributes()).thenReturn(null);

    ConstraintViolation<?> result = underTest.validate(configuration);

    assertThat(result, is(nullValue()));
  }

  @Test
  public void testValidate_withoutStorageAttributes() {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    when(configuration.getAttributes()).thenReturn(attributes);

    ConstraintViolation<?> result = underTest.validate(configuration);

    assertThat(result, is(nullValue()));
  }

  @Test
  public void testValidate_withoutBlobStoreName() {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> storageAttributes = new HashMap<>();
    attributes.put("storage", storageAttributes);

    when(configuration.getAttributes()).thenReturn(attributes);

    ConstraintViolation<?> result = underTest.validate(configuration);

    assertThat(result, is(nullValue()));
  }

  @Test
  public void testValidate_withNullBlobStoreName() {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> storageAttributes = new HashMap<>();
    storageAttributes.put("blobStoreName", null);
    attributes.put("storage", storageAttributes);

    when(configuration.getAttributes()).thenReturn(attributes);

    ConstraintViolation<?> result = underTest.validate(configuration);

    assertThat(result, is(nullValue()));
  }
}
