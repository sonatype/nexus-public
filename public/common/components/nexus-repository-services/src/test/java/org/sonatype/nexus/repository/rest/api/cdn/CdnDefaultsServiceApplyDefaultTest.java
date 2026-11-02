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
package org.sonatype.nexus.repository.rest.api.cdn;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.Configuration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdnDefaultsServiceApplyDefaultTest
{
  private final CdnDefaultsService withTrueDefault = new CdnDefaultsService()
  {
    @Override
    public boolean getDefaultForNewRepositories() {
      return true;
    }

    @Override
    public void setDefaultForNewRepositories(final boolean value) {
      /* no-op */ }
  };

  private final CdnDefaultsService withFalseDefault = new CdnDefaultsService()
  {
    @Override
    public boolean getDefaultForNewRepositories() {
      return false;
    }

    @Override
    public void setDefaultForNewRepositories(final boolean value) {
      /* no-op */ }
  };

  @Test
  void appliesDefaultWhenGlobalDistributionAbsent() {
    Configuration config = mockConfigurationWithStorage(new HashMap<>());
    withTrueDefault.applyDefaultIfAbsent(config);

    assertThat(config.attributes("storage").get("globalDistribution"), is(true));
  }

  @Test
  void appliesDefaultWhenGlobalDistributionExplicitlyNull() {
    Map<String, Object> storageMap = new HashMap<>();
    storageMap.put("globalDistribution", null);
    Configuration config = mockConfigurationWithStorage(storageMap);

    withTrueDefault.applyDefaultIfAbsent(config);

    assertThat(config.attributes("storage").get("globalDistribution"), is(true));
  }

  @Test
  void doesNotOverrideExplicitTrue() {
    Map<String, Object> storageMap = new HashMap<>();
    storageMap.put("globalDistribution", true);
    Configuration config = mockConfigurationWithStorage(storageMap);

    withFalseDefault.applyDefaultIfAbsent(config);

    // Explicit true stays true even when the capability default is false.
    assertThat(config.attributes("storage").get("globalDistribution"), is(true));
  }

  @Test
  void doesNotOverrideExplicitFalse() {
    Map<String, Object> storageMap = new HashMap<>();
    storageMap.put("globalDistribution", false);
    Configuration config = mockConfigurationWithStorage(storageMap);

    withTrueDefault.applyDefaultIfAbsent(config);

    // Explicit false stays false even when the capability default is true.
    assertThat(config.attributes("storage").get("globalDistribution"), is(false));
  }

  private static Configuration mockConfigurationWithStorage(final Map<String, Object> storageMap) {
    Configuration config = mock(Configuration.class);
    NestedAttributesMap storageAttrs = new NestedAttributesMap("storage", storageMap);
    when(config.attributes("storage")).thenReturn(storageAttrs);
    return config;
  }
}
