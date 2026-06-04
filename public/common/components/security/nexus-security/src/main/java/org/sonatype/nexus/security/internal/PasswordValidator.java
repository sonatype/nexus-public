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
package org.sonatype.nexus.security.internal;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates passwords against configurable rules.
 *
 * <p>
 * By default, requires passwords to be at least 8 characters (NIST SP 800-63B recommendation).
 * Administrators can customize validation via properties:
 * </p>
 *
 * <ul>
 * <li>{@code nexus.password.validator} - Custom regex pattern (e.g., ".*" to allow any password)</li>
 * <li>{@code nexus.password.validator.message} - Custom error message</li>
 * </ul>
 */
@Component
public class PasswordValidator
{
  /**
   * Default regex requiring minimum 8 characters per NIST SP 800-63B.
   */
  static final String DEFAULT_PASSWORD_REGEX = ".{8,}";

  static final String DEFAULT_ERROR_MESSAGE = "Password must be at least 8 characters";

  static final String CUSTOM_VALIDATOR_ERROR_MESSAGE = "Password does not match corporate policy";

  private final Predicate<String> passwordValidator;

  private final String errorMessage;

  @Autowired
  public PasswordValidator(
      @Nullable @Value("${nexus.password.validator:#{null}}") final String passwordValidator,
      @Nullable @Value("${nexus.password.validator.message:#{null}}") final String errorMessage)
  {
    boolean hasCustomValidator = StringUtils.isNotBlank(passwordValidator);
    String regex = hasCustomValidator ? passwordValidator : DEFAULT_PASSWORD_REGEX;
    this.passwordValidator = Pattern.compile(regex).asPredicate();
    this.errorMessage = determineErrorMessage(errorMessage, hasCustomValidator);
  }

  private static String determineErrorMessage(final String customMessage, final boolean hasCustomValidator) {
    if (StringUtils.isNotBlank(customMessage)) {
      return customMessage;
    }
    return hasCustomValidator ? CUSTOM_VALIDATOR_ERROR_MESSAGE : DEFAULT_ERROR_MESSAGE;
  }

  public void validate(final String password) {
    if (password == null) {
      return;
    }
    if (!passwordValidator.test(password)) {
      throw new ValidationErrorsException(errorMessage);
    }
  }
}
