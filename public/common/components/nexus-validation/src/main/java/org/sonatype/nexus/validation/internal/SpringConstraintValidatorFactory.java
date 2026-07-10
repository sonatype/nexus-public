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
package org.sonatype.nexus.validation.internal;

import java.lang.reflect.Constructor;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allow {@link ConstraintValidator validators} to be enhanced.
 *
 * @since 3.0
 */
@Component
public final class SpringConstraintValidatorFactory
    implements ConstraintValidatorFactory
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final ApplicationContext applicationContext;

  @Autowired
  public SpringConstraintValidatorFactory(final ApplicationContext applicationContext) {
    this.applicationContext = checkNotNull(applicationContext);
  }

  @Override
  public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
    log.trace("Resolving validator instance for type: {}", key);
    try {
      T validator = applicationContext.getBean(key);
      if (validator != null) {
        return validator;
      }
    }
    catch (NoSuchBeanDefinitionException e) {
      log.debug("No bean found in context, trying manual creation");
    }
    return create(key);
  }

  @Override
  public void releaseInstance(final ConstraintValidator<?, ?> instance) {
    // empty
  }

  private static <T extends ConstraintValidator<?, ?>> T create(final Class<T> key) {
    try {
      Constructor<T> ctor = key.getDeclaredConstructor();
      ctor.setAccessible(true);
      return ctor.newInstance();
    }
    catch (Exception e) {
      throw new NoSuchBeanDefinitionException(key);
    }
  }
}
