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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.security.UserIdHelper;

import com.google.common.collect.Maps;
import jakarta.inject.Provider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public abstract class BlobStoreOverrideImplTestSupport
{
  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Mock
  protected BlobStoreConfigurationStore configStore;

  @Mock
  protected SecretsService secretsService;

  protected BlobStoreDescriptor fileDescriptor;

  protected BlobStoreDescriptor s3Descriptor;

  protected BlobStoreDescriptor azureDescriptor;

  protected BlobStoreDescriptor gcsDescriptor;

  protected List<BlobStoreDescriptor> registeredDescriptors;

  protected Provider<List<BlobStoreDescriptor>> blobStoreDescriptorsProvider;

  protected BlobStoreOverrideImpl underTest;

  protected static final String TEST_USER = "test-user";

  private MockedStatic<UserIdHelper> userIdHelperMockedStatic;

  @Before
  public void setUp() {
    // The override path passes null to encryptMaven directly (Shiro isn't bound at doStart), so
    // this stub is not exercised by BlobStoreOverrideImpl itself. It is retained because subclass
    // tests may exercise descriptor-level paths (e.g. BaseBlobStoreManager#encryptSensitiveAttributes,
    // BaseBlobStoreManager.java:950-951) that still call UserIdHelper.get() from the create/update
    // request-scope where Shiro is bound in production.
    userIdHelperMockedStatic = mockStatic(UserIdHelper.class);
    userIdHelperMockedStatic.when(UserIdHelper::get).thenReturn(TEST_USER);

    fileDescriptor = mock(BlobStoreDescriptor.class, "File");
    s3Descriptor = mock(BlobStoreDescriptor.class, "S3");
    azureDescriptor = mock(BlobStoreDescriptor.class, "Azure Cloud Storage");
    gcsDescriptor = mock(BlobStoreDescriptor.class, "Google Cloud Storage");

    // Default every descriptor to Collections.emptyList() for getSensitiveConfigurationFields()
    // so an unstubbed lookup does not return null and silently pass the isEmpty() guard —
    // that would let a plaintext credential slip past unencrypted in tests. Individual tests
    // override this stub when they need a specific sensitive-field list.
    org.mockito.Mockito.lenient()
        .when(fileDescriptor.getSensitiveConfigurationFields())
        .thenReturn(Collections.emptyList());
    org.mockito.Mockito.lenient()
        .when(s3Descriptor.getSensitiveConfigurationFields())
        .thenReturn(Collections.emptyList());
    org.mockito.Mockito.lenient()
        .when(azureDescriptor.getSensitiveConfigurationFields())
        .thenReturn(Collections.emptyList());
    org.mockito.Mockito.lenient()
        .when(gcsDescriptor.getSensitiveConfigurationFields())
        .thenReturn(Collections.emptyList());

    registeredDescriptors = new ArrayList<>();
    registeredDescriptors.add(fileDescriptor);
    registeredDescriptors.add(s3Descriptor);
    registeredDescriptors.add(azureDescriptor);
    registeredDescriptors.add(gcsDescriptor);

    blobStoreDescriptorsProvider = () -> registeredDescriptors;

    underTest = new BlobStoreOverrideImpl(configStore, secretsService, blobStoreDescriptorsProvider);
  }

  @After
  public void tearDown() {
    if (userIdHelperMockedStatic != null) {
      userIdHelperMockedStatic.close();
    }
  }

  protected BlobStoreConfiguration defaultConfig() {
    BlobStoreConfiguration config = createConfig("default", "File");
    config.getAttributes().get("file").put("path", "default");
    return config;
  }

  protected BlobStoreConfiguration createConfig(final String name, final String type) {
    Map<String, Map<String, Object>> attributes = Maps.newHashMap();
    attributes.put(toConfigKey(type), Maps.newHashMap());
    BlobStoreConfigurationData config = new BlobStoreConfigurationData();
    config.setName(name);
    config.setType(type);
    config.setAttributes(attributes);
    return config;
  }

  protected static String toConfigKey(final String type) {
    return type.toLowerCase();
  }

}
