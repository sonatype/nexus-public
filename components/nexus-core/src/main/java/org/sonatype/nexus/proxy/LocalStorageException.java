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
 * Generic local storage exception thrown by given storage implementation (more special than generic IOExceptions), and
 * so. Denotes a (probably) unrecoverable, serious system and/or IO error that needs some Core action to manage it.
 *
 * @author cstamas
 */
public class LocalStorageException
    extends StorageException
{
  private static final long serialVersionUID = -5815995204584266723L;

  public LocalStorageException(String msg) {
    super(msg);
  }

  public LocalStorageException(String msg, Throwable cause) {
    super(msg, cause);
  }

  public LocalStorageException(Throwable cause) {
    super(cause.getMessage(), cause);
  }
}
