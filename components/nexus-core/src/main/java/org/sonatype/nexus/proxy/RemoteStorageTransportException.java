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
package org.sonatype.nexus.proxy;

import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;

import com.google.common.base.Preconditions;

/**
 * Top level remote storage exception for cases when outbound request is unsuccessful, it failed, but not due to remote
 * party (or interaction with it), but due to some other circumstance, like {@link RemoteRepositoryStorage} transport
 * problems, pools depleted etc.
 *
 * @author cstamas
 * @since 2.2
 */
public abstract class RemoteStorageTransportException
    extends RemoteStorageException
{
  private static final long serialVersionUID = 8752853110808887464L;

  private final ProxyRepository repository;

  /**
   * Constructor.
   */
  public RemoteStorageTransportException(final ProxyRepository repository, final String message,
                                         final Throwable cause)
  {
    super(message, cause);
    this.repository = Preconditions.checkNotNull(repository);
  }

  /**
   * Returns the involved proxy repository. Never returns {@code null}.
   *
   * @return the involved proxy repository.
   */
  public ProxyRepository getRepository() {
    return repository;
  }
}
