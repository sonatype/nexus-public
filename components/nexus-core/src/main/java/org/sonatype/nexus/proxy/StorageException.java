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

import java.io.IOException;

/**
 * Generic storage exception thrown by given storage implementation (like IOExceptions), and so. Denotes a (probably)
 * unrecoverable, serious system and/or IO error. <b>This class is deprecated, and will be removed in future
 * releases!</b> The StorageException was used in more then half of cases to "wrap" an IOException and that did not
 * make
 * any sense. IOException will replace the StorageException usage, but internally, two descendants of IOExceptions,
 * LocalStorageException and RemoteStorageException should be used to "fine tune" Nexus Core behavior.
 *
 * @author cstamas
 * @deprecated Use {@link LocalStorageException} or {@link RemoteStorageException} respectively, or, catch {@link
 *             IOException} in your code as this exception is about to be removed in future releases.
 */
@Deprecated
public class StorageException
    extends IOException
{
  private static final long serialVersionUID = -7119754988039787918L;

  public StorageException(String msg) {
    super(msg);
  }

  public StorageException(String msg, Throwable cause) {
    super(msg);

    initCause(cause);
  }

  public StorageException(Throwable cause) {
    super("A storage exception occured!");

    initCause(cause);
  }
}
