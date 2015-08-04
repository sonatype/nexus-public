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
package org.sonatype.nexus.obr.group;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.obr.ObrContentClass;
import org.sonatype.nexus.obr.metadata.ManagedObrSite;
import org.sonatype.nexus.obr.metadata.ObrMetadataSource;
import org.sonatype.nexus.obr.metadata.ObrResourceReader;
import org.sonatype.nexus.obr.metadata.ObrResourceWriter;
import org.sonatype.nexus.obr.util.ObrUtils;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractGroupRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.InvalidGroupingException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;
import org.osgi.service.obr.Resource;

import static com.google.common.base.Preconditions.checkNotNull;

@Named(ObrGroupRepository.ROLE_HINT)
@Description("OBR Group")
public class ObrGroupRepository
    extends AbstractGroupRepository
    implements GroupRepository
{
  public static final String ROLE_HINT = "obr-group";

  private final ContentClass obrContentClass;

  private final ObrGroupRepositoryConfigurator obrGroupRepositoryConfigurator;

  private final ObrMetadataSource obrMetadataSource;

  private final RepositoryKind obrGroupRepositoryKind = new DefaultRepositoryKind(GroupRepository.class, null);

  private long lastModified = Long.MIN_VALUE;

  @Inject
  public ObrGroupRepository(final @Named(ObrContentClass.ID) ContentClass obrContentClass,
                            final ObrGroupRepositoryConfigurator obrGroupRepositoryConfigurator,
                            final @Named("obr-bindex") ObrMetadataSource obrMetadataSource)
  {
    this.obrContentClass = checkNotNull(obrContentClass);
    this.obrGroupRepositoryConfigurator = checkNotNull(obrGroupRepositoryConfigurator);
    this.obrMetadataSource = checkNotNull(obrMetadataSource);
  }

  public ContentClass getRepositoryContentClass() {
    return obrContentClass;
  }

  @Override
  protected Configurator getConfigurator() {
    return obrGroupRepositoryConfigurator;
  }

  public RepositoryKind getRepositoryKind() {
    return obrGroupRepositoryKind;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<ObrGroupRepositoryConfiguration>()
    {
      public ObrGroupRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
        return new ObrGroupRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  public Collection<StorageItem> list(final boolean fromTask, final StorageCollectionItem item)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    return ObrUtils.augmentListedItems(item.getRepositoryItemUid(), super.list(fromTask, item));
  }

  @Override
  protected Collection<StorageItem> doListItems(final ResourceStoreRequest request)
      throws ItemNotFoundException, StorageException
  {
    return getLocalStorage().listItems(this, request);
  }

  @Override
  protected StorageItem doRetrieveItem(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    final String path = request.getRequestPath();

    if (ObrUtils.isObrMetadataRequest(request)) {
      return mergeObrMetadata(request);
    }
    else if (!"/".equals(path) && !path.startsWith("/.")) {
      return retrieveBundleItem(request);
    }

    return getLocalStorage().retrieveItem(this, request);
  }

  /**
   * Searches through the member OBRs to find the repository that contains the given item.
   *
   * @param request the resource request
   * @return the matching bundle item
   */
  private StorageItem retrieveBundleItem(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    final String path = request.getRequestPath();

    ObrResourceReader reader = null;
    for (final Repository r : getRequestRepositories(request)) {
      try {
        reader = obrMetadataSource.getReader(new ManagedObrSite(ObrUtils.retrieveObrItem(r)));
        for (Resource i = reader.readResource(); i != null; i = reader.readResource()) {
          final URL url = i.getURL();
          if ("file".equals(url.getProtocol()) && path.equals(url.getPath())) {
            return r.retrieveItem(false, request);
          }
        }
      }
      catch (final IOException e) {
        // ignore
      }
      finally {
        IOUtils.closeQuietly(reader);
      }
    }

    throw new ItemNotFoundException(request, this);
  }

  @Override
  public void setMemberRepositoryIds(final List<String> repositories)
      throws NoSuchRepositoryException, InvalidGroupingException
  {
    lastModified = Long.MIN_VALUE;

    super.setMemberRepositoryIds(repositories);
  }

  @Override
  public void removeMemberRepositoryId(final String repositoryId) {
    lastModified = Long.MIN_VALUE;

    super.removeMemberRepositoryId(repositoryId);
  }

  /**
   * Stream all the member OBRs together into a single OBR.
   *
   * @param request the resource request
   * @return the merged OBR metadata
   */
  private StorageItem mergeObrMetadata(final ResourceStoreRequest request)
      throws StorageException
  {
    final RepositoryItemUid obrUid = createUid(request.getRequestPath());
    StorageItem obrItem = ObrUtils.getCachedItem(obrUid);

    long modified = 0;
    final Collection<StorageFileItem> memberObrItems = new ArrayList<StorageFileItem>();
    for (final Repository r : getMemberRepositories()) {
      try {
        final StorageFileItem item = ObrUtils.retrieveObrItem(r);
        modified = Math.max(modified, item.getModified());
        memberObrItems.add(item);
      }
      catch (final StorageException e) {
        // ignore this particular OBR and continue
      }
    }

    RepositoryItemUidLock uidLock = obrUid.getLock();
    if (null == obrItem || lastModified < modified) {
      uidLock.lock(Action.create);
      try {
        obrItem = ObrUtils.getCachedItem(obrUid);
        if (null == obrItem || lastModified < modified) {
          ObrResourceReader reader = null;
          ObrResourceWriter writer = null;

          try {
            writer = obrMetadataSource.getWriter(obrUid);
            for (final StorageFileItem f : memberObrItems) {
              try {
                reader = obrMetadataSource.getReader(new ManagedObrSite(f));
                for (Resource i = reader.readResource(); i != null; i = reader.readResource()) {
                  writer.append(i);
                }
              }
              catch (final IOException e) {
                log.warn("Problem merging OBR metadata from " + f.getRepositoryItemUid(), e);
              }
              finally {
                IOUtils.closeQuietly(reader);
              }
            }

            writer.complete(); // the OBR is only updated once the stream is complete and closed
          }
          finally {
            IOUtils.closeQuietly(writer);
          }

          obrItem = ObrUtils.getCachedItem(obrUid);
          if (null == obrItem) {
            // this shouldn't happen as we just saved it, but just in case...
            throw new StorageException("Problem reading OBR metadata from repository " + getId());
          }

          lastModified = modified;
        }
      }
      finally {
        uidLock.unlock();
      }
    }

    return obrItem;
  }
}
