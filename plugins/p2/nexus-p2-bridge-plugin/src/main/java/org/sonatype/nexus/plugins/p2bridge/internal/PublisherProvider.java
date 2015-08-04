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
package org.sonatype.nexus.plugins.p2bridge.internal;

import javax.inject.Inject;
import javax.inject.Provider;

import org.sonatype.p2.bridge.Publisher;

//@Named
//@Singleton
public class PublisherProvider
    implements Provider<Publisher>
{

  private final P2Runtime p2Runtime;

  private Publisher service;

  @Inject
  public PublisherProvider(final P2Runtime p2Runtime) {
    this.p2Runtime = p2Runtime;
  }

  @Override
  public Publisher get() {
    if (service == null) {
      service = p2Runtime.get().getService(Publisher.class);
    }
    return service;
  }

}
