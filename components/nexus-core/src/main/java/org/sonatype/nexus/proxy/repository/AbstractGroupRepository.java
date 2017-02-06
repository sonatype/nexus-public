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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.events.RepositoryEventEvictUnusedItems;
import org.sonatype.nexus.proxy.events.RepositoryGroupMembersChangedEvent;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsGroupLocalOnlyAttribute;
import org.sonatype.nexus.proxy.mapping.RequestRepositoryMapper;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.proxy.walker.WalkerFilter;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * An abstract group repository. The specific behaviour (ie. metadata merge) should be implemented in subclases.
 *
 * @author cstamas
 */
public abstract class AbstractGroupRepository
    extends AbstractRepository
    implements GroupRepository
{
  // == these below are injected

  private RepositoryRegistry repoRegistry;

  private RequestRepositoryMapper requestRepositoryMapper;

  // ==

  @Inject
  public void populateAbstractGroupRepository(
      final RepositoryRegistry repoRegistry, final RequestRepositoryMapper requestRepositoryMapper)
  {
    this.repoRegistry = checkNotNull(repoRegistry);
    this.requestRepositoryMapper = requestRepositoryMapper;
  }

  @Override
  protected AbstractGroupRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (AbstractGroupRepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }

  @Subscribe
  public void onEvent(final RepositoryRegistryEventRemove evt) {
    final AbstractGroupRepositoryConfiguration extConfig = this.getExternalConfiguration(false);

    if (extConfig != null && extConfig.getMemberRepositoryIds().contains(evt.getRepository().getId())) {
      removeMemberRepositoryId(evt.getRepository().getId());
    }
  }

  @Override
  protected void prepareForSave()
      throws ConfigurationException
  {
    super.prepareForSave();

    boolean membersChanged = false;
    List<String> currentMemberIds = Collections.emptyList();
    List<String> newMemberIds = Collections.emptyList();

    if (isConfigured()) {
      // we must do this before the super.onEvent() call!
      // members changed if config was dirty AND the "old" and "new" member ID list (List<String>) are NOT equal
      membersChanged =
          getCurrentCoreConfiguration().isDirty()
              && !getExternalConfiguration(false).getMemberRepositoryIds().equals(
              getExternalConfiguration(true).getMemberRepositoryIds());

      // we have to "remember" these before commit happens in super.onEvent
      // but ONLY if we are dirty and we do have "member changes" (see membersChanged above)
      // this same boolean drives the firing of the event too, for which we are actually collecting these lists
      // if no event to be fired, these lines should also not execute, since they are "dirtying" the config
      // if membersChange is true, config is already dirty, and we DO KNOW there is member change to happen
      // and we will fire the event too
      if (membersChanged) {
        currentMemberIds = getExternalConfiguration(false).getMemberRepositoryIds();
        newMemberIds = getExternalConfiguration(true).getMemberRepositoryIds();
      }
    }

    if (membersChanged) {
      // fire another event
      eventBus().post(new RepositoryGroupMembersChangedEvent(this, currentMemberIds, newMemberIds));
    }
  }

  @Override
  protected boolean doExpireCaches(final ResourceStoreRequest request, final WalkerFilter filter) {
    final List<Repository> members = getMemberRepositories();
    for (Repository member : members) {
      member.expireCaches(request, filter);
    }
    return super.doExpireCaches(request, filter);
  }

  @Override
  protected Collection<String> doEvictUnusedItems(final ResourceStoreRequest request, final long timestamp) {
    HashSet<String> result = new HashSet<String>();
    // here, we just iterate over members and call evict
    final List<Repository> members = getMemberRepositories();
    for (Repository repository : members) {
      result.addAll(repository.evictUnusedItems(request, timestamp));
    }
    eventBus().post(new RepositoryEventEvictUnusedItems(this));
    return result;
  }

  // ==

  @Override
  protected Collection<StorageItem> doListItems(ResourceStoreRequest request)
      throws ItemNotFoundException, StorageException
  {
    HashSet<String> names = new HashSet<String>();
    ArrayList<StorageItem> result = new ArrayList<StorageItem>();
    boolean found = false;
    try {
      addItems(names, result, getLocalStorage().listItems(this, request));

      found = true;
    }
    catch (ItemNotFoundException ignored) {
      // ignored
    }

    RepositoryItemUid uid = createUid(request.getRequestPath());

    final boolean isRequestGroupLocalOnly =
        request.isRequestGroupLocalOnly() || uid.getBooleanAttributeValue(IsGroupLocalOnlyAttribute.class);

    final Map<Repository, Throwable> memberThrowables = memberThrowablesFor(request);

    if (!isRequestGroupLocalOnly) {
      for (Repository repo : getMemberRepositories()) {
        if (!request.getProcessedRepositories().contains(repo.getId())) {
          try {
            addItems(names, result, repo.list(false, request));
            found = true;
          }
          catch (ItemNotFoundException e) {
            // ignored, but bookkeeping happens now
            memberThrowables.put(repo, e);
          }
          catch (IllegalOperationException e) {
            // ignored, but bookkeeping happens now
            memberThrowables.put(repo, e);
          }
          catch (StorageException e) {
            // ignored, but bookkeeping happens now
            memberThrowables.put(repo, e);
          }
        }
        else {
          if (log.isDebugEnabled()) {
            log.debug(
                String.format(
                    "Repository %s member of group %s was already processed during this request! Skipping it from processing. Request: %s",
                    RepositoryStringUtils.getHumanizedNameString(repo),
                    RepositoryStringUtils.getHumanizedNameString(this), request.toString()));
          }
        }
      }
    }

    if (!found) {
      if (!isRequestGroupLocalOnly) {
        throw new GroupItemNotFoundException(request, this, memberThrowables);
      }
      else {
        throw new GroupItemNotFoundException(reasonFor(request, this,
            "The %s not found in local storage of group repository %s (no member processing happened).",
            request.getRequestPath(), this), memberThrowables);
      }
    }

    return result;
  }

  private static void addItems(HashSet<String> names, ArrayList<StorageItem> result,
                               Collection<StorageItem> listItems)
  {
    for (StorageItem item : listItems) {
      if (names.add(item.getPath())) {
        result.add(item);
      }
    }
  }

  @Override
  protected StorageItem doRetrieveItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (!request.isRequestGroupMembersOnly()) {
      try {
        // local always wins
        return super.doRetrieveItem(request);
      }
      catch (ItemNotFoundException ignored) {
        // ignored
      }
    }

    boolean hasRequestAuthorizedFlag = request.getRequestContext().containsKey(AccessManager.REQUEST_AUTHORIZED);

    if (!hasRequestAuthorizedFlag) {
      request.getRequestContext().put(AccessManager.REQUEST_AUTHORIZED, Boolean.TRUE);
    }

    final Map<Repository, Throwable> memberThrowables = memberThrowablesFor(request);

    try {
      RepositoryItemUid uid = createUid(request.getRequestPath());

      final boolean isRequestGroupLocalOnly =
          request.isRequestGroupLocalOnly() || uid.getBooleanAttributeValue(IsGroupLocalOnlyAttribute.class);

      if (!isRequestGroupLocalOnly) {
        for (Repository repo : getRequestRepositories(request)) {
          if (!request.getProcessedRepositories().contains(repo.getId())) {
            try {
              StorageItem item = repo.retrieveItem(request);

              if (item instanceof StorageCollectionItem) {
                item = new DefaultStorageCollectionItem(this, request, true, false);
              }

              return item;
            }
            catch (IllegalOperationException e) {
              // ignored, but bookkeeping happens now
              memberThrowables.put(repo, e);
            }
            catch (ItemNotFoundException e) {
              // ignored, but bookkeeping happens now
              memberThrowables.put(repo, e);
            }
            catch (StorageException e) {
              // ignored, but bookkeeping happens now
              memberThrowables.put(repo, e);
            }
            catch (AccessDeniedException e) {
              // cannot happen, since we add/check for AccessManager.REQUEST_AUTHORIZED flag
              // ignored, but bookkeeping happens now
              memberThrowables.put(repo, e);
            }
          }
          else {
            if (log.isDebugEnabled()) {
              log.debug(
                  String.format(
                      "Repository %s member of group %s was already processed during this request! Skipping it from processing. Request: %s",
                      RepositoryStringUtils.getHumanizedNameString(repo),
                      RepositoryStringUtils.getHumanizedNameString(this), request.toString()));
            }
          }
        }
      }
      if (!isRequestGroupLocalOnly) {
        throw new GroupItemNotFoundException(request, this, memberThrowables);
      }
      else {
        throw new GroupItemNotFoundException(reasonFor(request, this,
            "The %s not found in local storage of group repository %s (no member processing happened).",
            request.getRequestPath(), this), memberThrowables);
      }
    }
    finally {
      if (!hasRequestAuthorizedFlag) {
        request.getRequestContext().remove(AccessManager.REQUEST_AUTHORIZED);
      }
    }
  }

  @Override
  public List<String> getMemberRepositoryIds() {
    ArrayList<String> result =
        new ArrayList<String>(getExternalConfiguration(false).getMemberRepositoryIds().size());

    for (String id : getExternalConfiguration(false).getMemberRepositoryIds()) {
      result.add(id);
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public void setMemberRepositoryIds(List<String> repositories)
      throws NoSuchRepositoryException, InvalidGroupingException
  {
    getExternalConfiguration(true).clearMemberRepositoryIds();

    for (String repoId : repositories) {
      addMemberRepositoryId(repoId);
    }
  }

  @Override
  public void addMemberRepositoryId(String repositoryId)
      throws NoSuchRepositoryException, InvalidGroupingException
  {
    // validate THEN modify
    // this will throw NoSuchRepository if needed
    Repository repo = repoRegistry.getRepository(repositoryId);

    // check for cycles
    List<String> memberIds = new ArrayList<String>(getExternalConfiguration(false).getMemberRepositoryIds());
    memberIds.add(repo.getId());
    checkForCyclicReference(getId(), memberIds, getId());

    // check for compatibility
    validateMemberRepository(repo);

    // if we are here, all is well
    getExternalConfiguration(true).addMemberRepositoryId(repo.getId());
  }

  private void checkForCyclicReference(final String id, List<String> memberRepositoryIds, String path)
      throws InvalidGroupingException
  {
    if (memberRepositoryIds.contains(id)) {
      throw new InvalidGroupingException(id, path);
    }

    for (String memberId : memberRepositoryIds) {
      try {
        GroupRepository group = repoRegistry.getRepositoryWithFacet(memberId, GroupRepository.class);
        checkForCyclicReference(id, group.getMemberRepositoryIds(), path + '/' + memberId);
      }
      catch (NoSuchRepositoryException e) {
        // not a group repo, just ignore
      }
    }
  }

  @Override
  public void removeMemberRepositoryId(String repositoryId) {
    getExternalConfiguration(true).removeMemberRepositoryId(repositoryId);
  }

  @Override
  public List<Repository> getMemberRepositories() {
    ArrayList<Repository> result = new ArrayList<Repository>();

    for (String repoId : getMemberRepositoryIds()) {
      try {
        Repository repo = repoRegistry.getRepository(repoId);
        result.add(repo);
      }
      catch (NoSuchRepositoryException e) {
        if (log.isDebugEnabled()) {
          this.log.warn("Could not find repository '{}' while iterating members", repoId, e);
        }
        else {
          this.log.warn("Could not find repository '{}' while iterating members", repoId);
        }
        // XXX throw new StorageException( e ) ;
      }
    }

    return result;
  }

  /**
   * Validates if a repository can be added as member of group. By default will only check that content class of
   * group
   * repository is compatible will content class of member repository.
   *
   * @param repository to be added
   * @throws InvalidGroupingException If passed in repository cannot be added as member
   * @since 2.6
   */
  protected void validateMemberRepository(final Repository repository)
      throws InvalidGroupingException
  {
    if (!getRepositoryContentClass().isCompatible(repository.getRepositoryContentClass())) {
      throw new InvalidGroupingException(getRepositoryContentClass(), repository.getRepositoryContentClass());
    }
  }

  protected List<Repository> getRequestRepositories(ResourceStoreRequest request)
      throws StorageException
  {
    List<Repository> members = getMemberRepositories();

    try {
      return requestRepositoryMapper.getMappedRepositories(this, request, members);
    }
    catch (NoSuchResourceStoreException e) {
      throw new LocalStorageException(e);
    }
  }

  @Override
  public List<StorageItem> doRetrieveItems(ResourceStoreRequest request)
      throws GroupItemNotFoundException, StorageException
  {
    ArrayList<StorageItem> items = new ArrayList<StorageItem>();

    RepositoryItemUid uid = createUid(request.getRequestPath());

    final boolean isRequestGroupLocalOnly =
        request.isRequestGroupLocalOnly() || uid.getBooleanAttributeValue(IsGroupLocalOnlyAttribute.class);

    final Map<Repository, Throwable> memberThrowables = memberThrowablesFor(request);

    if (!isRequestGroupLocalOnly) {
      for (Repository repository : getRequestRepositories(request)) {
        if (!request.getProcessedRepositories().contains(repository.getId())) {
          try {
            StorageItem item = repository.retrieveItem(false, request);

            items.add(item);
          }
          catch (ItemNotFoundException e) {
            // ignored, but bookkeeping happens now
            memberThrowables.put(repository, e);
          }
          catch (RepositoryNotAvailableException e) {
            if (log.isDebugEnabled()) {
              log.debug(
                  RepositoryStringUtils.getFormattedMessage(
                      "Member repository %s is not available, request failed.", e.getRepository()));
            }
            // ignored, but bookkeeping happens now
            memberThrowables.put(repository, e);
          }
          catch (StorageException e) {
            throw e;
          }
          catch (IllegalOperationException e) {
            log.warn("Member repository request failed", e);
            // ignored, but bookkeeping happens now
            memberThrowables.put(repository, e);
          }
        }
        else {
          if (log.isDebugEnabled()) {
            log.debug(
                String.format(
                    "Repository %s member of group %s was already processed during this request! Skipping it from processing. Request: %s",
                    RepositoryStringUtils.getHumanizedNameString(repository),
                    RepositoryStringUtils.getHumanizedNameString(this), request.toString()));
          }
        }
      }
    }

    if (items.isEmpty()) {
      if (!isRequestGroupLocalOnly) {
        throw new GroupItemNotFoundException(request, this, memberThrowables);
      }
      else {
        throw new GroupItemNotFoundException(reasonFor(request, this,
            "The %s not found in local storage of group repository %s (no member processing happened).",
            request.getRequestPath(), this), memberThrowables);
      }
    }

    return items;
  }

  // ===================================================================================
  // Inner stuff

  @Override
  public void maintainNotFoundCache(ResourceStoreRequest request)
      throws ItemNotFoundException
  {
    // just maintain the cache (ie. expiration), but don't make NFC
    // affect call delegation to members
    try {
      super.maintainNotFoundCache(request);
    }
    catch (ItemNotFoundException e) {
      // ignore it
    }
  }

  @Override
  public List<Repository> getTransitiveMemberRepositories() {
    return getTransitiveMemberRepositories(this);
  }

  protected List<Repository> getTransitiveMemberRepositories(GroupRepository group) {
    List<Repository> repos = new ArrayList<Repository>();
    for (Repository repo : group.getMemberRepositories()) {
      if (repo.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
        repos.addAll(getTransitiveMemberRepositories(repo.adaptToFacet(GroupRepository.class)));
      }
      else {
        repos.add(repo);
      }
    }
    return repos;
  }

  @Override
  public List<String> getTransitiveMemberRepositoryIds() {
    List<String> ids = new ArrayList<String>();
    for (Repository repo : getTransitiveMemberRepositories()) {
      ids.add(repo.getId());
    }
    return ids;
  }

  private static Map<Repository, Throwable> memberThrowablesFor(final ResourceStoreRequest request) {
    if (request.isDescribe()) {
      return Maps.newLinkedHashMap();
    }
    else {
      return new DiscardingMap<>(); // don't bother storing throwables as they will never be used
    }
  }

  /**
   * Accepts puts, but never stores anything.
   */
  static final class DiscardingMap<K, V>
      extends AbstractMap<K, V>
  {
    @Override
    public Set<Entry<K, V>> entrySet() {
      return Collections.emptySet();
    }

    @Override
    public V put(final K key, final V value) {
      return null;
    }
  }

}
