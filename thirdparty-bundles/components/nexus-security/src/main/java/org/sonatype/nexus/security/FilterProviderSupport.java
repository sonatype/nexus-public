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
package org.sonatype.nexus.security;

import javax.inject.Provider;
import javax.servlet.Filter;

import com.google.inject.Key;
import com.google.inject.name.Names;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link Filter} providers.
 *
 * @since 2.8
 */
public class FilterProviderSupport
  implements Provider<Filter>
{
  private final Filter filter;

  public FilterProviderSupport(final Filter filter) {
    this.filter = checkNotNull(filter);
  }

  @Override
  public Filter get() {
    return filter;
  }

  public static Key<Filter> filterKey(final String name) {
    return Key.get(Filter.class, Names.named(name));
  }
}
