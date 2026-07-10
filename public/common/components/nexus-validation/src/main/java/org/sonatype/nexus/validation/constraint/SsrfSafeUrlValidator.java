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

import java.net.URI;

import org.sonatype.nexus.validation.ConstraintValidatorSupport;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Validates that a URL follows configuration for accessing private/local network address.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
public class SsrfSafeUrlValidator
    extends ConstraintValidatorSupport<SsrfSafeUrl, URI>
{
  private final AntiSsrfService antiSsrfService;

  @Autowired
  public SsrfSafeUrlValidator(final AntiSsrfService antiSsrfService) {
    this.antiSsrfService = checkNotNull(antiSsrfService);
  }

  @Override
  public boolean isValid(final URI url, final ConstraintValidatorContext context) {
    if (url == null) {
      return true;
    }

    String host = url.getHost();
    if (host == null || host.isBlank()) {
      return true;
    }

    try {
      antiSsrfService.validateHostWithoutCache(host);
      return true;
    }
    catch (ValidationException e) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(e.getMessage())
          .addConstraintViolation();
      return false;
    }
  }
}
