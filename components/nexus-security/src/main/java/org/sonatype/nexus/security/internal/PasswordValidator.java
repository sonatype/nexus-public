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

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.sonatype.nexus.rest.ValidationErrorsException;

/**
 * @since 3.25
 */
@Named
@Singleton
public class PasswordValidator
{
  private final Predicate<String> passwordValidator;

  private final String errorMessage;

  @Inject
  public PasswordValidator(
      @Nullable @Named("nexus.password.validator") final String passwordValidator,
      @Nullable @Named("nexus.password.validator.message") final String errorMessage)
  {
    this.passwordValidator =
        Optional.ofNullable(passwordValidator).map(Pattern::compile).map(Pattern::asPredicate).orElse(pw -> true);
    this.errorMessage = StringUtils.defaultIfBlank(errorMessage, "Password does not match corporate policy");
  }

  public void validate(final String password) {
    if (!passwordValidator.test(password)) {
      throw new ValidationErrorsException(errorMessage);
    }
  }
}
