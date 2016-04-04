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
package org.sonatype.nexus.transaction;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builder of {@link Transactional} specifications for situations (eg. lambdas) where annotations are not allowed.
 *
 * @since 3.0
 */
@SuppressWarnings("rawtypes")
public final class TransactionalBuilder<E extends Exception>
{
  private static final Class[] NOTHING = {};

  static final Transactional DEFAULT_SPEC = new TransactionalImpl(NOTHING, NOTHING, NOTHING);

  private final Supplier<? extends Transaction> db;

  private Class[] commitOn = NOTHING;

  private Class[] retryOn = NOTHING;

  private Class[] swallow = NOTHING;

  private Class<E> throwing;

  TransactionalBuilder(@Nullable final Supplier<? extends Transaction> db) {
    this.db = db;
  }

  /**
   * @see Transactional#commitOn()
   */
  @SafeVarargs
  public final TransactionalBuilder<E> commitOn(Class<? extends Exception>... exceptionTypes) {
    commitOn = deepCheckNotNull(exceptionTypes).clone();
    return this;
  }

  /**
   * @see Transactional#retryOn()
   */
  @SafeVarargs
  public final TransactionalBuilder<E> retryOn(Class<? extends Exception>... exceptionTypes) {
    retryOn = deepCheckNotNull(exceptionTypes).clone();
    return this;
  }

  /**
   * @see Transactional#swallow()
   */
  @SafeVarargs
  public final TransactionalBuilder<E> swallow(Class<? extends Exception>... exceptionTypes) {
    swallow = deepCheckNotNull(exceptionTypes).clone();
    return this;
  }

  @SuppressWarnings("unchecked")
  public final <X extends Exception> TransactionalBuilder<X> throwing(Class<X> exceptionType) {
    throwing = (Class<E>) checkNotNull(exceptionType);
    return (TransactionalBuilder<X>) this;
  }

  /**
   * Calls the given operation in the context of the current {@link Transactional} settings.
   */
  public <T> T call(final Operation<T, E> operation) throws E {
    return Operations.transactional(operation, build(), db, throwing);
  }

  /**
   * Builds a new immutable instance of {@link Transactional} based on the current settings.
   */
  @VisibleForTesting
  Transactional build() {
    if (commitOn.length > 0 || retryOn.length > 0 || swallow.length > 0) {
      return new TransactionalImpl(commitOn, retryOn, swallow);
    }
    return DEFAULT_SPEC;
  }

  /**
   * Checks that the given array and its elements are not null.
   */
  private static <T> T[] deepCheckNotNull(final T[] elements) {
    for (T e : elements) {
      checkNotNull(e);
    }
    return elements;
  }

  /**
   * Implementation of {@link Transactional} that follows the behaviour specified in {@link Annotation}.
   */
  private static final class TransactionalImpl
      implements Transactional
  {
    private final Class[] commitOn;

    private final Class[] retryOn;

    private final Class[] swallow;

    @SuppressWarnings("pmd:ArrayIsStoredDirectly") // as they were already cloned in the builder
    TransactionalImpl(final Class[] commitOn, final Class[] retryOn, final Class[] swallow) {
      this.commitOn = commitOn;
      this.retryOn = retryOn;
      this.swallow = swallow;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return Transactional.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Exception>[] commitOn() {
      return commitOn.clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Exception>[] retryOn() {
      return retryOn.clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Exception>[] swallow() {
      return swallow.clone();
    }

    @Override
    public int hashCode() {
      return (127 * "commitOn".hashCode() ^ Arrays.hashCode(commitOn))
          + (127 * "retryOn".hashCode() ^ Arrays.hashCode(retryOn))
          + (127 * "swallow".hashCode() ^ Arrays.hashCode(swallow));
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Transactional)) {
        return false;
      }

      final Transactional spec = (Transactional) o;
      return Arrays.equals(commitOn, spec.commitOn())
          && Arrays.equals(retryOn, spec.retryOn())
          && Arrays.equals(swallow, spec.swallow());
    }

    @Override
    public String toString() {
      return String.format("@%s(commitOn=%s, retryOn=%s, swallow=%s)", annotationType().getName(),
          Arrays.toString(commitOn), Arrays.toString(retryOn), Arrays.toString(swallow));
    }
  }
}
