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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.Collections;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.secrets.Secret;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.internal.blobstore.BlobStoreOverrideImpl.NEXUS_BLOB_STORE_OVERRIDE;

/**
 * Test {@code BlobStoreOverrideImpl}.
 */
public class BlobStoreOverrideImplTest
    extends BlobStoreOverrideImplTestSupport
{
  @Test
  public void testNoBlobStoresNoOverride() {
    when(configStore.list()).thenReturn(Collections.emptyList());
    underTest.apply();
    verifyNoMoreInteractions(configStore);
    verifyNoInteractions(secretsService);
  }

  @Test
  public void testNoOverride() {
    BlobStoreConfiguration config = defaultConfig();
    when(configStore.list()).thenReturn(singletonList(config));
    underTest.apply();
    verify(configStore, never()).update(any());
    verifyNoInteractions(secretsService);
  }

  @Test
  public void testWithOverride() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE, "{\"default\":{\"file\":{\"path\":\"other_path\"}}}");
    BlobStoreConfiguration config = defaultConfig();
    when(configStore.list()).thenReturn(singletonList(config));
    underTest.apply();
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("file").get("path"), equalTo("other_path"));
    verifyNoInteractions(secretsService);
  }

  @Test(expected = IllegalStateException.class)
  public void testWithParsingError() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE, "invalid json");
    BlobStoreConfiguration config = defaultConfig();
    when(configStore.list()).thenReturn(singletonList(config));
    underTest.apply();
  }

  @Test
  public void testS3BucketOverride() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"some s3 blob store\":{\"s3\":{\"bucket\":\"some-other-s3-bucket\"}}}");
    BlobStoreConfiguration defaultConfig = defaultConfig();
    BlobStoreConfiguration config = createConfig("some s3 blob store", "S3");
    config.getAttributes().get("s3").put("bucket", "some-s3-bucket");
    when(configStore.list()).thenReturn(asList(defaultConfig, config));
    underTest.apply();
    verify(configStore).update(config);
    verify(configStore, never()).update(defaultConfig);
    assertThat((String) config.getAttributes().get("s3").get("bucket"), equalTo("some-other-s3-bucket"));
  }

  @Test
  public void testS3BucketAndFilePathOverride() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"some s3 blob store\":{\"s3\":{\"bucket\":\"some-other-s3-bucket\"}},\"default\":{\"file\":{\"path\":\"other_path\"}}}");
    BlobStoreConfiguration defaultConfig = defaultConfig();
    BlobStoreConfiguration config = createConfig("some s3 blob store", "S3");
    config.getAttributes().get("s3").put("bucket", "some-s3-bucket");
    when(configStore.list()).thenReturn(asList(defaultConfig, config));
    underTest.apply();
    verify(configStore).update(config);
    verify(configStore).update(defaultConfig);
    assertThat((String) config.getAttributes().get("s3").get("bucket"), equalTo("some-other-s3-bucket"));
    assertThat((String) defaultConfig.getAttributes().get("file").get("path"), equalTo("other_path"));
  }

  // --- Credential encryption tests ---
  //
  // Only S3 exercises the encryption path in production because only its descriptor's read side
  // (AmazonS3Factory) dereferences _<id>. Azure and GCS deliberately do NOT declare
  // getSensitiveConfigurationFields() at present: Azure has no SecretsService integration on the
  // read path, and GCS uses EncryptDecryptService via a different key. Wiring those would break
  // authentication outright, so treating them as opaque strings here is intentional and covered
  // by testAzureAccountKeyMergedVerbatimWithoutEncryption below.

  @Test
  public void testS3SecretAccessKeyEncryption() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"secretAccessKey\":\"AKIA-plaintext\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    when(configStore.list()).thenReturn(singletonList(config));

    Secret newSecret = stubEncryptMavenReturning("_1");

    underTest.apply();

    // userId is passed as null because doStart() runs before Shiro is bound — see NEXUS-54061.
    verify(secretsService).encryptMaven(eq("blobstore-config"), argCharArrayEquals("AKIA-plaintext"), isNull());
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"), equalTo("_1"));
    verify(secretsService, never()).remove(any());
    assertThat(newSecret.getId(), equalTo("_1"));
  }

  @Test
  public void testS3SessionTokenEncryption() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"sessionToken\":\"session-plain\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("sessionToken"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    when(configStore.list()).thenReturn(singletonList(config));

    stubEncryptMavenReturning("_7");

    underTest.apply();

    verify(secretsService).encryptMaven(eq("blobstore-config"), argCharArrayEquals("session-plain"), isNull());
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("s3").get("sessionToken"), equalTo("_7"));
  }

  @Test
  public void testAzureAccountKeyMergedVerbatimWithoutEncryption() {
    // AzureBlobStoreDescriptor does not declare accountKey as a sensitive field (see comment on
    // "Credential encryption tests" above). The override JSON's accountKey must therefore land in
    // the config verbatim, without any SecretsService interaction. This test locks that in.
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-azure\":{\"azure cloud storage\":{\"accountKey\":\"az-plaintext\"}}}");

    BlobStoreConfiguration config = createConfig("my-azure", "Azure Cloud Storage");
    when(configStore.list()).thenReturn(singletonList(config));

    underTest.apply();

    verifyNoInteractions(secretsService);
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("azure cloud storage").get("accountKey"),
        equalTo("az-plaintext"));
  }

  @Test
  public void testNonCredentialAttributesUntouched() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"bucket\":\"prod-bucket\",\"region\":\"us-east-1\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    config.getAttributes().get("s3").put("bucket", "old-bucket");
    config.getAttributes().get("s3").put("region", "eu-west-1");
    when(configStore.list()).thenReturn(singletonList(config));

    underTest.apply();

    verifyNoInteractions(secretsService);
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("s3").get("bucket"), equalTo("prod-bucket"));
    assertThat((String) config.getAttributes().get("s3").get("region"), equalTo("us-east-1"));
  }

  @Test
  public void testMixedCredentialAndNonCredential() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"bucket\":\"prod-bucket\",\"secretAccessKey\":\"plain\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    config.getAttributes().get("s3").put("bucket", "old-bucket");
    when(configStore.list()).thenReturn(singletonList(config));

    stubEncryptMavenReturning("_9");

    underTest.apply();

    verify(secretsService).encryptMaven(eq("blobstore-config"), argCharArrayEquals("plain"), isNull());
    verify(secretsService, never()).encryptMaven(eq("blobstore-config"), argCharArrayEquals("prod-bucket"),
        isNull());
    assertThat((String) config.getAttributes().get("s3").get("bucket"), equalTo("prod-bucket"));
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"), equalTo("_9"));
    verify(configStore).update(config);
  }

  @Test
  public void testUntouchedLegacyPbeTokenInConfigIsNotDoubleEncrypted() {
    // A field NOT present in the override JSON (bucket is overridden; secretAccessKey is not).
    // The config carries an existing legacy PBE token in secretAccessKey. Previously the loop
    // iterated every sensitive field of the config and re-encrypted the token text as if it
    // were the credential — this test locks in that we only touch fields the operator actually
    // supplied in the override JSON.
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"bucket\":\"new-bucket\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    config.getAttributes().get("s3").put("bucket", "old-bucket");
    config.getAttributes().get("s3").put("secretAccessKey", "PBE-legacy-token-that-is-not-underscore-id");
    when(configStore.list()).thenReturn(singletonList(config));

    underTest.apply();

    verifyNoInteractions(secretsService);
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("s3").get("bucket"), equalTo("new-bucket"));
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"),
        equalTo("PBE-legacy-token-that-is-not-underscore-id"));
  }

  @Test
  public void testIdempotentPlaintextOverrideDoesNotCreateNewSecret() {
    // Env var is permanently set (container pattern). Current DB has _5 which decrypts to the
    // same plaintext the operator keeps supplying. Without this check every restart would create
    // a new secrets row and delete the previous one — a churn bug on healthy configs.
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"secretAccessKey\":\"AKIA-plaintext\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    config.getAttributes().get("s3").put("secretAccessKey", "_5");
    when(configStore.list()).thenReturn(singletonList(config));

    Secret existing = mock(Secret.class, "existing-_5");
    when(existing.decrypt()).thenReturn("AKIA-plaintext".toCharArray());
    when(secretsService.from("_5")).thenReturn(existing);

    underTest.apply();

    verify(secretsService, never()).encryptMaven(any(), any(), any());
    verify(secretsService, never()).remove(any());
    verify(configStore, never()).update(any());
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"), equalTo("_5"));
  }

  @Test
  public void testIdempotencyAlreadyEncryptedInJson() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"secretAccessKey\":\"_42\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    config.getAttributes().get("s3").put("secretAccessKey", "old-plaintext-was-broken");
    when(configStore.list()).thenReturn(singletonList(config));

    // Mock secret validation - secret exists and can be decrypted
    when(secretsService.exists("_42")).thenReturn(true);
    Secret validSecret = mock(Secret.class);
    when(validSecret.decrypt()).thenReturn("decrypted-value".toCharArray());
    when(secretsService.from("_42")).thenReturn(validSecret);

    underTest.apply();

    verify(secretsService, never()).encryptMaven(any(), any(), any());
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"), equalTo("_42"));
  }

  @Test
  public void testOverwritingExistingEncryptedValueRemovesOldSecret() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"secretAccessKey\":\"new-plaintext\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    config.getAttributes().get("s3").put("secretAccessKey", "_1");
    when(configStore.list()).thenReturn(singletonList(config));

    Secret oldSecret = mock(Secret.class, "old-_1");
    when(oldSecret.decrypt()).thenReturn("stale-decrypted".toCharArray());
    when(secretsService.from("_1")).thenReturn(oldSecret);

    stubEncryptMavenReturning("_2");

    underTest.apply();

    verify(secretsService).encryptMaven(eq("blobstore-config"), argCharArrayEquals("new-plaintext"), isNull());
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"), equalTo("_2"));
    verify(secretsService).remove(oldSecret);
  }

  @Test
  public void testEncryptionFailureRollsBackNewlyCreatedSecrets() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"secretAccessKey\":\"first\",\"sessionToken\":\"second\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields())
        .thenReturn(asList("secretAccessKey", "sessionToken"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    when(configStore.list()).thenReturn(singletonList(config));

    Secret firstCreated = mock(Secret.class, "created-_11");
    when(firstCreated.getId()).thenReturn("_11");
    when(secretsService.encryptMaven(any(), any(), any()))
        .thenReturn(firstCreated)
        .thenThrow(new CipherException("boom"));

    try {
      underTest.apply();
      fail("Expected CipherException to propagate");
    }
    catch (CipherException expected) {
      // expected
    }

    verify(configStore, never()).update(any());
    verify(secretsService).remove(firstCreated);
  }

  @Test
  public void testUppercaseTypeKeyInOverrideJsonStillEncrypts() {
    // Attribute type sections are stored under lowercased keys in BlobStoreConfiguration#getAttributes.
    // An override JSON that spells the type "S3" (uppercase) must still merge into the "s3" section
    // AND still route sensitive fields through encryption — otherwise the plaintext credential lands
    // in the config unencrypted, which is exactly the bug this ticket fixes.
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"S3\":{\"secretAccessKey\":\"AKIA-plaintext\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    when(configStore.list()).thenReturn(singletonList(config));

    stubEncryptMavenReturning("_5");

    underTest.apply();

    verify(secretsService).encryptMaven(eq("blobstore-config"), argCharArrayEquals("AKIA-plaintext"), isNull());
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"), equalTo("_5"));
  }

  @Test
  public void testCrossStoreFailureDoesNotRollBackAlreadyCommittedSecrets() {
    // Two stores: first commits successfully, second throws on store.update(). The rollback
    // must remove only the SECOND store's uncommitted secret; the first store's secret is
    // already referenced on disk and must NOT be deleted, or we'd orphan the persisted _<id>.
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"first\":{\"s3\":{\"secretAccessKey\":\"first-plain\"}},"
            + "\"second\":{\"s3\":{\"secretAccessKey\":\"second-plain\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration first = createConfig("first", "S3");
    BlobStoreConfiguration second = createConfig("second", "S3");
    when(configStore.list()).thenReturn(asList(first, second));

    Secret firstSecret = mock(Secret.class, "created-first");
    when(firstSecret.getId()).thenReturn("_11");
    Secret secondSecret = mock(Secret.class, "created-second");
    when(secondSecret.getId()).thenReturn("_12");
    when(secretsService.encryptMaven(any(), any(), any())).thenReturn(firstSecret, secondSecret);

    // First store commits. Second store's update throws.
    org.mockito.Mockito.doNothing().when(configStore).update(first);
    org.mockito.Mockito.doThrow(new RuntimeException("db down")).when(configStore).update(second);

    try {
      underTest.apply();
      fail("Expected RuntimeException to propagate");
    }
    catch (RuntimeException expected) {
      // expected
    }

    // First store's secret must remain — its _<id> is already on disk.
    verify(secretsService, never()).remove(firstSecret);
    // Second store's secret must be rolled back.
    verify(secretsService).remove(secondSecret);
  }

  @Test
  public void testUnknownBlobStoreTypeMergesButSkipsEncryption() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"unknown-store\":{\"customtype\":{\"someKey\":\"someValue\"}}}");

    BlobStoreConfiguration config = createConfig("unknown-store", "CustomType");
    when(configStore.list()).thenReturn(singletonList(config));

    underTest.apply();

    verifyNoInteractions(secretsService);
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("customtype").get("someKey"), equalTo("someValue"));
  }

  @Test
  public void testSecretIdInjectionFromNonBlobstorePurposeIsRejected() {
    // Attempt to inject a secret ID that fails decryption
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"secretAccessKey\":\"_999\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    config.getAttributes().get("s3").put("secretAccessKey", "existing-key");
    when(configStore.list()).thenReturn(singletonList(config));

    // Mock the secret exists but fails to decrypt
    when(secretsService.exists("_999")).thenReturn(true);
    Secret injectedSecret = mock(Secret.class);
    when(injectedSecret.decrypt()).thenThrow(new RuntimeException("Decryption failed"));
    when(secretsService.from("_999")).thenReturn(injectedSecret);

    underTest.apply();

    // The injection should be rejected - config should not be updated
    verify(configStore, never()).update(any());
    // The original value should remain unchanged
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"), equalTo("existing-key"));
  }

  @Test
  public void testSecretIdInjectionWithNonexistentSecretIsRejected() {
    // Attempt to inject a secret ID that doesn't exist
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"secretAccessKey\":\"_999\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    config.getAttributes().get("s3").put("secretAccessKey", "existing-key");
    when(configStore.list()).thenReturn(singletonList(config));

    // Mock the secret doesn't exist
    when(secretsService.exists("_999")).thenReturn(false);

    underTest.apply();

    // The injection should be rejected - config should not be updated
    verify(configStore, never()).update(any());
    // The original value should remain unchanged
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"), equalTo("existing-key"));
  }

  @Test
  public void testValidSecretIdFromBlobstorePurposeIsAccepted() {
    // Supply a valid secret ID that exists and can be decrypted
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"my-s3\":{\"s3\":{\"secretAccessKey\":\"_42\"}}}");
    when(s3Descriptor.getSensitiveConfigurationFields()).thenReturn(singletonList("secretAccessKey"));

    BlobStoreConfiguration config = createConfig("my-s3", "S3");
    config.getAttributes().get("s3").put("secretAccessKey", "old-key");
    when(configStore.list()).thenReturn(singletonList(config));

    // Mock the secret exists and can be decrypted
    when(secretsService.exists("_42")).thenReturn(true);
    Secret validSecret = mock(Secret.class);
    when(validSecret.decrypt()).thenReturn("decrypted-value".toCharArray());
    when(secretsService.from("_42")).thenReturn(validSecret);

    underTest.apply();

    // The valid secret ID should be accepted
    verify(configStore).update(config);
    assertThat((String) config.getAttributes().get("s3").get("secretAccessKey"), equalTo("_42"));
  }

  // --- helpers ---

  private Secret stubEncryptMavenReturning(final String id) {
    Secret secret = mock(Secret.class, "created-" + id);
    when(secret.getId()).thenReturn(id);
    when(secretsService.encryptMaven(any(), any(), any())).thenReturn(secret);
    return secret;
  }

  private static char[] argCharArrayEquals(final String expected) {
    return org.mockito.ArgumentMatchers.argThat(new org.mockito.ArgumentMatcher<char[]>()
    {
      @Override
      public boolean matches(final char[] actual) {
        return actual != null && new String(actual).equals(expected);
      }

      @Override
      public String toString() {
        return "char[]=\"" + expected + "\"";
      }
    });
  }
}
