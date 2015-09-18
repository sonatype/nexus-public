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
package org.sonatype.nexus.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link ConstraintViolation} helpers.
 *
 * @since 3.0
 */
public class ConstraintViolations
{
  private static final Logger log = LoggerFactory.getLogger(ConstraintViolations.class);

  /**
   * Propagate {@link ConstraintViolationException} if there are any violations.
   *
   * @throws ConstraintViolationException
   */
  public static void maybePropagate(final Set<? extends ConstraintViolation<?>> violations, final Logger log) {
    checkNotNull(violations);
    checkNotNull(log);

    if (!violations.isEmpty()) {
      String message = String.format("Validation failed; %d constraints violated", violations.size());

      if (log.isWarnEnabled()) {
        StringBuilder buff = new StringBuilder();
        int c = 0;
        for (ConstraintViolation<?> violation : violations) {
          buff.append("  ").append(++c).append(") ")
              .append(violation.getMessage())
              .append(", type: ")
              .append(violation.getRootBeanClass())
              .append(", property: ")
              .append(violation.getPropertyPath())
              .append(", value: ")
              .append(violation.getInvalidValue())
              .append(System.lineSeparator());
        }
        log.warn("{}:{}{}", message, System.lineSeparator(), buff);
      }

      throw new ConstraintViolationException(message, violations);
    }
  }

  /**
   * Propagate {@link ConstraintViolationException} if there are any violations.
   *
   * @throws ConstraintViolationException
   */
  public static void maybePropagate(final Set<? extends ConstraintViolation<?>> violations) {
    maybePropagate(violations, log);
  }
}
