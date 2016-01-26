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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link VariableResolver} that resolves a single variable, or a set
 * of variables, to a single constant value.
 *
 * @since 3.0
 */
public class ConstantVariableResolver
    implements VariableResolver
{
  private final Set<String> variables;
  private final Object value;

  public ConstantVariableResolver(final Object value, final String... variables) {
    this.variables = Collections.unmodifiableSet(Arrays.stream(variables).collect(Collectors.toSet()));
    this.value = value;
  }

  @Override
  public Optional<Object> resolve(final String variable) {
    if (variables.contains(variable)) {
      return Optional.of(value);
    }
    else {
      return Optional.empty();
    }
  }

  @Override
  public Set<String> getVariableSet() {
    return variables;
  }

  /**
   * Create a {@link VariableSource} with a single {@link ConstantVariableResolver} resolver.
   *
   * @param value to return when resolving variables
   * @param variables to resolve
   * @return variable source
   */
  public static VariableSource sourceFor(final Object value, final String... variables) {
    return new VariableSourceBuilder().addResolver(new ConstantVariableResolver(value, variables)).build();
  }
}
