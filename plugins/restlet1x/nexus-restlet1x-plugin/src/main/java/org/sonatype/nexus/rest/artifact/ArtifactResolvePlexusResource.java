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
package org.sonatype.nexus.rest.artifact;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreHelper;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.rest.model.ArtifactResolveResource;
import org.sonatype.nexus.rest.model.ArtifactResolveResourceResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.apache.commons.lang.StringUtils;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * POM Resource handler.
 *
 * @author cstamas
 */
@Named
@Singleton
@Path(ArtifactResolvePlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class ArtifactResolvePlexusResource
    extends AbstractArtifactPlexusResource
{
  public static final String RESOURCE_URI = "/artifact/maven/resolve";

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
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:artifact]");
  }

  /**
   * Resolve an artifact identified by GAV coordinates given, and retrieve a set of details about that artifact.
   *
   * @param g Group id of the artifact (Required).
   * @param a Artifact id of the artifact (Required).
   * @param v Version of the artifact (Required) Supports resolving of "LATEST", "RELEASE" and snapshot versions
   *          ("1.0-SNAPSHOT") too.
   * @param r Repository that the artifact is contained in (Required).
   * @param p Packaging type of the artifact (Optional).
   * @param c Classifier of the artifact (Optional).
   * @param e Extension of the artifact (Optional).
   */
  @Override
  @GET
  @ResourceMethodSignature(queryParams = {
      @QueryParam("g"), @QueryParam("a"), @QueryParam("v"),
      @QueryParam("r"), @QueryParam("p"), @QueryParam("c"), @QueryParam("e")
  }, output = ArtifactResolveResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    Form form = request.getResourceRef().getQueryAsForm();

    String groupId = form.getFirstValue("g");

    String artifactId = form.getFirstValue("a");

    String version = form.getFirstValue("v");

    String packaging = form.getFirstValue("p");

    String classifier = form.getFirstValue("c");

    String repositoryId = form.getFirstValue("r");

    String extension = form.getFirstValue("e");

    if (groupId == null || artifactId == null || version == null || repositoryId == null) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
          "At least following request parameters have to be given: gavr!");
    }

    // default the packaging
    if (StringUtils.isBlank(packaging)) {
      packaging = "jar";
    }

    // a flag that will prevent actual download as happens as sideeffect if this is proxy repository
    // and artifact being resolved is not present. This will not affect metadata download, since those are
    // needed for resolution!
    boolean isLocalOnly = isLocal(request, null);

    ArtifactStoreRequest gavRequest =
        getResourceStoreRequest(request, false, false, repositoryId, groupId, artifactId, version, packaging,
            classifier, extension);

    try {
      final MavenRepository mavenRepository = getMavenRepository(repositoryId);

      final ArtifactStoreHelper helper = mavenRepository.getArtifactStoreHelper();

      // not returning null, will throw INFEx instead
      final Gav resolvedGav = helper.resolveArtifact(gavRequest);

      String repositoryPath = mavenRepository.getGavCalculator().gavToPath(resolvedGav);

      StorageFileItem resolvedFile = null;

      if (!isLocalOnly) {
        resolvedFile = helper.retrieveArtifact(gavRequest);
      }

      ArtifactResolveResource resource = new ArtifactResolveResource();

      resource.setPresentLocally(resolvedFile != null);

      if (resolvedFile != null) {
        resource.setSha1(resolvedFile.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_SHA1_KEY));
      }

      resource.setGroupId(resolvedGav.getGroupId());

      resource.setArtifactId(resolvedGav.getArtifactId());

      resource.setVersion(resolvedGav.getVersion());

      resource.setClassifier(resolvedGav.getClassifier());

      resource.setExtension(resolvedGav.getExtension());

      resource.setFileName(resolvedGav.getName());

      resource.setRepositoryPath(repositoryPath);

      resource.setSnapshot(resolvedGav.isSnapshot());

      if (resource.isSnapshot()) {
        resource.setBaseVersion(resolvedGav.getBaseVersion());

        if (resolvedGav.getSnapshotBuildNumber() != null) {
          resource.setSnapshotBuildNumber(resolvedGav.getSnapshotBuildNumber());

          resource.setSnapshotTimeStamp(resolvedGav.getSnapshotTimeStamp());
        }
      }

      ArtifactResolveResourceResponse result = new ArtifactResolveResourceResponse();

      result.setData(resource);

      return result;
    }
    catch (Exception e) {
      handleException(request, response, e);

      return null;
    }
  }
}
