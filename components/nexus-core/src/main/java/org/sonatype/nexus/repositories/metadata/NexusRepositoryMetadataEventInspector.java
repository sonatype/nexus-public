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
package org.sonatype.nexus.repositories.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.mirror.PublishedMirrors;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Mirror;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.repository.metadata.MetadataHandlerException;
import org.sonatype.nexus.repository.metadata.RepositoryMetadataHandler;
import org.sonatype.nexus.repository.metadata.model.RepositoryMemberMetadata;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.model.RepositoryMirrorMetadata;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

@Named
@Singleton
public class NexusRepositoryMetadataEventInspector
    extends ComponentSupport
    implements EventSubscriber
{
  private final ContentClass maven1ContentClass;

  private final ContentClass maven2ContentClass;

  private final RepositoryMetadataHandler repositoryMetadataHandler;

  private final RepositoryRegistry repositoryRegistry;

  private final ApplicationStatusSource applicationStatusSource;

  @Inject
  public NexusRepositoryMetadataEventInspector(final @Named("maven1") ContentClass maven1ContentClass,
                                               final @Named("maven2") ContentClass maven2ContentClass,
                                               final RepositoryMetadataHandler repositoryMetadataHandler,
                                               final RepositoryRegistry repositoryRegistry,
                                               final ApplicationStatusSource applicationStatusSource)
  {
    this.maven1ContentClass = maven1ContentClass;
    this.maven2ContentClass = maven2ContentClass;
    this.repositoryMetadataHandler = repositoryMetadataHandler;
    this.repositoryRegistry = repositoryRegistry;
    this.applicationStatusSource = applicationStatusSource;
  }

  @Subscribe
  public void on(final NexusStartedEvent evt) {
    inspect(evt);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryRegistryEventAdd evt) {
    inspect(evt);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryConfigurationUpdatedEvent evt) {
    inspect(evt);
  }

  protected void inspect(Event<?> evt) {
    if (evt instanceof NexusStartedEvent) {
      // on start, we batch process all of those
      for (Repository repository : repositoryRegistry.getRepositories()) {
        processRepository(repository);
      }

      return;
    }

    // stuff below should happen only if this is a RUNNING instance!
    if (!applicationStatusSource.getSystemStatus().isNexusStarted()) {
      return;
    }

    if (evt instanceof RepositoryRegistryEventAdd) {
      processRepository(((RepositoryRegistryEventAdd) evt).getRepository());
    }
    else if (evt instanceof RepositoryConfigurationUpdatedEvent) {
      processRepository(((RepositoryConfigurationUpdatedEvent) evt).getRepository());
    }
  }

  protected void processRepository(Repository repository) {
    if (repository.getRepositoryContentClass().isCompatible(maven2ContentClass)
        || repository.getRepositoryContentClass().isCompatible(maven1ContentClass)) {
      if (LocalStatus.OUT_OF_SERVICE.equals(repository.getLocalStatus())) {
        return;
      }

      String repositoryUrl = null;

      String repositoryLocalUrl = null;

      List<RepositoryMirrorMetadata> mirrors = null;

      if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
        repositoryUrl = getRepositoryLocalUrl(repository);

        repositoryLocalUrl = null;
      }
      else if (repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
        // this is a maven repository
        MavenRepository mrepository = repository.adaptToFacet(MavenRepository.class);

        if (mrepository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
          repositoryUrl = mrepository.adaptToFacet(ProxyRepository.class).getRemoteUrl();

          repositoryLocalUrl = getRepositoryLocalUrl(mrepository);
        }
        else {
          repositoryUrl = getRepositoryLocalUrl(mrepository);

          repositoryLocalUrl = null;
        }
      }
      else {
        // huh? unknown stuff, better to not tamper with it
        return;
      }

      if (repository.getRepositoryKind().isFacetAvailable(HostedRepository.class)) {
        mirrors = getMirrors(repository.getId());
      }

      RepositoryMetadata rm = new RepositoryMetadata();
      rm.setUrl(repositoryUrl);
      rm.setId(repository.getId());
      rm.setName(repository.getName());
      rm.setLayout(repository.getRepositoryContentClass().getId());
      rm.setPolicy(getRepositoryPolicy(repository));
      rm.setMirrors(mirrors);

      if (repositoryLocalUrl != null) {
        rm.setLocalUrl(repositoryLocalUrl);
      }

      if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
        List<Repository> members = repository.adaptToFacet(GroupRepository.class).getMemberRepositories();

        List<RepositoryMemberMetadata> memberMetadatas =
            new ArrayList<RepositoryMemberMetadata>(members.size());

        for (Repository member : members) {
          RepositoryMemberMetadata memberMetadata = new RepositoryMemberMetadata();

          memberMetadata.setId(member.getId());

          memberMetadata.setName(member.getName());

          memberMetadata.setUrl(getRepositoryLocalUrl(member));

          memberMetadata.setPolicy(getRepositoryPolicy(member));

          memberMetadatas.add(memberMetadata);
        }

        rm.getMemberRepositories().addAll(memberMetadatas);
      }

      try {
        NexusRawTransport nrt = new NexusRawTransport(repository, true, false);

        repositoryMetadataHandler.writeRepositoryMetadata(rm, nrt);

        // "decorate" the file attrs
        StorageFileItem file = nrt.getLastWriteFile();

        file.setContentGeneratorId(NexusRepositoryMetadataContentGenerator.ID);

        repository.getAttributesHandler().storeAttributes(file);
      }
      catch (MetadataHandlerException e) {
        log.info("Could not write repository metadata!", e);
      }
      catch (IOException e) {
        log.warn("IOException during write of repository metadata!", e);
      }
      catch (Exception e) {
        log.info("Could not save repository metadata: ", e);
      }
    }
  }

  protected String getRepositoryLocalUrl(Repository repository) {
    if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      return "@rootUrl@/content/groups/" + repository.getId();
    }
    else {
      return "@rootUrl@/content/repositories/" + repository.getId();
    }
  }

  protected String getRepositoryPolicy(Repository repository) {
    if (repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
      return repository.adaptToFacet(MavenRepository.class).getRepositoryPolicy().toString().toLowerCase();
    }
    else if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      List<Repository> members = repository.adaptToFacet(GroupRepository.class).getMemberRepositories();

      HashSet<String> memberPolicies = new HashSet<String>();

      for (Repository member : members) {
        memberPolicies.add(getRepositoryPolicy(member));
      }

      if (memberPolicies.size() == 1) {
        return memberPolicies.iterator().next();
      }
      else {
        return RepositoryMetadata.POLICY_MIXED;
      }
    }
    else {
      return RepositoryMetadata.POLICY_MIXED;
    }
  }

  protected List<RepositoryMirrorMetadata> getMirrors(String repositoryId) {
    try {
      List<RepositoryMirrorMetadata> mirrors = new ArrayList<RepositoryMirrorMetadata>();

      Repository repository = repositoryRegistry.getRepository(repositoryId);

      PublishedMirrors publishedMirrors = repository.getPublishedMirrors();

      for (Mirror mirror : (List<Mirror>) publishedMirrors.getMirrors()) {
        RepositoryMirrorMetadata md = new RepositoryMirrorMetadata();

        md.setId(mirror.getId());

        md.setUrl(mirror.getUrl());

        mirrors.add(md);
      }

      return mirrors;
    }
    catch (NoSuchRepositoryException e) {
      log.debug("Repository not found, returning no mirrors");
    }

    return null;
  }
}
