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
package org.sonatype.nexus.repository;

import java.util.function.Predicate;

/**
 * {@link Predicate} that filters repositories according to whether they have a given facet.
 *
 * @since 3.0
 */
public class HasFacet
    implements Predicate<Repository>, com.google.common.base.Predicate<Repository>
{
  private final Class<? extends Facet> type;

  public HasFacet(final Class<? extends Facet> type) {
    this.type = type;
  }

  @Override
  public boolean apply(final Repository input) {
    return test(input);
  }

  @Override
  public boolean test(final Repository input) {
    return input.optionalFacet(type).isPresent();
  }

  public static HasFacet hasFacet(final Class<? extends Facet> type) {
    return new HasFacet(type);
  }
}
