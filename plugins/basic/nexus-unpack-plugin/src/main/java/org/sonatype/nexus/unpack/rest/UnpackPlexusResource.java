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
package org.sonatype.nexus.unpack.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.AbstractResourceStoreContentPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;

import static org.sonatype.nexus.rest.repositories.AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY;

/**
 * A REST Resource that accepts upload (zip file), and it simply explodes it in the root of the given repository.
 *
 * @author cstamas
 */
@Path("/repositories/{" + REPOSITORY_ID_KEY + "}/content-compressed")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Named
@Singleton
public class UnpackPlexusResource
    extends AbstractResourceStoreContentPlexusResource
{
  private static final String DELETE_BEFORE_UNPACK = "delete";

  public UnpackPlexusResource() {
    this.setModifiable(true);
    this.setRequireStrictChecking(false);
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/repositories/*/content-compressed/**",
        "contentAuthcBasic,perms[nexus:contentcompressed]");
  }

  @Override
  public String getResourceUri() {
    return "/repositories/{" + REPOSITORY_ID_KEY + "}/content-compressed";
  }

  @Override
  public boolean acceptsUpload() {
    return true;
  }

  /**
   * Handles uploads of ZIP files. Unpacks zip file to current path. If the delete query parameter is true the
   * everything at the current path will be removed before the zip file is unpacked.
   */
  @Override
  @POST
  @ResourceMethodSignature(
      pathParams = {@PathParam(REPOSITORY_ID_KEY)},
      queryParams = {@QueryParam(DELETE_BEFORE_UNPACK)}
  )
  public Object upload(final Context context,
                       final Request request,
                       final Response response,
                       final List<FileItem> files)
      throws ResourceException
  {
    try {
      final Repository repository = getResourceStore(request);
      final String basePath = getResourceStorePath(request);

      final Form form = request.getResourceRef().getQueryAsForm();
      final boolean delete = form.getFirst(DELETE_BEFORE_UNPACK) != null;

      if (basePath.toLowerCase().endsWith(".md5") || basePath.toLowerCase().endsWith(".sha1")) {
        // maven deploys checksums even if not asked to
        return null;
      }

      if (delete) {
        try {
          final StorageItem item = repository.retrieveItem(getResourceStoreRequest(request, basePath));
          deleteItem(repository, item);
        }
        catch (ItemNotFoundException e) {
          // that's good
        }
      }

      for (final FileItem fileItem : files) {
        final File tempFile = File.createTempFile("unzip", ".zip");
        try {
          copyToFile(fileItem, tempFile);
          final ZipFile zip = new ZipFile(tempFile);
          try {
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
              final ZipEntry entry = entries.nextElement();
              if (entry.getName().endsWith("/")) {
                // must be a folder
                continue;
              }
              final ResourceStoreRequest storeRequest =
                  getResourceStoreRequest(request, basePath + "/" + entry.getName());
              try (InputStream is = zip.getInputStream(entry)) {
                repository.storeItem(storeRequest, is, null);
              }
            }
          }
          finally {
            close(zip);
          }
        }
        finally {
          tempFile.delete();
        }
      }
    }
    catch (Exception t) {
      handleException(request, response, t);
    }
    return null;
  }

  /**
   * Handles uploads of ZIP files. Unpacks zip file to current path. If the delete query parameter is true the
   * everything at the current path will be removed before the zip file is unpacked.
   */
  @PUT
  @ResourceMethodSignature(
      pathParams = {@PathParam(REPOSITORY_ID_KEY)},
      queryParams = {@QueryParam(DELETE_BEFORE_UNPACK)}
  )
  public Object uploadPut(final Context context,
                          final Request request,
                          final Response response,
                          final List<FileItem> files)
      throws ResourceException
  {
    // NOTE: this method is only used to get the annotation processing to work correctly
    // we can not have an @PUT and @POST on the same method, so this is the quick and dirty work around.
    return this.upload(context, request, response, files);
  }

  private void deleteItem(final Repository repository, final StorageItem item) throws Exception {
    repository.deleteItem(item.getResourceStoreRequest());
  }

  private void close(final ZipFile zip) {
    try {
      zip.close();
    }
    catch (IOException e) {
      getLogger().debug("Could not close ZipFile", e);
    }
  }

  private void copyToFile(final FileItem source, final File target)
      throws IOException
  {
    try (InputStream is = source.getInputStream()) {
      FileUtils.copyInputStreamToFile(is, target);
    }
  }

  @Override
  protected Repository getResourceStore(final Request request)
      throws NoSuchResourceStoreException, ResourceException
  {
    final String repoId = request.getAttributes().get(REPOSITORY_ID_KEY).toString();
    return getUnprotectedRepositoryRegistry().getRepository(repoId);
  }
}
