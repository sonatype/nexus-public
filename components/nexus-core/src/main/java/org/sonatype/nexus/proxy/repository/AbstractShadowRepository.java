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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.events.RepositoryItemEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDelete;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStore;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.proxy.walker.AbstractFileWalkerProcessor;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * The Class ShadowRepository.
 *
 * @author cstamas
 */
public abstract class AbstractShadowRepository
    extends AbstractRepository
    implements ShadowRepository
{

  /**
   * The cached instance of master Repository, to not have it look up at every {@link #getMasterRepository()}
   * invocation from registry. This instance changes as master ID in configuration changes.
   */
  private volatile Repository masterRepository;

  @Override
  protected AbstractShadowRepositoryConfiguration getExternalConfiguration(boolean forModification) {
    return (AbstractShadowRepositoryConfiguration) super.getExternalConfiguration(forModification);
  }

  @Override
  public boolean isSynchronizeAtStartup() {
    return getExternalConfiguration(false).isSynchronizeAtStartup();
  }

  @Override
  public void setSynchronizeAtStartup(final boolean val) {
    getExternalConfiguration(true).setSynchronizeAtStartup(val);
  }

  @Override
  public Repository getMasterRepository() {
    // return the cached instance
    return masterRepository;
  }

  @Override
  public void setMasterRepository(final Repository masterRepository)
      throws IncompatibleMasterRepositoryException
  {
    if (getMasterRepositoryContentClass().getId().equals(masterRepository.getRepositoryContentClass().getId())) {
      // set master ID in configuration
      getExternalConfiguration(true).setMasterRepositoryId(masterRepository.getId());
      // cache the instance
      this.masterRepository = masterRepository;
    }
    else {
      throw new IncompatibleMasterRepositoryException(this, masterRepository.getId());
    }
  }

  /**
   * The shadow is delegating it's availability to it's master, but we can still shot down the shadow only.
   */
  @Override
  public LocalStatus getLocalStatus() {
    return super.getLocalStatus().shouldServiceRequest()
        && getMasterRepository().getLocalStatus().shouldServiceRequest() ? LocalStatus.IN_SERVICE
        : LocalStatus.OUT_OF_SERVICE;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onRepositoryItemEvent(final RepositoryItemEvent ievt) {
    // NEXUS-5673: do we need to act on event at all?
    if (!getLocalStatus().shouldServiceRequest()) {
      return;
    }
    // is this event coming from our master?
    if (getMasterRepository() == ievt.getRepository()) {
      try {
        if (ievt instanceof RepositoryItemEventStore || ievt instanceof RepositoryItemEventCache) {
          createLink(ievt.getItem());
        }
        else if (ievt instanceof RepositoryItemEventDelete) {
          deleteLink(ievt.getItem());
        }
      }
      catch (UnsupportedStorageOperationException e) {
        // NEXUS-5673
        // this should be a bug? Could happen in case when master instructs shadow to create a link for a
        // release artifact, while this shadow has a snapshot repository policy. Then, how was this shapshot
        // made a shadow of release repository?
        log.debug("Shadow {} refuses to maintain links, ignoring event {}", this, ievt, e);
      }
      catch (IllegalOperationException e) {
        // NEXUS-5673
        // repo out of service should be handled above
        // maybe a ReadOnly shadow?
        log.debug("Shadow {} refuses to maintain links, ignoring event {}", this, ievt, e);
      }
      catch (ItemNotFoundException e) {
        // NEXUS-5673
        // happens regularly for parents, as those are not transformed and just pollutes the log
        // similar for M2 checksum files
        log.debug("Corresponding item in {} for master path not found, ignoring event {}", this, ievt);
      }
      catch (Exception e) {
        log.warn("Could not sync shadow {} for event {}", this, ievt, e);
      }
    }
  }

  protected abstract void deleteLink(StorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;

  protected abstract StorageLinkItem createLink(StorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException;

  protected void synchronizeLink(final StorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
  {
    createLink(item);
  }

  /**
   * Synchronize with master.
   */
  @Override
  public void synchronizeWithMaster() {
    if (!getLocalStatus().shouldServiceRequest()) {
      return;
    }

    log.info("Syncing shadow " + getId() + " with master repository " + getMasterRepository().getId());

    final ResourceStoreRequest root = new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT, true);

    expireNotFoundCaches(root);

    final AbstractFileWalkerProcessor sw = new AbstractFileWalkerProcessor()
    {
      @Override
      protected void processFileItem(WalkerContext context, StorageFileItem item)
          throws Exception
      {
        synchronizeLink(item);
      }
    };

    final DefaultWalkerContext ctx = new DefaultWalkerContext(getMasterRepository(), root);

    ctx.getProcessors().add(sw);

    try {
      getWalker().walk(ctx);
    }
    catch (WalkerException e) {
      if (!(e.getWalkerContext().getStopCause() instanceof ItemNotFoundException)) {
        // everything that is not ItemNotFound should be reported,
        // otherwise just neglect it
        throw e;
      }
    }
  }

  protected StorageItem doRetrieveItemFromMaster(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    try {
      return getMasterRepository().retrieveItem(request);
    }
    catch (AccessDeniedException e) {
      // if client has no access to content over shadow, we just hide the fact
      throw new ItemNotFoundException(reasonFor(request, this,
          "Path %s not found in repository %s",
          RepositoryStringUtils.getHumanizedNameString(this)), e);
    }
  }
}
