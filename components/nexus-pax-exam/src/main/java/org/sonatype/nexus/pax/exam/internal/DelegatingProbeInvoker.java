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
package org.sonatype.nexus.pax.exam.internal;

import java.util.function.Supplier;

import org.ops4j.pax.exam.ProbeInvoker;

/**
 * Delegates to the supplied {@link ProbeInvoker}.
 * 
 * @since 3.10
 */
public class DelegatingProbeInvoker
    implements ProbeInvoker
{
  private final Supplier<ProbeInvoker> supplier;

  private volatile ProbeInvoker delegate;

  public DelegatingProbeInvoker(final Supplier<ProbeInvoker> supplier) {
    this.supplier = supplier;
  }

  @Override
  public void call(final Object... args) {
    waitForDelegate().call(args);
  }

  private ProbeInvoker waitForDelegate() {
    if (delegate == null) {
      synchronized (this) {
        if (delegate == null) {
          delegate = supplier.get();
        }
      }
    }
    return delegate;
  }
}
