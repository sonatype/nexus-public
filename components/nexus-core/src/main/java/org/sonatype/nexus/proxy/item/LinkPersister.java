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
package org.sonatype.nexus.proxy.item;

import java.io.IOException;
import java.io.OutputStream;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;

/**
 * Component handling links, and their persistence to storage. This component does not handle metadata, only the link
 * content solely. How to represent links on storage may vary, and has nothing to do where is storage located or what
 * technology is used for it. Hence, this component helps to handle links in storage independent way, and separates
 * that
 * logic from LocalRepositoryStorage implementations. Some implementation may still handle the problem in alternate
 * ways
 * (not using this component), but the default is given here.
 *
 * @author cstamas
 */
public interface LinkPersister
{
  /**
   * Uses ContentLocator to inspect the supplied content and decide whether the content holds a "serialized" link
   * form
   * or not.
   */
  boolean isLinkContent(final ContentLocator locator)
      throws IOException;

  /**
   * Reads the link content "serialized" form, and creates the UID of the target, if possible.
   */
  RepositoryItemUid readLinkContent(final ContentLocator locator)
      throws NoSuchRepositoryException, IOException;

  /**
   * Writes "serialized" form of the link into the supplied output stream. It does flush if write is succesful. It
   * will always try to close the output stream, even in case of an IOException.
   */
  void writeLinkContent(StorageLinkItem link, OutputStream os)
      throws IOException;
}
