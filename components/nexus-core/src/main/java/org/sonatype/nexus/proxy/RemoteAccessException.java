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

/**
 * Top level class for all remote related auth/authz problems.
 *
 * @author cstamas
 */
public abstract class RemoteAccessException
    extends RemoteStorageException
{
  private static final long serialVersionUID = 391662938886542734L;

  private final ProxyRepository repository;

  public RemoteAccessException(ProxyRepository repository, String message) {
    this(repository, message, null);
  }

  public RemoteAccessException(ProxyRepository repository, String message, Throwable cause) {
    super(message, cause);

    this.repository = repository;
  }

  public ProxyRepository getRepository() {
    return repository;
  }

}
