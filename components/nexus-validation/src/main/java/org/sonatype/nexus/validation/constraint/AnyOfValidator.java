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
package org.sonatype.nexus.validation.constraint;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.validation.ConstraintValidatorSupport;

@Named
public class AnyOfValidator
    extends ConstraintValidatorSupport<AnyOf, String>
{
  Set<String> values;

  String paramName;

  String message;

  public AnyOfValidator() {
  }

  @Override
  public void initialize(final AnyOf annotation) {
    Class<? extends Enum<?>> enumClass = annotation.enumClass();

    paramName = annotation.parameterName();
    message = annotation.message();
    values = Stream.of(enumClass.getEnumConstants())
        .map(Enum::name)
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    boolean isValid = values.contains(value.toLowerCase());

    if (!isValid) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(getEscapeHelper().stripJavaEl(buildMessage()))
          .addConstraintViolation();
    }

    return isValid;
  }

  private String buildMessage() {
    return String.format(message, paramName, String.join(",", values));
  }
}
