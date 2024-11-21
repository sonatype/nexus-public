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

/**
 * Implementation of {@link Transactional} that follows the behaviour specified in {@link Annotation}.
 *
 * @since 3.2
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class TransactionalImpl
    implements Transactional
{
  private final String reason;

  private final Class[] commitOn;

  private final Class[] retryOn;

  private final Class[] swallow;

  private final TransactionIsolation isolation;

  @SuppressWarnings("pmd:ArrayIsStoredDirectly") // we assume these are safe to store
  TransactionalImpl(
      final String reason,
      final Class[] commitOn,
      final Class[] retryOn,
      final Class[] swallow,
      final TransactionIsolation isolation)
  {
    this.reason = reason;
    this.commitOn = commitOn;
    this.retryOn = retryOn;
    this.swallow = swallow;
    this.isolation = isolation;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return Transactional.class;
  }

  @Override
  public String reason() {
    return reason;
  }

  @Override
  public Class<? extends Exception>[] commitOn() {
    return commitOn;
  }

  @Override
  public Class<? extends Exception>[] retryOn() {
    return retryOn;
  }

  @Override
  public Class<? extends Exception>[] swallow() {
    return swallow;
  }

  @Override
  public TransactionIsolation isolation() {
    return isolation;
  }

  @Override
  public int hashCode() {
    return (127 * "reason".hashCode() ^ reason.hashCode())
        + (127 * "commitOn".hashCode() ^ Arrays.hashCode(commitOn))
        + (127 * "retryOn".hashCode() ^ Arrays.hashCode(retryOn))
        + (127 * "swallow".hashCode() ^ Arrays.hashCode(swallow))
        + (127 * "isolation".hashCode() ^ isolation.hashCode());
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
    return reason.equals(spec.reason())
        && Arrays.equals(commitOn, spec.commitOn())
        && Arrays.equals(retryOn, spec.retryOn())
        && Arrays.equals(swallow, spec.swallow())
        && isolation == spec.isolation();

  }

  @Override
  public String toString() {
    return String.format("@%s(reason=%s, commitOn=%s, retryOn=%s, swallow=%s, isolation=%s)",
        annotationType().getName(),
        reason,
        Arrays.toString(commitOn),
        Arrays.toString(retryOn),
        Arrays.toString(swallow),
        isolation);
  }
}
