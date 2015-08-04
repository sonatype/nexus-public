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
package org.sonatype.plexus.rest.representation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

/**
 * A simple Restlet.org representation based on InputStream.
 *
 * @author cstamas
 */
public class InputStreamRepresentation
    extends OutputRepresentation
{
  private InputStream is;

  public InputStreamRepresentation(MediaType mediaType, InputStream is) {
    super(mediaType);

    setTransient(true);

    this.is = is;
  }

  @Override
  public InputStream getStream()
      throws IOException
  {
    return is;
  }

  @Override
  public void write(OutputStream outputStream)
      throws IOException
  {
    try {
      IOUtils.copy(is, outputStream);
    }
    finally {
      is.close();
    }
  }

}
