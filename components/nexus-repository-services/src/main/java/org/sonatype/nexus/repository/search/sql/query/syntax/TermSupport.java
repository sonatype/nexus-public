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
package org.sonatype.nexus.repository.search.sql.query.syntax;

import java.util.Objects;

/**
 * Support class for {@link Term} implementations which contain a single value.
 */
public abstract class TermSupport<T>
    implements Term
{
  private final T term;

  protected TermSupport(final T term) {
    this.term = term;
  }

  public T get() {
    return term;
  }

  @Override
  public int hashCode() {
    return Objects.hash(term);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TermSupport<?> other = (TermSupport<?>) obj;
    return Objects.equals(term, other.term);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [" + term + "]";
  }
}
