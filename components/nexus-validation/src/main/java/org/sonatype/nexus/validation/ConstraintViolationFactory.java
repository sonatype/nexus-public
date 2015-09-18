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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.Constraint;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import javax.validation.ConstraintViolation;
import javax.validation.Payload;
import javax.validation.Validator;

import org.sonatype.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Factory of {@link ConstraintViolation}s to be used (rarely) for manual validation.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ConstraintViolationFactory
    extends ComponentSupport
{
  private final Provider<Validator> validatorProvider;

  @Inject
  public ConstraintViolationFactory(final Provider<Validator> validatorProvider) {
    this.validatorProvider = checkNotNull(validatorProvider);
  }

  /**
   * Create a violation with specified path and message.
   *
   * @param path    violation path
   * @param message violation message
   * @return created violation
   */
  public ConstraintViolation<?> createViolation(final String path, final String message) {
    checkNotNull(path);
    checkNotNull(message);
    return validatorProvider.get().validate(new HelperBean(path, message)).iterator().next();
  }

  /**
   * Bean passing path/message.
   */
  @HelperAnnotation
  private static class HelperBean
  {
    private final String path;

    private final String message;

    public HelperBean(final String path, final String message) {
      this.path = path;
      this.message = message;
    }

    public String getPath() {
      return path;
    }

    public String getMessage() {
      return message;
    }
  }

  /**
   * Annotation to trigger validation.
   *
   * @since 3.0
   */
  @Target({TYPE})
  @Retention(RUNTIME)
  @Constraint(validatedBy = HelperValidator.class)
  @Documented
  private @interface HelperAnnotation
  {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
  }

  /**
   * {@link HelperAnnotation} validator.
   */
  private static class HelperValidator
      extends ConstraintValidatorSupport<HelperAnnotation, HelperBean>
  {
    @Override
    public boolean isValid(final HelperBean bean, final ConstraintValidatorContext context) {
      context.disableDefaultConstraintViolation();

      // build a custom property path
      ConstraintViolationBuilder builder = context.buildConstraintViolationWithTemplate(bean.getMessage());
      NodeBuilderCustomizableContext nodeBuilder = null;
      for (String part : bean.getPath().split("\\.")) {
        if (nodeBuilder == null) {
          nodeBuilder = builder.addPropertyNode(part);
        }
        else {
          nodeBuilder = nodeBuilder.addPropertyNode(part);
        }
      }
      if (nodeBuilder != null) {
        nodeBuilder.addConstraintViolation();
      }

      return false;
    }
  }
}
