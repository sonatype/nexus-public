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
package org.sonatype.nexus.proxy.access;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Interface for access manager.
 *
 * @author t.cservenak
 */
public interface AccessManager
{
  /**
   * Key used for authenticated username in request.
   */
  String REQUEST_USER = "request.user";

  /**
   * Key used for request source address.
   */
  String REQUEST_REMOTE_ADDRESS = "request.address";

  /**
   * Key used to mark is the request coming over confidential channel (https).
   */
  String REQUEST_CONFIDENTIAL = "request.isConfidential";

  /**
   * Key used to mark the request certificates of confidential channel (https).
   */
  String REQUEST_CERTIFICATES = "request.certificates";

  /**
   * Key used to mark a request as already authorized, if set, no authorization will be performed
   */
  String REQUEST_AUTHORIZED = "request.authorized";

  /**
   * Key used for authenticated user agent in request.
   */
  String REQUEST_AGENT = "request.agent";

  /**
   * The implementation of this method should throw AccessDeniedException or any subclass if it denies access.
   *
   * @throws AccessDeniedException the access denied exception
   */
  void decide(Repository repository, ResourceStoreRequest request, Action action)
      throws AccessDeniedException;
}
