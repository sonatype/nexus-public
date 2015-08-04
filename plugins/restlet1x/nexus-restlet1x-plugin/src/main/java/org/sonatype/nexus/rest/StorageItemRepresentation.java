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
package org.sonatype.nexus.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.sonatype.nexus.proxy.item.StorageItem;

import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

public class StorageItemRepresentation
    extends OutputRepresentation
{
  private final StorageItem item;

  public StorageItemRepresentation(final MediaType mediaType, final StorageItem file) {
    super(mediaType);

    this.item = file;

    setModificationDate(new Date(file.getModified()));

    setAvailable(true);
  }

  protected StorageItem getStorageItem() {
    return item;
  }

  @Override
  public void write(OutputStream outputStream)
      throws IOException
  {
  }

}
