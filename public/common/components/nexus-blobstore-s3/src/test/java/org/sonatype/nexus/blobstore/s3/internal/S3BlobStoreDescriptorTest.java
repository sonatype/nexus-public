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
package org.sonatype.nexus.blobstore.s3.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Provider;
import jakarta.validation.ValidationException;

import org.sonatype.nexus.rest.ValidationErrorsException;

import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.SelectOption;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.blobstore.s3.internal.capability.CustomS3RegionCapability;
import org.sonatype.nexus.blobstore.s3.internal.capability.CustomS3RegionCapabilityConfiguration;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import com.google.common.base.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_PREFIX_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;

@ExtendWith(MockitoExtension.class)
class S3BlobStoreDescriptorTest
{

  @Mock
  private BlobStoreQuotaService quotaService;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private Provider<CapabilityRegistry> capabilityRegistryProvider;

  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Mock
  private AntiSsrfService antiSsrfService;

  private S3BlobStoreDescriptor underTest;

  private Map<String, BlobStore> blobStores;

  @BeforeEach
  void setup() {
    underTest = new S3BlobStoreDescriptor(quotaService, blobStoreManager, capabilityRegistryProvider, antiSsrfService);
    blobStores = new HashMap<>();

    lenient().when(blobStoreManager.get(anyString())).thenAnswer(invocation -> {
      String name = invocation.getArgument(0, String.class);
      return blobStores.computeIfAbsent(name, k -> mockBlobStore(k, "mock", new HashMap<>()));
    });

    lenient().when(blobStoreManager.browse()).thenAnswer(invocation -> blobStores.values());
  }

  @AfterEach
  void tearDown() {
    blobStores.clear();
  }

  @Test
  void testS3BlobStoreValidatesItsQuota() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

    underTest.validateConfig(config);

    verify(quotaService, times(1)).validateSoftQuotaConfig(any());
  }

  @Test
  void testSingleS3ConfigurationIsValid() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

    Map<String, Object> s3Attributes = new HashMap<>();
    s3Attributes.put("bucket", "bucket");

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("s3", s3Attributes);

    config.setName("self");
    config.setAttributes(attributes);

    underTest.validateConfig(config);
  }

  @Test
  void testConfigSharesBucketWithNonOverlappingPrefixesIsValid() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

    Map<String, Object> otherS3Attributes = new HashMap<>();
    otherS3Attributes.put("bucket", "bucket");
    otherS3Attributes.put("prefix", "prefix");

    Map<String, Map<String, Object>> otherAttributes = new HashMap<>();
    otherAttributes.put("s3", otherS3Attributes);

    blobStores.put("other", mockBlobStore("other", S3BlobStore.TYPE, otherAttributes));

    Map<String, Object> selfS3Attributes = new HashMap<>();
    selfS3Attributes.put("bucket", "bucket");
    selfS3Attributes.put("prefix", "foo");

    Map<String, Map<String, Object>> selfAttributes = new HashMap<>();
    selfAttributes.put("s3", selfS3Attributes);

    config.setName("self");
    config.setAttributes(selfAttributes);

    underTest.validateConfig(config);
  }

  @Test
  void testConfigSharesBucketWithNoPrefixIsInvalid() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

    Map<String, Object> otherS3Attributes = new HashMap<>();
    otherS3Attributes.put("bucket", "bucket");
    otherS3Attributes.put("prefix", "");

    Map<String, Map<String, Object>> otherAttributes = new HashMap<>();
    otherAttributes.put("s3", otherS3Attributes);

    blobStores.put("other", mockBlobStore("other", S3BlobStore.TYPE, otherAttributes));

    Map<String, Object> selfS3Attributes = new HashMap<>();
    selfS3Attributes.put("bucket", "bucket");
    selfS3Attributes.put("prefix", "");

    Map<String, Map<String, Object>> selfAttributes = new HashMap<>();
    selfAttributes.put("s3", selfS3Attributes);

    config.setName("self");
    config.setAttributes(selfAttributes);

    ValidationException exception = assertThrows(ValidationException.class, () -> {
      underTest.validateConfig(config);
    });
    assertEquals("Blob Store 'other' is already using bucket 'bucket'", exception.getMessage());
  }

  @Test
  void testConfigSharesBucketWithOverlappingPrefixesInvalid() {
    String[][] prefixes = {
        {"foo", "foo"},
        {"", "foo"},
        {"foo", ""},
        {"foo/bar", "foo"},
        {"foo", "foo/bar"}
    };

    for (String[] prefixPair : prefixes) {
      MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

      Map<String, Object> otherS3Attributes = new HashMap<>();
      otherS3Attributes.put("bucket", "bucket");
      otherS3Attributes.put("prefix", prefixPair[0]);

      Map<String, Map<String, Object>> otherAttributes = new HashMap<>();
      otherAttributes.put("s3", otherS3Attributes);

      blobStores.put("other", mockBlobStore("other", S3BlobStore.TYPE, otherAttributes));

      Map<String, Object> selfS3Attributes = new HashMap<>();
      selfS3Attributes.put("bucket", "bucket");
      selfS3Attributes.put("prefix", prefixPair[1]);

      Map<String, Map<String, Object>> selfAttributes = new HashMap<>();
      selfAttributes.put("s3", selfS3Attributes);

      config.setName("self");
      config.setAttributes(selfAttributes);

      ValidationException exception = assertThrows(ValidationException.class, () -> {
        underTest.validateConfig(config);
      });
      assertEquals("Blob Store 'other' is already using bucket 'bucket' with prefix '" + prefixPair[0] + "'",
          exception.getMessage());
    }
  }

  @Test
  void testConfigSharesBucketWithNonOverlappingPrefixesValid() {
    String[][] prefixes = {
        {"foo", "bar"},
        {"foo", "bar/foo"},
        {"foo", "foo_bar"}
    };

    for (String[] prefixPair : prefixes) {
      MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

      Map<String, Object> otherS3Attributes = new HashMap<>();
      otherS3Attributes.put("bucket", "bucket");
      otherS3Attributes.put("prefix", prefixPair[0]);

      Map<String, Map<String, Object>> otherAttributes = new HashMap<>();
      otherAttributes.put("s3", otherS3Attributes);

      blobStores.put("other", mockBlobStore("other", S3BlobStore.TYPE, otherAttributes));

      Map<String, Object> selfS3Attributes = new HashMap<>();
      selfS3Attributes.put("bucket", "bucket");
      selfS3Attributes.put("prefix", prefixPair[1]);

      Map<String, Map<String, Object>> selfAttributes = new HashMap<>();
      selfAttributes.put("s3", selfS3Attributes);

      config.setName("self");
      config.setAttributes(selfAttributes);

      underTest.validateConfig(config);
    }
  }

  @Test
  void testConfigSharesBucketNameWithDifferentEndpointsIsValid() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

    Map<String, Object> otherS3Attributes = new HashMap<>();
    otherS3Attributes.put("bucket", "bucket");
    otherS3Attributes.put("endpoint", "aws");

    Map<String, Map<String, Object>> otherAttributes = new HashMap<>();
    otherAttributes.put("s3", otherS3Attributes);

    blobStores.put("other", mockBlobStore("other", S3BlobStore.TYPE, otherAttributes));

    Map<String, Object> selfS3Attributes = new HashMap<>();
    selfS3Attributes.put("bucket", "bucket");
    selfS3Attributes.put("endpoint", "non-aws");

    Map<String, Map<String, Object>> selfAttributes = new HashMap<>();
    selfAttributes.put("s3", selfS3Attributes);

    config.setName("self");
    config.setAttributes(selfAttributes);

    underTest.validateConfig(config);
  }

  @Test
  void testTransformPrefixByTrimmingAndCollapsingDuplicateSlashes() {
    String[][] prefixes = {
        {null, ""},
        {"", ""},
        {" ", " "},
        {"/test", "test"},
        {"/test/", "test"},
        {" /test/ ", "test"},
        {"/ test /", "test"},
        {"///test///", "test"},
        {"///te/st///", "te/st"},
        {"te////st", "te/st"},
        {"///te////st///", "te/st"},
        {"//////", ""}
    };

    for (String[] prefixPair : prefixes) {
      MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

      Map<String, Object> s3Attributes = new HashMap<>();
      s3Attributes.put(BUCKET_PREFIX_KEY, prefixPair[0]);

      Map<String, Map<String, Object>> attributes = new HashMap<>();
      attributes.put(CONFIG_KEY, s3Attributes);

      config.setName("self");
      config.setAttributes(attributes);

      underTest.sanitizeConfig(config);

      assertEquals(prefixPair[1], config.getAttributes().get(CONFIG_KEY).get(BUCKET_PREFIX_KEY));
    }
  }

  @Test
  void testCustomS3RegionCapabilityIsEnabled() {
    S3BlobStoreDescriptor spyDescriptor =
        spy(new S3BlobStoreDescriptor(quotaService, blobStoreManager, capabilityRegistryProvider, antiSsrfService));
    doReturn(true).when(spyDescriptor).isCustomS3RegionCapabilityEnabled();

    CustomS3RegionCapability mockCapability = mock(CustomS3RegionCapability.class);

    CustomS3RegionCapabilityConfiguration mockConfig = mock(CustomS3RegionCapabilityConfiguration.class);
    List<SelectOption> mockRegionsList =
        singletonList(new SelectOption("test-region", "Test Region"));
    when(mockConfig.getRegionsList()).thenReturn(mockRegionsList);
    when(mockCapability.getConfig()).thenReturn(mockConfig);

    CapabilityReference mockCapabilityReference = mock(CapabilityReference.class);
    when(mockCapabilityReference.capabilityAs(CustomS3RegionCapability.class)).thenReturn(mockCapability);

    when(capabilityRegistryProvider.get()).thenReturn(capabilityRegistry);
    when(capabilityRegistry.get(any(Predicate.class))).thenReturn(singletonList(mockCapabilityReference));

    List<SelectOption> regionOptions = spyDescriptor.getRegionOptions();

    assertEquals(mockRegionsList, regionOptions);
  }

  @Test
  void testValidPublicEndpointPasses() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

    Map<String, Object> s3Attributes = new HashMap<>();
    s3Attributes.put("bucket", "bucket");
    s3Attributes.put("endpoint", "https://s3.endpoint.com");

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("s3", s3Attributes);

    config.setName("self");
    config.setAttributes(attributes);

    underTest.validateConfig(config);
  }

  @Test
  void testLocalhostEndpointBlocked() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

    Map<String, Object> s3Attributes = new HashMap<>();
    s3Attributes.put("bucket", "bucket");
    s3Attributes.put("endpoint", "http://localhost:9000");

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("s3", s3Attributes);

    config.setName("self");
    config.setAttributes(attributes);

    doThrow(new ValidationException("loopback address"))
        .when(antiSsrfService)
        .validateHostWithoutCache("localhost");

    assertThrows(ValidationException.class, () -> {
      underTest.validateConfig(config);
    });
  }

  @Test
  void testInvalidEndpointUrlFormat() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

    Map<String, Object> s3Attributes = new HashMap<>();
    s3Attributes.put("bucket", "bucket");
    s3Attributes.put("endpoint", "not a valid url");

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("s3", s3Attributes);

    config.setName("self");
    config.setAttributes(attributes);

    assertThrows(ValidationErrorsException.class, () -> {
      underTest.validateConfig(config);
    });
  }

  private BlobStore mockBlobStore(String name, String type, Map<String, Map<String, Object>> attributes) {
    BlobStore blobStore = mock(BlobStore.class);
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();

    config.setName(name);
    config.setType(type);
    config.setAttributes(attributes);

    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);

    return blobStore;
  }
}
