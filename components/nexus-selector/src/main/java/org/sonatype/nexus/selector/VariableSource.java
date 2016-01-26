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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A source of values for variables based on an ordered list or {@link VariableResolver}'s.
 *
 * @since 3.0
 */
public class VariableSource
{
  private final List<VariableResolver> resolvers;
  private final Set<String> variableSet;

  public VariableSource(final List<VariableResolver> resolvers) {
    this.resolvers = Collections.unmodifiableList(resolvers);
    Set<String> variables = resolvers.stream()
      .map(VariableResolver::getVariableSet)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());
    this.variableSet = Collections.unmodifiableSet(variables);
  }

  /**
   * Get an {@link Optional} value for a given {@code variable}.
   *
   * @param variable for which to get the value
   * @return optional value
   */
  public Optional<Object> get(final String variable) {
    return resolvers.stream()
      .map(vr -> vr.resolve(variable))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst();
  }

  /**
   * Get the superset of variables that are resolvable by the list of resolvers.
   *
   * @return the set of resolvable variables
   */
  public Set<String> getVariableSet() {
    return variableSet;
  }
}
