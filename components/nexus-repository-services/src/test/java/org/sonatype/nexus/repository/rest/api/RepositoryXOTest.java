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
package org.sonatype.nexus.repository.rest.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class RepositoryXOTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  private final String name;

  private final Format format;

  private final String expectedFormat;

  private final Type type;

  private final String expectedType;

  private final String url;

  private final Map<String, Object> attributes;

  private final Map<String, Map<String, Object>> expectedAttributes;

  public RepositoryXOTest(
      final String name,
      final Format format,
      final String expectedFormat,
      final Type type,
      final String expectedType,
      final String url,
      final Map<String, Object> attributes,
      final Map<String, Map<String, Object>> expectedAttributes)
  {
    this.name = name;
    this.format = format;
    this.expectedFormat = expectedFormat;
    this.type = type;
    this.expectedType = expectedType;
    this.url = url;
    this.attributes = attributes;
    this.expectedAttributes = expectedAttributes;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {
            "x", format("npm"), "npm", new ProxyType(), "proxy", "u", Map.of("remoteUrl", "url"),
            Map.of("proxy", Map.of("remoteUrl", "url"))
        },
        {"y", format("maven"), "maven", new HostedType(), "hosted", "u", Map.of("remoteUrl", "foo"), Map.of()},
        {"z", format("nuget"), "nuget", new GroupType(), "group", "u", Map.of("remoteUrl", "foo"), Map.of()}
    });
  }

  @Test
  public void testConvertRepositoryToRepositoryXO() {
    when(repository.getName()).thenReturn(name);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
    when(repository.getUrl()).thenReturn(url);

    Configuration mockConfiguration = configuration(type.getValue(), attributes);
    when(repository.getConfiguration()).thenReturn(mockConfiguration);

    RepositoryXO repositoryXO = RepositoryXO.fromRepository(repository);

    assertThat(repositoryXO.getName(), is(name));
    assertThat(repositoryXO.getFormat(), is(expectedFormat));
    assertThat(repositoryXO.getType(), is(expectedType));
    assertThat(repositoryXO.getUrl(), is(url));
    assertThat(repositoryXO.getAttributes(), is(expectedAttributes));
  }

  private static Format format(final String value) {
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn(value);
    return format;
  }

  private Configuration configuration(final String type, final Map<String, Object> value) {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getAttributes()).thenReturn(Map.of(type, value));
    when(configuration.attributes(type)).thenReturn(new NestedAttributesMap(type, value));
    return configuration;
  }
}
