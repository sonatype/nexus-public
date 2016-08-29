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

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.apache.commons.jexl3.JexlException;

import static org.sonatype.nexus.selector.JexlSelector.prettyExceptionMsg;

/**
 * Simple validator that will make sure the expression is parseable by jexl engine
 *
 * @since 3.1
 */
@Named
public class JexlExpressionValidator
    extends ComponentSupport
{
  private final ConstraintViolationFactory constraintViolationFactory;

  @Inject
  public JexlExpressionValidator(ConstraintViolationFactory constraintViolationFactory) {
    this.constraintViolationFactory = constraintViolationFactory;
  }

  public void validate(String expression) {
    try {
      new JexlSelector(expression);
    }
    catch (Exception e) {
      String msg = e instanceof JexlException ? prettyExceptionMsg((JexlException) e) : e.getMessage(); //NOSONAR
      log.debug(msg, e);
      throw new ConstraintViolationException(e.getMessage(),
          Collections.singleton(constraintViolationFactory.createViolation("expression", msg)));
    }
  }
}
