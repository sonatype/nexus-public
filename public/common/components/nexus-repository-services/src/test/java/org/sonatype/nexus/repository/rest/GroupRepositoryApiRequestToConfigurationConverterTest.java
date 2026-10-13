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
package org.sonatype.nexus.repository.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.SimpleConfiguration;
import org.sonatype.nexus.repository.rest.api.model.GroupAttributes;
import org.sonatype.nexus.repository.rest.api.model.GroupRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.GLOBAL_DISTRIBUTION;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STRICT_CONTENT_TYPE_VALIDATION;

@ExtendWith(MockitoExtension.class)
class GroupRepositoryApiRequestToConfigurationConverterTest
{
  @Mock
  ConfigurationStore configurationStore;

  GroupRepositoryApiRequestToConfigurationConverter<GroupRepositoryApiRequest> underTest =
      new GroupRepositoryApiRequestToConfigurationConverter<>();

  @BeforeEach
  void setup() {
    SimpleConfiguration config = new SimpleConfiguration();
    config.setAttributes(new HashMap<>());
    when(configurationStore.newConfiguration()).thenReturn(config);
    underTest.setConfigurationStore(configurationStore);
  }

  @Test
  void testConvert() {
    GroupRepositoryApiRequest request = new GroupRepositoryApiRequest("maven-central", "maven2", true,
        new StorageAttributes("my-blobstore", true, false), new GroupAttributes(List.of("a-member", "b-member")));

    Configuration configuration = underTest.convert(request);

    assertThat(configuration.getRepositoryName(), is("maven-central"));
    assertThat(configuration.getRecipeName(), is("maven2-group"));
    assertThat(configuration.isOnline(), is(true));

    assertThat(configuration.getAttributes(), allOf(hasKey(STORAGE), hasKey("group")));

    assertThat(configuration.attributes("group").backing(), is(Map.of("memberNames", List.of("a-member", "b-member"))));
    assertThat(configuration.attributes(STORAGE).backing(),
        is(Map.of(BLOB_STORE_NAME, "my-blobstore", STRICT_CONTENT_TYPE_VALIDATION, true, GLOBAL_DISTRIBUTION, false,
            "dataStoreName", "nexus")));
  }
}
