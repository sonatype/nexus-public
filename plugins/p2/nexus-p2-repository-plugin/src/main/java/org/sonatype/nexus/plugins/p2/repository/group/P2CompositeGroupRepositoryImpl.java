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
package org.sonatype.nexus.plugins.p2.repository.group;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.plugins.p2.repository.P2CompositeGroupRepository;
import org.sonatype.nexus.plugins.p2.repository.P2Constants;
import org.sonatype.nexus.plugins.p2.repository.P2ContentClass;
import org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.events.RepositoryEventLocalStatusChanged;
import org.sonatype.nexus.proxy.events.RepositoryGroupMembersChangedEvent;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractGroupRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.p2.bridge.CompositeRepository;

import com.google.common.base.Throwables;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.plugins.p2.repository.P2Constants.COMPOSITE_ARTIFACTS_XML;
import static org.sonatype.nexus.plugins.p2.repository.P2Constants.COMPOSITE_CONTENT_XML;
import static org.sonatype.nexus.plugins.p2.repository.P2Constants.P2_INDEX;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.createTemporaryP2Repository;

/**
 * P2 group using P2 composite repositories.
 *
 * @since 2.6
 */
@Named(P2CompositeGroupRepositoryImpl.ROLE_HINT)
@Description("Eclipse P2 Composite")
public class P2CompositeGroupRepositoryImpl
    extends AbstractGroupRepository
    implements P2CompositeGroupRepository, GroupRepository
{
  public static final String ROLE_HINT = "p2-composite";

  private final ContentClass contentClass;

  private final P2GroupRepositoryConfigurator p2GroupRepositoryConfigurator;

  private final CompositeRepository compositeRepository;

  private final ApplicationStatusSource applicationStatusSource;

  private RepositoryKind repositoryKind;

  @Inject
  public P2CompositeGroupRepositoryImpl(final @Named(P2ContentClass.ID) ContentClass contentClass,
                                        final P2GroupRepositoryConfigurator p2GroupRepositoryConfigurator,
                                        final CompositeRepository compositeRepository,
                                        final ApplicationStatusSource applicationStatusSource)
  {
    this.contentClass = checkNotNull(contentClass);
    this.p2GroupRepositoryConfigurator = checkNotNull(p2GroupRepositoryConfigurator);
    this.compositeRepository = checkNotNull(compositeRepository);
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
  }

  @Override
  protected Configurator getConfigurator() {
    return p2GroupRepositoryConfigurator;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<P2GroupRepositoryConfiguration>()
    {
      @Override
      public P2GroupRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
        return new P2GroupRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  public RepositoryKind getRepositoryKind() {
    if (repositoryKind == null) {
      repositoryKind =
          new DefaultRepositoryKind(GroupRepository.class,
              Arrays.asList(new Class<?>[]{P2CompositeGroupRepository.class}));
    }
    return repositoryKind;
  }

  @Override
  public ContentClass getRepositoryContentClass() {
    return contentClass;
  }

  @Override
  protected StorageItem doRetrieveItem(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    final RepositoryItemUid uid = createUid(P2Constants.METADATA_LOCK_PATH);
    final RepositoryItemUidLock lock = uid.getLock();
    final boolean requestGroupLocalOnly = request.isRequestGroupLocalOnly();
    try {
      lock.lock(Action.read);
      request.setRequestGroupLocalOnly(true);
      return super.doRetrieveItem(request);
    }
    finally {
      request.setRequestGroupLocalOnly(requestGroupLocalOnly);
      lock.unlock();
    }
  }

  @Override
  protected Collection<StorageItem> doListItems(final ResourceStoreRequest request)
      throws ItemNotFoundException, StorageException
  {
    final RepositoryItemUid uid = createUid(P2Constants.METADATA_LOCK_PATH);
    final RepositoryItemUidLock lock = uid.getLock();
    final boolean requestGroupLocalOnly = request.isRequestGroupLocalOnly();
    try {
      lock.lock(Action.read);
      request.setRequestGroupLocalOnly(true);
      return super.doListItems(request);
    }
    finally {
      request.setRequestGroupLocalOnly(requestGroupLocalOnly);
      lock.unlock();
    }
  }

  @Subscribe
  public void onEvent(final RepositoryGroupMembersChangedEvent event) {
    if (this.equals(event.getRepository())) {
      createP2CompositeXmls(event.getNewRepositoryMemberIds(), true);
    }
  }

  @Subscribe
  public void onEvent(final RepositoryRegistryEventAdd event) {
    if (this.equals(event.getRepository())) {
      createP2CompositeXmls(getMemberRepositoryIds(), false);
    }
  }

  @Subscribe
  public void onEvent(final NexusStartedEvent event) {
    createP2CompositeXmls(getMemberRepositoryIds(), false);
  }

  @Subscribe
  public void onEvent(final RepositoryEventLocalStatusChanged event) {
    if (this.equals(event.getRepository()) && event.getNewLocalStatus().shouldServiceRequest()) {
      createP2CompositeXmls(getMemberRepositoryIds(), true);
    }
  }

  private void createP2CompositeXmls(final List<String> memberRepositoryIds,
                                     final boolean forced)
  {
    if (!getLocalStatus().shouldServiceRequest()
        || !applicationStatusSource.getSystemStatus().isNexusStarted()) {
      return;
    }
    if (!forced) {
      try {
        retrieveItem(true, new ResourceStoreRequest(COMPOSITE_ARTIFACTS_XML));
        // xmls are present, so bailout
        return;
      }
      catch (Exception e) {
        // will regenerate
      }
    }

    try {
      File tempP2Repository = null;
      try {
        tempP2Repository = createTemporaryP2Repository();

        final URI[] memberRepositoryUris = toUris(memberRepositoryIds);

        compositeRepository.addArtifactsRepository(tempP2Repository.toURI(), memberRepositoryUris);
        compositeRepository.addMetadataRepository(tempP2Repository.toURI(), memberRepositoryUris);

        final RepositoryItemUid uid = createUid(P2Constants.METADATA_LOCK_PATH);
        final RepositoryItemUidLock lock = uid.getLock();
        try {
          lock.lock(Action.create);

          NexusUtils.storeItemFromFile(
              COMPOSITE_ARTIFACTS_XML,
              new File(tempP2Repository, COMPOSITE_ARTIFACTS_XML),
              this,
              getMimeSupport().guessMimeTypeFromPath(COMPOSITE_ARTIFACTS_XML)
          );
          NexusUtils.storeItemFromFile(
              COMPOSITE_CONTENT_XML,
              new File(tempP2Repository, COMPOSITE_CONTENT_XML),
              this,
              getMimeSupport().guessMimeTypeFromPath(COMPOSITE_CONTENT_XML)
          );
          NexusUtils.storeItem(
              this,
              new ResourceStoreRequest(P2_INDEX),
              getClass().getResourceAsStream("/META-INF/p2Composite.index"),
              getMimeSupport().guessMimeTypeFromPath(P2_INDEX),
              null
          );
        }
        finally {
          lock.unlock();
        }
      }
      finally {
        FileUtils.deleteDirectory(tempP2Repository);
      }
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private URI[] toUris(final List<String> memberRepositoryIds) {
    if (memberRepositoryIds == null || memberRepositoryIds.isEmpty()) {
      return new URI[0];
    }
    final URI[] uris = new URI[memberRepositoryIds.size()];
    for (int i = 0; i < memberRepositoryIds.size(); i++) {
      try {
        uris[i] = new URI("../../repositories/" + memberRepositoryIds.get(i));
      }
      catch (URISyntaxException e) {
        throw Throwables.propagate(e);
      }
    }
    return uris;
  }

}
