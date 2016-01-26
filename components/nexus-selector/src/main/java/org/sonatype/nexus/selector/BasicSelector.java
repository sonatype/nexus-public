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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A basic {@link Selector} implementation to support Nexus 2 style
 * repository target selection.
 *
 * @since 3.0
 */
public class BasicSelector
    implements Selector
{
  private final Map<String, Pattern> patterns;

  public BasicSelector(final Map<String, String> patterns) {
    Map<String, Pattern> patternMap = patterns.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Pattern.compile(e.getValue())));
    this.patterns = Collections.unmodifiableMap(patternMap);
  }

  @Override
  public boolean evaluate(final VariableSource variableSource) {
    return patterns.entrySet().stream().allMatch(matcher(variableSource));
  }

  private Predicate<Map.Entry<String, Pattern>> matcher(final VariableSource variableSource) {
    return t -> {
      Optional<Object> value = variableSource.get(t.getKey());
      return value.isPresent() && t.getValue().matcher((String) value.get()).matches();
    };
  }
}
