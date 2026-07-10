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
package org.sonatype.nexus.bootstrap.validation;

import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.executable.ExecutableValidator;

import org.sonatype.nexus.validation.internal.AlwaysTraversableResolver;
import org.sonatype.nexus.validation.internal.AopAwareParanamerParameterNameProvider;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ValidationConfiguration
{
  /**
   * Token class used to preload validation message interpolation.
   */
  static class PreloadToken
  {
    @NotNull(message = "{org.sonatype.nexus.validation.constraint.notnull}")
    String empty;
  }

  /*
   * For use by ValidationAspect
   */
  public static ExecutableValidator EXECUTABLE_VALIDATOR = null;

  @Bean
  public ValidatorFactory validatorFactory(final ConstraintValidatorFactory constraintValidatorFactory) {
    ValidatorFactory factory = Validation.byDefaultProvider()
        .configure()
        .constraintValidatorFactory(constraintValidatorFactory)
        .parameterNameProvider(new AopAwareParanamerParameterNameProvider())
        .traversableResolver(new AlwaysTraversableResolver())
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory();

    // FIXME: Install custom MessageInterpolator that can properly find/merge ValidationMessages.properties for bundles

    // exercise interpolator to preload elements (avoids issues later when TCCL might be different)
    factory.getValidator().validate(new PreloadToken());

    return factory;
  }

  @Primary
  @Bean
  public Validator validator(final ValidatorFactory validatorFactory) {
    Validator validator = validatorFactory.getValidator();
    EXECUTABLE_VALIDATOR = validator.forExecutables();
    return validator;
  }
}
