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
package org.sonatype.nexus.repository.rest.cma;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.sonatype.nexus.repository.storage.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Utils for working with {@link Component} responses in the REST API.
 *
 * @since 3.9
 */
public class ComponentResponseUtils
{
  private static final String NAME = "name";

  private static final String GROUP = "group";

  private static final String VERSION = "version";

  private ComponentResponseUtils() {
    // empty
  }

  /**
   * Create a simple map to use in a {@link org.sonatype.nexus.rest.SimpleApiResponse} from the given {@link Component}.
   *
   * @param component the {@link Component} to produce the map from
   * @return Map of component attributes. Will always contain the name, and optionally the group and version if they exist.
   */
  public static Map<String, String> mapFor(Component component) {
    Builder<String, String> builder = ImmutableMap.<String, String>builder().put(NAME, component.name());
    maybePut(builder, component, Component::group, GROUP);
    maybePut(builder, component, Component::version, VERSION);
    return builder.build();
  }

  public static <T> void maybePut(final Builder<String, String> builder,
                                  final T t,
                                  final Function<T, String> f,
                                  final String key)
  {
    Optional.of(t).map(f).ifPresent(v -> builder.put(key, v));
  }
}
