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
package org.sonatype.nexus.rest.index.artifact;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.AbstractArtifactViewProvider;
import org.sonatype.nexus.rest.NoSuchRepositoryAccessException;
import org.sonatype.nexus.rest.model.ArtifactInfoResource;
import org.sonatype.nexus.rest.model.ArtifactInfoResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryUrlResource;
import org.sonatype.plexus.rest.ReferenceFactory;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchResponse;
import org.restlet.data.Request;

/**
 * Artifact info view provider.
 *
 * @author Velo
 * @author cstamas
 */
@Named("info")
@Singleton
public class InfoArtifactViewProvider
    extends AbstractArtifactViewProvider
{
  private final IndexerManager indexerManager;

  private final RepositoryRegistry protectedRepositoryRegistry;

  private final RepositoryRegistry repositoryRegistry;

  private final ReferenceFactory referenceFactory;

  private final AccessManager accessManager;

  @Inject
  public InfoArtifactViewProvider(final IndexerManager indexerManager,
                                  final @Named("protected") RepositoryRegistry protectedRepositoryRegistry,
                                  final RepositoryRegistry repositoryRegistry,
                                  final ReferenceFactory referenceFactory,
                                  final AccessManager accessManager)
  {
    this.indexerManager = indexerManager;
    this.protectedRepositoryRegistry = protectedRepositoryRegistry;
    this.repositoryRegistry = repositoryRegistry;
    this.referenceFactory = referenceFactory;
    this.accessManager = accessManager;
  }

  @Override
  protected Object retrieveView(ResourceStoreRequest request, RepositoryItemUid itemUid, StorageItem item,
                                Request req)
      throws IOException
  {
    StorageFileItem fileItem = (StorageFileItem) item;

    Set<String> repositories = new LinkedHashSet<String>();

    // the artifact does exists on the repository it was found =D
    repositories.add(itemUid.getRepository().getId());

    final String checksum =
        fileItem == null ? null : fileItem.getRepositoryItemAttributes()
            .get(DigestCalculatingInspector.DIGEST_SHA1_KEY);
    if (checksum != null) {
      IteratorSearchResponse searchResponse = null;

      try {
        searchResponse =
            indexerManager.searchArtifactSha1ChecksumIterator(checksum, null, null, null, null, null);

        for (ArtifactInfo info : searchResponse) {
          repositories.add(info.repository);
        }
      }
      catch (NoSuchRepositoryException e) {
        // should never trigger this exception since I'm searching on all repositories
        getLogger().error(e.getMessage(), e);
      }
      finally {
        if (searchResponse != null) {
          searchResponse.close();
        }
      }
    }

    // hosted / cache check useful if the index is out to date or disable
    for (Repository repo : protectedRepositoryRegistry.getRepositories()) {
      // already found the artifact on this repo
      if (repositories.contains(repo.getId())) {
        continue;
      }

      final ResourceStoreRequest repoRequest =
          new ResourceStoreRequest(itemUid.getPath(), request.isRequestLocalOnly(),
              request.isRequestRemoteOnly());
      if (repo.getLocalStorage().containsItem(repo, repoRequest)) {
        try {
          StorageItem repoItem = repo.retrieveItem(repoRequest);
          if (checksum == null
              ||
              checksum.equals(repoItem.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_SHA1_KEY))) {
            repositories.add(repo.getId());
          }
        }
        catch (AccessDeniedException e) {
          // that is fine, user doesn't have access
          continue;
        }
        catch (RepositoryNotAvailableException e) {
          // this could happen normally if a repository is not available, do not complain too loudly
          getLogger().trace("Repository not available; ignoring", e);
        }
        catch (Exception e) {
          getLogger().error(e.getMessage(), e);
        }
      }
    }

    ArtifactInfoResourceResponse result = new ArtifactInfoResourceResponse();

    ArtifactInfoResource resource = new ArtifactInfoResource();
    resource.setRepositoryId(itemUid.getRepository().getId());
    resource.setRepositoryName(itemUid.getRepository().getName());
    resource.setRepositoryPath(itemUid.getPath());
    resource.setRepositories(createRepositoriesUrl(repositories, req, itemUid.getPath()));
    resource.setPresentLocally(fileItem != null);

    if (fileItem != null) {
      resource.setMd5Hash(fileItem.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_MD5_KEY));
      resource.setSha1Hash(checksum);
      resource.setLastChanged(fileItem.getModified());
      resource.setSize(fileItem.getLength());
      resource.setUploaded(fileItem.getCreated());
      resource.setUploader(fileItem.getRepositoryItemAttributes().get(AccessManager.REQUEST_USER));
      resource.setMimeType(fileItem.getMimeType());

      try {
        accessManager.decide(itemUid.getRepository(), request, Action.delete);
        resource.setCanDelete(true);
      }
      catch (AccessDeniedException e) {
        resource.setCanDelete(false);
      }
    }

    result.setData(resource);

    return result;
  }

  /**
   * Here, we do want _real_ data: hashes, size, dates of link targets too, if any.
   */
  @Override
  protected boolean dereferenceLinks() {
    return true;
  }

  private List<RepositoryUrlResource> createRepositoriesUrl(Set<String> repositories, Request req, String path) {
    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    List<RepositoryUrlResource> urls = new ArrayList<RepositoryUrlResource>();
    for (String repositoryId : repositories) {
      RepositoryUrlResource repoUrl = new RepositoryUrlResource();

      try {
        protectedRepositoryRegistry.getRepository(repositoryId);
        repoUrl.setCanView(true);
      }
      catch (NoSuchRepositoryAccessException e) {
        // don't have view access, so won't see it!
        repoUrl.setCanView(false);
      }
      catch (NoSuchRepositoryException e) {
        // completely unexpect, probably another thread removed this repo
        getLogger().error(e.getMessage(), e);
        continue;
      }

      repoUrl.setRepositoryId(repositoryId);
      try {
        repoUrl.setRepositoryName(repositoryRegistry.getRepository(repositoryId).getName());
      }
      catch (NoSuchRepositoryException e) {
        // should never happen;
        getLogger().error(e.getMessage(), e);
      }
      repoUrl.setArtifactUrl(referenceFactory.createReference(req,
          "content/repositories/" + repositoryId + path).toString());
      repoUrl.setPath(path);

      urls.add(repoUrl);
    }
    return urls;
  }
}
