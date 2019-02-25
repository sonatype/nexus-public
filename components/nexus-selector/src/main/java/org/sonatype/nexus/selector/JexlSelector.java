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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.jexl3.JexlContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.TRUE;

/**
 * {@link Selector} implementation that uses JEXL to evaluate expressions describing the selection criteria.
 *
 * @see <a href="http://commons.apache.org/proper/commons-jexl/">Commons Jexl</a>
 *
 * @since 3.0
 */
public class JexlSelector
    implements Selector
{
  public static final String TYPE = "jexl";

  protected final JexlExpression expression;

  public JexlSelector(final JexlExpression expression) {
    this.expression = checkNotNull(expression);
  }

  @Override
  public boolean evaluate(final VariableSource source) {
    return TRUE.equals(expression.evaluate(asJexlContext(source)));
  }

  @Override
  public void toSql(final SelectorSqlBuilder sqlBuilder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return expression.getParsedText();
  }

  /**
   * Wraps the given {@link VariableSource} so it can be used as a lazy {@link JexlContext}.
   */
  private static JexlContext asJexlContext(final VariableSource source) {
    return new JexlContext()
    {
      private final Set<String> names = source.getVariableSet();

      private final Map<String, Optional<?>> values = new HashMap<>(names.size());

      @Override
      public boolean has(final String name) {
        return names.contains(name);
      }

      @Override
      public Object get(final String name) {
        return values.computeIfAbsent(name, source::get).orElse(null);
      }

      @Override
      public void set(final String name, final Object value) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
