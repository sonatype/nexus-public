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
import java.net.URISyntaxException;

import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

/**
 * Validates a url.
 *
 * @since 3.13
 */
public class UrlStringValidator
    extends ConstraintValidatorSupport<UrlString, String>
{
  private static final UrlValidator urlValidator = new UrlValidator();

  @Override
  public boolean isValid(final String url, final ConstraintValidatorContext constraintValidatorContext) {
    if (Strings2.isBlank(url)) {
      return true;
    }

    try {
      URI uri = new URI(url);
      return urlValidator.isValid(uri, constraintValidatorContext);
    }
    catch (URISyntaxException ignored) {
      return false;
    }
  }
}
