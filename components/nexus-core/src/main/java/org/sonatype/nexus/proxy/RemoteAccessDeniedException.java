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
 * Thrown when a request is denied by remote peer for security reasons (ie. HTTP RemoteRepositoryStorage gets 403
 * response code).
 *
 * @author cstamas
 */
public class RemoteAccessDeniedException
    extends RemoteAccessException
{
  private static final long serialVersionUID = -4719375204384900503L;

  private final String url;

  public RemoteAccessDeniedException(ProxyRepository repository, String url, String message) {
    this(repository, url, message, null);
  }

  public RemoteAccessDeniedException(ProxyRepository repository, String url, String message, Throwable cause) {
    super(repository, message, cause);

    this.url = url;
  }

  public String getRemoteUrl() {
    return url;
  }

}
