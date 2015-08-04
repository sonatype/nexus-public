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
package org.sonatype.nexus.plugins.siesta;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.sisu.siesta.common.validation.ValidationErrorXO;
import org.sonatype.sisu.siesta.server.ValidationErrorsExceptionMappersSupport;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Maps {@link InvalidConfigurationException} to 400 with a list of {@link ValidationErrorXO} as body.
 *
 * @since 2.4
 */
@Named
@Singleton
public class InvalidConfigurationExceptionMapper
    extends ValidationErrorsExceptionMappersSupport<InvalidConfigurationException>
{

  @Override
  protected List<ValidationErrorXO> getValidationErrors(final InvalidConfigurationException exception) {
    final ValidationResponse validationResponse = exception.getValidationResponse();
    if (validationResponse != null) {
      final List<ValidationMessage> validationErrors = validationResponse.getValidationErrors();
      if (validationErrors != null && !validationErrors.isEmpty()) {
        return Lists.transform(validationErrors, new Function<ValidationMessage, ValidationErrorXO>()
        {
          @Nullable
          @Override
          public ValidationErrorXO apply(@Nullable final ValidationMessage validationMessage) {
            if (validationMessage != null) {
              return new ValidationErrorXO(validationMessage.getKey(), validationMessage.getMessage());
            }
            return null;
          }
        });
      }
    }
    return Lists.newArrayList(new ValidationErrorXO(exception.getMessage()));
  }

}
