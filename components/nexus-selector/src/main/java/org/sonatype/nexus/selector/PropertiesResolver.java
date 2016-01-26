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
package org.sonatype.nexus.selector;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link VariableResolver} that resolves variables in a {@code namespace} (i.e. {@code "namespace.foo" })
 * to values of type {@code V} based on either {@link Properties} or a {@link Map}.
 * <p>
 * The {@link Properties} or {@link Map} provided at construction is assumed to contain keys that are within
 * the {@code namespace}. If the resolver is expected to resolve {@code "foo.bar"}, then {@code namespace="foo"}
 * and a {@link Properties} or {@link Map} object contain the key {@code "bar"} should be provided to the constructor.
 *
 * @since 3.0
 */
public class PropertiesResolver<V>
    implements VariableResolver
{
  private final String namespace;
  private final Map<String, V> map;
  private final Set<String> variableSet;

  public PropertiesResolver(final String namespace, final Properties properties) {
    this(namespace, propertiesToMap(properties));
  }

  public PropertiesResolver(final String namespace, final Map<String, V> properties) {
    this.namespace = namespace;
    this.map = Collections.unmodifiableMap(
        properties.entrySet().stream()
          .collect(Collectors.<Map.Entry<String,V>,String,V>toMap(e -> namespace + "." + e.getKey(), Map.Entry::getValue)));
    this.variableSet = Collections.unmodifiableSet(this.map.keySet());
  }

  @Override
  public Optional<Object> resolve(final String variable) {
    if (map.containsKey(variable)) {
      return Optional.of(map.get(variable));
    }
    else {
      return Optional.empty();
    }
  }

  /**
   * Get the namespace associated with this properties based resolver.
   *
   * @return the namespace
   */
  public String getNamespace() {
    return namespace;
  }

  @SuppressWarnings("unchecked")
  private static <V> Map<String, V> propertiesToMap(final Properties properties) {
    return properties.entrySet().stream()
      .collect(Collectors.toMap(e -> (String)e.getKey(), e -> (V)e.getValue()));
  }

  @Override
  public Set<String> getVariableSet() {
    return variableSet;
  }
}
