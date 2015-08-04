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

/**
 * Thrown when a request is denied by Nexus for security reasons. This exception should be ALWAYS considered as
 * "authorization denied" type of stuff, since Nexus does not deal with authentication. Simply taken, this exception is
 * thrown for lack of permissions of the already authenticated subject.
 *
 * @author cstamas
 */
public class AccessDeniedException
    extends AuthorizationException
{
  private static final long serialVersionUID = 8341250956517740603L;

  private final ResourceStoreRequest request;

  public AccessDeniedException(String msg) {
    super(msg);

    this.request = null;
  }

  public AccessDeniedException(ResourceStoreRequest request, String msg) {
    super(msg);

    this.request = request;
  }

  /**
   * The RepositoryItemUid that is forbidden to access.
   */
  public ResourceStoreRequest getResourceStoreRequest() {
    return this.request;
  }
}
