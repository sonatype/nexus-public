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

import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.plugins.p2.repository.P2CompositeGroupRepository;
import org.sonatype.nexus.plugins.p2.repository.P2Constants;
import org.sonatype.nexus.plugins.p2.repository.P2ContentClass;
import org.sonatype.nexus.plugins.p2.repository.P2GroupRepository;
import org.sonatype.nexus.plugins.p2.repository.metadata.P2MetadataSource;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractGroupRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.InvalidGroupingException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;

@Named(P2GroupRepositoryImpl.ROLE_HINT)
@Description("Eclipse P2 Artifaxcts")
public class P2GroupRepositoryImpl
    extends AbstractGroupRepository
    implements P2GroupRepository, GroupRepository
{
  public static final String ROLE_HINT = "p2";

  private final ContentClass contentClass;

  private final P2MetadataSource<P2GroupRepository> metadataSource;

  private final P2GroupRepositoryConfigurator p2GroupRepositoryConfigurator;

  private RepositoryKind repositoryKind;

  @Inject
  public P2GroupRepositoryImpl(final @Named(P2ContentClass.ID) ContentClass contentClass,
                               final P2MetadataSource<P2GroupRepository> metadataSource,
                               final P2GroupRepositoryConfigurator p2GroupRepositoryConfigurator)
  {
    this.contentClass = checkNotNull(contentClass);
    this.metadataSource = checkNotNull(metadataSource);
    this.p2GroupRepositoryConfigurator = checkNotNull(p2GroupRepositoryConfigurator);
  }

  @Override
  public ContentClass getRepositoryContentClass() {
    return contentClass;
  }

  @Override
  public RepositoryKind getRepositoryKind() {
    if (repositoryKind == null) {
      repositoryKind =
          new DefaultRepositoryKind(GroupRepository.class,
              Arrays.asList(new Class<?>[]{P2GroupRepository.class}));
    }
    return repositoryKind;
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
  protected StorageItem doRetrieveItem(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    final StorageItem item;
    try {
      item = metadataSource.doRetrieveItem(request, this);
    }
    catch (IOException e) {
      throw new StorageException(e);
    }

    if (item != null) {
      return item;
    }

    return super.doRetrieveItem(request);
  }

  @Override
  public StorageItem retrieveItem(final boolean fromTask, final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    final RepositoryItemUid uid = createUid(P2Constants.METADATA_LOCK_PATH);
    final RepositoryItemUidLock lock = uid.getLock();

    try {
      lock.lock(Action.read);
      return super.retrieveItem(fromTask, request);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  protected void validateMemberRepository(final Repository repository)
      throws InvalidGroupingException
  {
    if (repository.getRepositoryKind().isFacetAvailable(P2CompositeGroupRepository.class)) {
      throw new InvalidGroupingException(String.format(
          "Repository '%s' cannot be grouped as P2 composite groups are not supported as members of P2 legacy groups",
          repository.getName()
      ));
    }
    super.validateMemberRepository(repository);
  }

}
