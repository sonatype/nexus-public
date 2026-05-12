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
package org.sonatype.nexus.repository.maven.rest;

import java.util.Arrays;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.rest.api.model.GroupAttributes;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MavenGroupRepositoryApiRequestToConfigurationConverter}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class MavenGroupRepositoryApiRequestToConfigurationConverterTest

{
  @Mock
  private ConfigurationStore configurationStore;

  private MavenGroupRepositoryApiRequestToConfigurationConverter underTest;

  @Before
  public void setUp() {
    when(configurationStore.newConfiguration()).thenReturn(new ConfigurationData());
    underTest = new MavenGroupRepositoryApiRequestToConfigurationConverter();
    underTest.setConfigurationStore(configurationStore);
  }

  @Test
  public void testConvert_groupAttributesSet() {
    MavenGroupRepositoryApiRequest request = createRequest();
    Configuration config = underTest.convert(request);

    assertThat(config, notNullValue());
    assertThat(config.attributes("group").get("memberNames"), notNullValue());
  }

  private MavenGroupRepositoryApiRequest createRequest() {
    StorageAttributes storage = new StorageAttributes("default", true);
    GroupAttributes group = new GroupAttributes(Arrays.asList("maven-hosted", "maven-central"));

    return new MavenGroupRepositoryApiRequest(
        "maven-group",
        true,
        storage,
        group);
  }
}
