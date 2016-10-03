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

import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.common.text.Strings2;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.MapContext;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * {@link Selector} implementation that uses JEXL to evaluate expressions describing the selection criteria.
 *
 * @see <a href="http://commons.apache.org/proper/commons-jexl/">Commons Jexl</a>
 * @since 3.0
 */
public class JexlSelector
    implements Selector
{
  // this stops JEXL from using expensive new Throwable().getStackTrace() to find caller info
  private static final JexlInfo CALLER_INFO = new JexlInfo(JexlSelector.class.getName(), 0, 0);

  private static final JexlEngine engine = new JexlBuilder().create();

  private final Optional<JexlExpression> expression;

  public JexlSelector(final String expression) {
    this.expression = isNullOrEmpty(expression) ? Optional.<JexlExpression>empty()
        : Optional.of(engine.createExpression(CALLER_INFO, expression));
  }

  @Override
  public boolean evaluate(final VariableSource variableSource) {
    if (expression.isPresent()) {
      Set<String> vars = variableSource.getVariableSet();
      JexlContext jc = new MapContext();

      // load the values, if present, into the context
      vars.forEach(variable -> variableSource.get(variable).ifPresent(value -> jc.set(variable, value)));

      Object o = expression.get().evaluate(jc);

      return (o instanceof Boolean) ? (Boolean) o : false;
    }
    else {
      return true;
    }
  }

  public static String prettyExceptionMsg(JexlException e) {
    JexlInfo info = e.getInfo();
    if (info != null) {
      String detail = e.getMessage();
      if (!Strings2.isBlank(detail)) {
        detail = e.getMessage().substring(detail.indexOf('\'') + 1, detail.lastIndexOf('\''));
      }
      String parseMsg = detail.isEmpty() ? detail : String.format(" Error parsing string: '%s'.", detail);
      return String.format("Invalid JEXL at line '%s' column '%s'.%s", info.getLine(), info.getColumn(), parseMsg);
    }
    else {
      return e.getMessage();
    }
  }

  @Override
  public String toString() {
    return expression.isPresent() ? expression.get().getParsedText() : "";
  }
}
