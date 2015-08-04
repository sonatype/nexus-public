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
package org.sonatype.nexus.rest.identify;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.rest.index.AbstractIndexerNexusPlexusResource;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.apache.maven.index.MAVEN;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Resource that is able to fetch the identified Nexus Artifact. The used hash algorithm and hash key are coming from
 * request attributes, and are posibly mapped from URL. Recognized algorithms: "sha1" and "md5".
 *
 * @author cstamas
 */
@Path(IdentifyHashPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Named("IdentifyHashPlexusResource")
@Singleton
public class IdentifyHashPlexusResource
    extends AbstractIndexerNexusPlexusResource
{
  public static final String ALGORITHM_KEY = "algorithm";

  public static final String HASH_KEY = "hash";

  public static final String RESOURCE_URI = "/identify/{" + ALGORITHM_KEY + "}/{" + HASH_KEY + "}";

  private final IndexerManager indexerManager;

  @Inject
  public IdentifyHashPlexusResource(final IndexerManager indexerManager) {
    this.indexerManager = indexerManager;
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/identify/*/*", "authcBasic,perms[nexus:identify]");
  }

  /**
   * Retrieve artifact details using a hash value.
   *
   * @param algorithm The hash algorithm (i.e. md5 or sha1).
   * @param hash      The hash string to compare.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {
      @PathParam(IdentifyHashPlexusResource.ALGORITHM_KEY),
      @PathParam(IdentifyHashPlexusResource.HASH_KEY)
  }, output = NexusArtifact.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    String alg = request.getAttributes().get(ALGORITHM_KEY).toString();

    String checksum = request.getAttributes().get(HASH_KEY).toString();

    NexusArtifact na = null;

    try {
      if ("sha1".equalsIgnoreCase(alg)) {
        Collection<NexusArtifact> nas =
            ai2NaColl(request, indexerManager.identifyArtifact(MAVEN.SHA1, checksum));

        if (nas != null && nas.size() > 0) {
          na = nas.iterator().next();
        }
      }
    }
    catch (IOException e) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "IOException during configuration retrieval!", e);
    }

    return na;
  }
}
