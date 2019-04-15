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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ConstraintViolationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.jexl3.JexlException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.common.text.Strings2.upper;
import static org.sonatype.nexus.selector.CselValidator.validateCselExpression;
import static org.sonatype.nexus.selector.JexlEngine.expandExceptionDetail;

/**
 * Factory for validating and creating selectors.
 *
 * @since 3.16
 */
@Named
@Singleton
public class SelectorFactory
    extends ComponentSupport
{
  private final JexlEngine jexlEngine = new JexlEngine();

  private final ConstraintViolationFactory constraintViolationFactory;

  @Inject
  public SelectorFactory(final ConstraintViolationFactory constraintViolationFactory) {
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
  }

  /**
   * Checks the given expression is valid for the chosen selector type.
   *
   * @throws ConstraintViolationException if the expression is invalid
   */
  public void validateSelector(final String type, final String expression) {
    try {
      switch (type) {
        case JexlSelector.TYPE:
          jexlEngine.parseExpression(expression);
          break;
        case CselSelector.TYPE:
          validateCselExpression(jexlEngine.parseExpression(expression));
          break;
        default:
          throw new IllegalArgumentException("Unknown selector type: " + type);
      }
    }
    catch (Exception e) {
      String detail = format("Invalid %s: %s", upper(type),
          e instanceof JexlException ? expandExceptionDetail((JexlException) e) : e.getMessage());

      log.debug(detail, e);

      throw new ConstraintViolationException(e.getMessage(),
          ImmutableSet.of(constraintViolationFactory.createViolation("expression", detail)));
    }
  }

  /**
   * Creates a new {@link Selector} for the given expression and type.
   */
  public Selector createSelector(final String type, final String expression) {
    switch (type) {
      case JexlSelector.TYPE:
        return new JexlSelector(jexlEngine.buildExpression(expression));
      case CselSelector.TYPE:
        return new CselSelector(jexlEngine.buildExpression(expression));
      default:
        throw new IllegalArgumentException("Unknown selector type: " + type);
    }
  }
}
