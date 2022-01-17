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

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mock;

public abstract class BlobStoreOverrideImplTestSupport
    extends TestSupport
{
  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Mock
  protected BlobStoreConfigurationStore configStore;

  protected BlobStoreOverrideImpl underTest;

  @Before
  public void setUp() {
    underTest = new BlobStoreOverrideImpl(configStore);
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
