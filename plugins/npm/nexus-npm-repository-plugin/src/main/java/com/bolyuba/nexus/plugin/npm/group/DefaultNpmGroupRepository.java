/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.group;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractGroupRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import com.bolyuba.nexus.plugin.npm.NpmContentClass;
import com.bolyuba.nexus.plugin.npm.hosted.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.internal.NpmMimeRulesSource;
import com.bolyuba.nexus.plugin.npm.service.GroupMetadataService;
import com.bolyuba.nexus.plugin.npm.service.MetadataServiceFactory;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
@Named(DefaultNpmGroupRepository.ROLE_HINT)
@Typed(GroupRepository.class)
@Description("Npm registry group repo")
public class DefaultNpmGroupRepository
    extends AbstractGroupRepository
    implements NpmGroupRepository, GroupRepository
{

  public static final String ROLE_HINT = "npm-group";

  private final ContentClass contentClass;

  private final NpmGroupRepositoryConfigurator configurator;

  private final RepositoryKind repositoryKind;

  private final NpmMimeRulesSource mimeRulesSource;

  private final GroupMetadataService groupMetadataService;

  @Inject
  public DefaultNpmGroupRepository(final @Named(NpmContentClass.ID) ContentClass contentClass,
                                   final NpmGroupRepositoryConfigurator configurator,
                                   final MetadataServiceFactory metadataServiceFactory)
  {

    this.groupMetadataService = metadataServiceFactory.createGroupMetadataService(this);
    this.mimeRulesSource = new NpmMimeRulesSource();
    this.contentClass = checkNotNull(contentClass);
    this.configurator = checkNotNull(configurator);
    this.repositoryKind = new DefaultRepositoryKind(NpmGroupRepository.class, null);
  }

  @Override
  public GroupMetadataService getMetadataService() { return groupMetadataService; }

  @Override
  protected Configurator getConfigurator() {
    return this.configurator;
  }

  @Override
  public RepositoryKind getRepositoryKind() {
    return this.repositoryKind;
  }

  @Override
  public ContentClass getRepositoryContentClass() {
    return this.contentClass;
  }

  @Override
  public MimeRulesSource getMimeRulesSource() {
    return mimeRulesSource;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<NpmGroupRepositoryConfiguration>()
    {
      @Override
      public NpmGroupRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
        return new NpmGroupRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  protected AbstractStorageItem doRetrieveLocalItem(ResourceStoreRequest storeRequest)
      throws ItemNotFoundException, LocalStorageException
  {
    if (!getMetadataService().isNpmMetadataServiced(storeRequest)) {
      // shut down NPM MD+tarball service completely
      return super.doRetrieveLocalItem(storeRequest);
    }
    try {
      PackageRequest packageRequest = null;
      try {
        packageRequest = new PackageRequest(storeRequest);
      } catch (IllegalArgumentException e) {
        // something completely different
        return super.doRetrieveLocalItem(storeRequest);
      }
      if (packageRequest != null) {
        if (packageRequest.isMetadata()) {
          ContentLocator contentLocator;
          if (packageRequest.isRegistryRoot()) {
            log.debug("Serving registry root...");
            contentLocator = groupMetadataService.produceRegistryRoot(packageRequest);
          }
          else if (packageRequest.isPackageRoot()) {
            log.debug("Serving package {} root...", packageRequest.getCoordinates().getPackageName());
            contentLocator = groupMetadataService.producePackageRoot(packageRequest);
          }
          else {
            log.debug("Serving package {} version {}...", packageRequest.getCoordinates().getPackageName(), packageRequest.getCoordinates().getVersion());
            contentLocator = groupMetadataService.producePackageVersion(packageRequest);
          }
          if (contentLocator != null) {
            return new DefaultStorageFileItem(this, storeRequest, true, true, contentLocator);
          }
        }
        else {
          // registry special
          if (packageRequest.isRegistrySpecial() && packageRequest.getCoordinates().getPath().startsWith("/-/all")) {
            log.debug("Serving registry root from /-/all...");
            return new DefaultStorageFileItem(this, storeRequest, true, true,
                groupMetadataService.produceRegistryRoot(packageRequest));
          }
          log.debug("Unknown registry special {}", packageRequest.getCoordinates().getPath());
        }
      }
      log.debug("No NPM metadata for path {}", storeRequest.getRequestPath());
      throw new ItemNotFoundException(
          reasonFor(storeRequest, this, "No content for path %s", storeRequest.getRequestPath()));
    }
    catch (IOException e) {
      throw new LocalStorageException("Metadata service error", e);
    }
  }

  /**
   * npm group will pass on to it's hosted member a publish request. npm publishes with single PUT request where
   * tarball is inlined as JSON resource, hence, no danger that subsequent requests may end up in different place.
   *
   * @since 1.2.0
   */
  @Override
  public void storeItem(ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException,
             IllegalOperationException,
             StorageException,
             AccessDeniedException
  {
    List<Repository> members = getTransitiveMemberRepositories();
    for (Repository member : members) {
      NpmHostedRepository hosted = member.adaptToFacet(NpmHostedRepository.class);
      if (hosted != null) {
        try {
          hosted.storeItem(request, is, userAttributes);
          return;
        }
        catch (ItemNotFoundException e) {
          IllegalRequestException ex = new IllegalRequestException(
              request,
              String.format("npm group %s member %s has failed publish request", getId(), hosted.getId())
          );
          ex.initCause(e);
          throw ex;
        }
      }
    }
    throw new IllegalRequestException(
        request,
        String.format("npm group %s has no hosted member to pass on publish request", getId())
    );
  }
}