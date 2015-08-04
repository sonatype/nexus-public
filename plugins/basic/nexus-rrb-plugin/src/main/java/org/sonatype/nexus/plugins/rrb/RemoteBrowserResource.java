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
package org.sonatype.nexus.plugins.rrb;

import java.net.URLDecoder;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.http.QueryStringBuilder;
import org.sonatype.nexus.rest.AbstractResourceStoreContentPlexusResource;
import org.sonatype.nexus.rest.repositories.AbstractRepositoryPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.thoughtworks.xstream.XStream;
import org.apache.http.client.HttpClient;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A REST resource for retrieving directories from a remote repository.
 */
@Path(RemoteBrowserResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Named
@Singleton
public class RemoteBrowserResource
    extends AbstractResourceStoreContentPlexusResource
    implements PlexusResource
{
  public static final String RESOURCE_URI = "/repositories/{" +
      AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY + "}/remotebrowser";

  private final Logger logger = LoggerFactory.getLogger(RemoteBrowserResource.class);

  private final QueryStringBuilder queryStringBuilder;

  private final Hc4Provider httpClientProvider;

  @Inject
  public RemoteBrowserResource(final QueryStringBuilder queryStringBuilder,
                               final Hc4Provider httpClientProvider)
  {
    this.queryStringBuilder = checkNotNull(queryStringBuilder);
    this.httpClientProvider = checkNotNull(httpClientProvider);
    setRequireStrictChecking(false);
  }

  @Override
  public Object getPayloadInstance() {
    // if you allow PUT or POST you would need to return your object.
    return null;
  }

  @Override
  public void configureXStream(XStream xstream) {
    super.configureXStream(xstream);
    xstream.alias("rrbresponse", MavenRepositoryReaderResponse.class);
    xstream.alias("node", RepositoryDirectory.class);
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    // Allow anonymous access for now
    // return new PathProtectionDescriptor( "/repositories/*/remotebrowser/**", "anon" );
    return new PathProtectionDescriptor("/repositories/*/remotebrowser/**", "authcBasic,perms[nexus:browseremote]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  /**
   * Returns the directory nodes retrieved by remote browsing of proxy repository.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = MavenRepositoryReaderResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    String id = request.getAttributes().get(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY).toString();
    ResourceStoreRequest storageItem = getResourceStoreRequest(request);
    String remotePath = null;

    try {
      remotePath = URLDecoder.decode(storageItem.getRequestPath().substring(1), "UTF-8");
    }
    catch (Exception e) {
      // old way
      remotePath = storageItem.getRequestPath().substring(1);
    }

    try {
      ProxyRepository proxyRepository = getUnprotectedRepositoryRegistry()
          .getRepositoryWithFacet(id, ProxyRepository.class);
      HttpClient client = httpClientProvider.createHttpClient(proxyRepository.getRemoteStorageContext());

      MavenRepositoryReader mr = new MavenRepositoryReader(client, queryStringBuilder);
      MavenRepositoryReaderResponse data = new MavenRepositoryReaderResponse();
      // FIXME: Sort this out, NEXUS-4058 was closed about a year go (orig: we really should not do the encoding here, but this is work around until NEXUS-4058 is fixed).
      String localUrl = createRemoteResourceReference(request, id, "").toString(false, false);
      List<RepositoryDirectory> result = mr.extract(remotePath, localUrl, proxyRepository, id);
      data.setData(result);
      logger.debug("return value is {}", data);

      return data;
    }
    catch (NoSuchRepositoryException e) {
      this.logger.warn("Could not find repository: " + id, e);
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Could not find repository: " + id, e);
    }
  }

  protected Reference createRemoteResourceReference(Request request, String repoId, String remoteUrl) {
    Reference repoRootRef = createRepositoryReference(request, repoId);

    return createReference(repoRootRef, "remotebrowser/" + remoteUrl);
  }

  /**
   * DUMMY IMPLEMENTATION, just to satisfy superclass (but why is this class expanding it at all?)
   */
  @Override
  protected ResourceStore getResourceStore(final Request request)
      throws NoSuchResourceStoreException, ResourceException
  {
    return getUnprotectedRepositoryRegistry().getRepository(
        request.getAttributes().get(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY).toString());
  }
}
