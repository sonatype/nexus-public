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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import javax.ws.rs.core.Response;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.Utilities;
import org.sonatype.nexus.client.rest.jersey.ContextAwareUniformInterfaceException;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkState;

/**
 * @since 2.1
 */
public class JerseyUtilities
    extends SubsystemSupport<JerseyNexusClient>
    implements Utilities
{

  public JerseyUtilities(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public Date getLastModified(final String uri) {
    try {
      final ClientResponse response = getNexusClient()
          .uri(uri)
          .head();

      return response.getLastModified();
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  /**
   * @since 2.6
   */
  @Override
  public Utilities download(final String path, final File target)
      throws IOException
  {
    if (!target.exists()) {
      final File targetDir = target.getParentFile();
      // NOTE: can not use java.nio.Files here as this module needs to remain Java6 compatible
      FileUtils.forceMkdir(targetDir);
    }
    else {
      checkState(target.isFile() && target.canWrite(), "File '%s' is not a file or could not be written",
          target.getAbsolutePath());
    }

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(target);
      return download(path, fos);
    }
    finally {
      IOUtils.closeQuietly(fos);
    }
  }

  /**
   * @since 2.6
   */
  @Override
  public Utilities download(final String path, final OutputStream target)
      throws IOException
  {
    try {
      final ClientResponse response = getNexusClient().uri(path).get(ClientResponse.class);

      if (!ClientResponse.Status.OK.equals(response.getClientResponseStatus())) {
        throw getNexusClient().convert(new ContextAwareUniformInterfaceException(response)
        {
          @Override
          public String getMessage(final int status) {
            if (status == Response.Status.NOT_FOUND.getStatusCode()) {
              return String.format("Inexistent path: %s", path);
            }
            return null;
          }
        });
      }

      try {
        IOUtils.copy(response.getEntityInputStream(), target);
      }
      finally {
        response.close();
      }
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
    return this;
  }

}
