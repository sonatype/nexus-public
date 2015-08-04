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
 * IllegalOperationException is thrown when an illegal action is tried against a ResourceStore (ie. write to a read
 * only,
 * unavailable, etc.). Previously it was (wrongly) AccessDeniedException used to mark these problems, and it caused
 * problems on REST API to distinct an "authz problem = accessDenied = HTTP 401" and "bad request = HTTP 400".
 *
 * @author cstamas
 */
public abstract class IllegalOperationException
    extends Exception
{
  private static final long serialVersionUID = -1075426559861827023L;

  public IllegalOperationException(String message) {
    super(message);
  }

  public IllegalOperationException(String message, Throwable cause) {
    super(message, cause);
  }

  public IllegalOperationException(Throwable cause) {
    super(cause);
  }
}
