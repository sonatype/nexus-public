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
package org.sonatype.nexus.crypto.secrets.internal;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class EncryptionKeySourceImplTest
    extends TestSupport
{
  private static final String BASE_PATH = "src/test/resources/";

  private static final String TEST_SECRET_KEYS_PATH = BASE_PATH + "test-secret-keys.json";

  private static final String INVALID_FILE_PATH = BASE_PATH + "invalid-secret-keys.json";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testLoadsFile() {
    EncryptionKeySource encryptionKeySource = new EncryptionKeySourceImpl(TEST_SECRET_KEYS_PATH, objectMapper);

    Optional<SecretEncryptionKey> customKey = encryptionKeySource.getActiveKey();

    assertTrue(customKey.isPresent());
    SecretEncryptionKey secretEncryptionKey = customKey.get();
    assertThat(secretEncryptionKey.getId(), is("my-secret"));
    assertThat(secretEncryptionKey.getKey(), is(notNullValue()));
  }

  @Test
  public void testFailsIfUnableToReadFile() {
    EncryptionKeySource encryptionKeySource = new EncryptionKeySourceImpl(INVALID_FILE_PATH, objectMapper);

    UncheckedIOException expected = assertThrows(UncheckedIOException.class,
        encryptionKeySource::getActiveKey);

    assertThat(expected.getMessage(), containsString(INVALID_FILE_PATH));
  }

  @Test
  public void testGetsSpecificSecretKey() {
    EncryptionKeySource encryptionKeySource = new EncryptionKeySourceImpl(TEST_SECRET_KEYS_PATH, objectMapper);

    Optional<SecretEncryptionKey> customKey = encryptionKeySource.getKey("random-key-s3");

    assertTrue(customKey.isPresent());
    SecretEncryptionKey secretEncryptionKey = customKey.get();
    assertThat(secretEncryptionKey.getId(), is("random-key-s3"));
    assertThat(secretEncryptionKey.getKey(), is(notNullValue()));
  }

  @Test
  public void testGetsNonExistingSecretKey() {
    EncryptionKeySource encryptionKeySource = new EncryptionKeySourceImpl(TEST_SECRET_KEYS_PATH, objectMapper);

    Optional<SecretEncryptionKey> customKey = encryptionKeySource.getKey("non-existing-key");

    assertFalse(customKey.isPresent());
  }

  @Test
  public void testSetsKeyWithoutFileReload() throws IOException {
    ObjectMapper mapper = spy(objectMapper);
    doCallRealMethod().when(mapper).readValue(any(File.class), eq(EncryptionKeyList.class));

    EncryptionKeySource encryptionKeySource = new EncryptionKeySourceImpl(TEST_SECRET_KEYS_PATH, mapper);

    //default active key (the one that comes from file) is 'my-secret'
    assertActiveKey(encryptionKeySource.getActiveKey(), "my-secret");
    verify(mapper, times(1)).readValue(any(File.class), eq(EncryptionKeyList.class));

    //updating active key
    encryptionKeySource.setActiveKey("test-secret-1");

    assertActiveKey(encryptionKeySource.getActiveKey(), "test-secret-1");
    //verify we didn't reload the file
    verify(mapper, times(1)).readValue(any(File.class), eq(EncryptionKeyList.class));
  }

  @Test
  public void testSetsKeyReloadingFile() throws IOException {
    ObjectMapper mapper = spy(objectMapper);
    doCallRealMethod().when(mapper).readValue(any(File.class), eq(EncryptionKeyList.class));

    EncryptionKeySource encryptionKeySource = new EncryptionKeySourceImpl(TEST_SECRET_KEYS_PATH, mapper);

    //default active key (the one that comes from file) is 'my-secret'
    assertActiveKey(encryptionKeySource.getActiveKey(), "my-secret");
    verify(mapper, times(1)).readValue(any(File.class), eq(EncryptionKeyList.class));

    //mock new call to get secrets
    doReturn(mockEncryptionKeys("new-key-2", ImmutableList.of(new SecretEncryptionKey("new-key", "random-s3cr3t"),
        new SecretEncryptionKey("new-key-2", "random-s3cr3t-2"))))
        .when(mapper).readValue(any(File.class), eq(EncryptionKeyList.class));

    //updating active key
    encryptionKeySource.setActiveKey("new-key");
    assertActiveKey(encryptionKeySource.getActiveKey(), "new-key");

    //verify we reloaded the file
    verify(mapper, times(2)).readValue(any(File.class), eq(EncryptionKeyList.class));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void assertActiveKey(final Optional<SecretEncryptionKey> maybeKey, final String expectedId) {
    assertTrue(maybeKey.isPresent());

    SecretEncryptionKey key = maybeKey.get();
    assertThat(key.getId(), is(expectedId));
  }

  private EncryptionKeyList mockEncryptionKeys(final String active, final List<SecretEncryptionKey> keys) {
    EncryptionKeyList keyList = new EncryptionKeyList();
    keyList.setActive(active);
    keyList.setKeys(keys);

    return keyList;
  }
}
