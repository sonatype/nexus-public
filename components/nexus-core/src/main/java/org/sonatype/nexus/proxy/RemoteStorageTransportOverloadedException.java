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

/**
 * Exception to be thrown in cases when {@link RemoteRepositoryStorage} is unable to make an outbound request because
 * it
 * is overloaded. Reason might be different, but they are usually cases when maximum connection limit is reached, or
 * some pools are depleted. Typically, these happens when given instance of {@link RemoteRepositoryStorage} is
 * overloaded.
 *
 * @author cstamas
 */
public class RemoteStorageTransportOverloadedException
    extends RemoteStorageTransportException
{
  private static final long serialVersionUID = -847618217772014024L;

  public RemoteStorageTransportOverloadedException(final ProxyRepository repository, final String message,
                                                   final Throwable cause)
  {
    super(repository, message, cause);
  }
}
