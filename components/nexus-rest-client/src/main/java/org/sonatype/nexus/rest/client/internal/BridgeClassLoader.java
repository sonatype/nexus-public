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
package org.sonatype.nexus.rest.client.internal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ClassLoader which bridges two classloaders into a single classloader view.
 *
 * @since 3.2.1
 */
class BridgeClassLoader
    extends ClassLoader
{
  private final ClassLoader secondary;

  public BridgeClassLoader(final ClassLoader primary, final ClassLoader secondary) {
    super(checkNotNull(primary));
    this.secondary = checkNotNull(secondary);
  }

  @Override
  protected Class<?> findClass(final String name) throws ClassNotFoundException {
    return secondary.loadClass(name);
  }
}
