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

import com.google.common.base.Preconditions;

/**
 * Remote storage exception for cases when outbound request is unsuccessful, due to remote peer hang up on us
 * unexpectedly.
 * Main use of this exception is to "translate" various EOF notification for various Remote Repository Storage
 * implementations
 * into single exception handled by Nexus Core.
 *
 * @author cstamas
 * @since 2.4
 */
public class RemoteStorageEOFException
    extends RemoteStorageException
{
  private final ProxyRepository repository;

  /**
   * Constructor.
   */
  public RemoteStorageEOFException(final ProxyRepository repository, final String message, final Throwable cause) {
    super(message, cause);
    this.repository = Preconditions.checkNotNull(repository);
  }

  /**
   * Constructor.
   */
  public RemoteStorageEOFException(final ProxyRepository repository, final Throwable cause) {
    this(repository, cause.getMessage(), cause);
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
