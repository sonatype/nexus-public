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
package org.sonatype.nexus.proxy.events;

import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

/**
 * The superclass event of ProxyReposiory ProxyMode related events.
 *
 * @author cstamas
 */
public abstract class RepositoryEventProxyMode
    extends RepositoryEvent
{
  private final ProxyMode oldProxyMode;

  private final ProxyMode newProxyMode;

  private final Throwable cause;

  public RepositoryEventProxyMode(final ProxyRepository repository, final ProxyMode oldProxyMode,
                                  final ProxyMode newProxyMode, final Throwable cause)
  {
    super(repository);

    this.oldProxyMode = oldProxyMode;

    this.newProxyMode = newProxyMode;

    this.cause = cause;
  }

  public ProxyMode getOldProxyMode() {
    return oldProxyMode;
  }

  public ProxyMode getNewProxyMode() {
    return newProxyMode;
  }

  public Throwable getCause() {
    return cause;
  }

  public ProxyRepository getRepository() {
    return (ProxyRepository) super.getRepository();
  }
}
