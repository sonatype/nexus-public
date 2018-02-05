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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.inject.Key;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allow {@link ConstraintValidator validators} to be enhanced by Guice.
 *
 * @since 3.0
 */
@Singleton
public final class GuiceConstraintValidatorFactory
    extends ComponentSupport
    implements ConstraintValidatorFactory
{
  private final BeanLocator beanLocator;

  @Inject
  public GuiceConstraintValidatorFactory(final BeanLocator beanLocator) {
    this.beanLocator = checkNotNull(beanLocator);
  }

  public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
    log.trace("Resolving validator instance for type: {}", key);
    return beanLocator.locate(Key.get(key)).iterator().next().getValue();
  }

  @Override
  public void releaseInstance(final ConstraintValidator<?, ?> instance) {
    // empty
  }
}
