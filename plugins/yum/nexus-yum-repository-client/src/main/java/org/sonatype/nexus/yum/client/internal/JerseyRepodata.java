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
package org.sonatype.nexus.yum.client.internal;

import java.io.IOException;

import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.yum.client.MetadataType;
import org.sonatype.nexus.yum.client.Repodata;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * @since yum 3.0
 */
public class JerseyRepodata
    extends SubsystemSupport<JerseyNexusClient>
    implements Repodata
{

  private final Repositories repositories;

  public JerseyRepodata(final JerseyNexusClient nexusClient, final Repositories repositories) {
    super(nexusClient);
    this.repositories = repositories;
  }

  @Override
  public <T> T getMetadata(final String repositoryId,
                           final MetadataType metadataType,
                           final Class<T> returnType)
      throws IOException
  {
    try {
      final String url = ensureUrlEndsWithSlash(repositoryId);
      final String location = getLocationOfMetadata(url, metadataType);
      if (location != null) {
        return handleResponse(
            getNexusClient().getClient().resource(url + location).get(ClientResponse.class),
            returnType,
            metadataType.getCompression()
        );
      }
      throw new NexusClientNotFoundException(
          "Could not find metadata type '" + metadataType.getType() + "'", null
      );
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public <T> T getMetadata(final String repositoryId,
                           final String version,
                           final MetadataType metadataType,
                           final Class<T> returnType)
      throws IOException
  {
    try {
      final String url = getNexusClient().resolveServicePath("yum/repos/" + repositoryId + "/" + version + "/");
      final String location = getLocationOfMetadata(url, metadataType);
      if (location != null) {
        return handleResponse(
            getNexusClient().getClient().resource(url + location).get(ClientResponse.class),
            returnType,
            metadataType.getCompression()
        );
      }
      throw new NexusClientNotFoundException(
          "Could not find metadata type '" + metadataType.getType() + "'", null
      );
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public String getMetadataPath(final String repositoryId,
                           final MetadataType metadataType)
      throws IOException
  {
    try {
      final String url = ensureUrlEndsWithSlash(repositoryId);
      final String location = getLocationOfMetadata(url, metadataType);
      if (location != null) {
        return "/" + location;
      }
      throw new NexusClientNotFoundException(
          "Could not find metadata type '" + metadataType.getType() + "'", null
      );
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public String getMetadataUrl(final String repositoryId,
                                final MetadataType metadataType)
      throws IOException
  {
    try {
      final String url = ensureUrlEndsWithSlash(repositoryId);
      final String location = getLocationOfMetadata(url, metadataType);
      if (location != null) {
        return url + location;
      }
      throw new NexusClientNotFoundException(
          "Could not find metadata type '" + metadataType.getType() + "'", null
      );
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public String getIndex(final String repositoryId, final String version) {
    return getIndex(repositoryId, version, null);
  }

  @Override
  public String getIndex(final String repositoryId, final String version, final String path) {
    try {
      return getNexusClient().serviceResource(
          "yum/repos/" + repositoryId + "/" + version + "/"
              + (path == null ? "" : path + (path.endsWith("/") ? "" : "/"))
      ).get(String.class);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  private String getLocationOfMetadata(final String url, final MetadataType metadataType) {
    final ClientResponse clientResponse = getNexusClient().getClient()
        .resource(url + "repodata/repomd.xml")
        .get(ClientResponse.class);
    try {
      if (clientResponse.getStatus() < 300) {
        final RepoMD repomd = new RepoMD(clientResponse.getEntityInputStream());
        return repomd.getLocation(metadataType.getType());
      }
      throw getNexusClient().convert(new UniformInterfaceException(clientResponse));
    }
    finally {
      clientResponse.close();
    }
  }

  private <T> T handleResponse(final ClientResponse clientResponse,
                               final Class<T> returnType,
                               final CompressionType compression)
      throws IOException
  {
    try {
      if (clientResponse.getStatus() < 300) {
        clientResponse.setEntityInputStream(
            new CompressionAdapter(compression).adapt(clientResponse.getEntityInputStream())
        );
        return clientResponse.getEntity(returnType);
      }
      throw getNexusClient().convert(new UniformInterfaceException(clientResponse));
    }
    finally {
      clientResponse.close();
    }
  }

  private String ensureUrlEndsWithSlash(final String repositoryId) {
    String uri = repositories.get(repositoryId).contentUri();
    if (!uri.endsWith("/")) {
      uri += "/";
    }
    return uri;
  }

}
