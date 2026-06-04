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
package org.sonatype.nexus.repository.manager.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maintain mapping of which groups a member repository is contained in
 *
 * @since 3.16
 */
@Component
public class GroupMemberMappingCache
    implements EventAware, Asynchronous
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final ReentrantLock lock = new ReentrantLock();

  private volatile boolean built = false;

  private final ConcurrentHashMap<String, Collection<String>> repositoryToContainingGroups = new ConcurrentHashMap<>();

  private RepositoryManager repositoryManager;

  public void init(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  private boolean shouldSkipEvent(final String repositoryName) {
    if (repositoryManager == null) {
      log.debug("Repository manager not initialized yet, skipping event for {}", repositoryName);
      return true;
    }
    return false;
  }

  @Subscribe
  public void on(final RepositoryCreatedEvent event) throws InterruptedException {
    String repositoryName = event.getRepository().getName();
    log.debug("Handling repository create event for {}", repositoryName);

    if (shouldSkipEvent(repositoryName)) {
      return;
    }

    if (groupFacet(event.getRepository(), 5).isPresent()) {
      if (lock.tryLock(5, TimeUnit.MINUTES)) {
        try {
          maybeCompute();
          maybeAddToMembers(repositoryName);
        }
        finally {
          lock.unlock();
        }
      }
    }
  }

  @Subscribe
  public void on(final RepositoryUpdatedEvent event) throws InterruptedException {
    String repositoryName = event.getRepository().getName();
    log.debug("Handling repository updated event for {}", repositoryName);

    if (shouldSkipEvent(repositoryName)) {
      return;
    }

    if (groupFacet(event.getRepository(), 5).isPresent()) {
      if (lock.tryLock(5, TimeUnit.MINUTES)) {
        try {
          maybeCompute();
          maybeRemoveFromMembers(repositoryName);
          maybeAddToMembers(repositoryName);
        }
        finally {
          lock.unlock();
        }
      }
    }
  }

  @Subscribe
  public void on(final RepositoryDeletedEvent event) throws InterruptedException {
    String repositoryName = event.getRepository().getName();
    log.debug("Handling repository deleted event for {}", repositoryName);

    if (shouldSkipEvent(repositoryName)) {
      return;
    }

    repositoryToContainingGroups.remove(repositoryName);

    if (groupFacet(event.getRepository(), 5).isPresent()) {
      if (lock.tryLock(5, TimeUnit.MINUTES)) {
        try {
          maybeCompute();
          maybeRemoveFromMembers(repositoryName);
        }
        finally {
          lock.unlock();
        }
      }
    }
  }

  /**
   * @param member the repository name
   * @return the names of group repositories which contain the specified member.
   */
  public Set<String> getGroups(final String member) {
    maybeCompute();
    return Optional.ofNullable(repositoryToContainingGroups.get(member))
        .map(HashSet::new)
        .orElseGet(HashSet::new);
  }

  private void maybeAddToMembers(final String modifiedRepositoryName) {
    log.debug("Computing members of {}", modifiedRepositoryName);
    Optional.ofNullable(repositoryManager.get(modifiedRepositoryName))
        .map(repository -> allMembers(repository, 5))
        .map(List::stream)
        .orElseGet(Stream::of)
        .map(Repository::getName)
        .forEach(memberName -> {
          log.debug("Adding member {}", memberName);
          repositoryToContainingGroups.computeIfAbsent(memberName, __ -> ConcurrentHashMap.newKeySet())
              .add(modifiedRepositoryName);
        });
  }

  private void maybeRemoveFromMembers(final String modifiedRepositoryName) {
    log.debug("Removing {} from cache", modifiedRepositoryName);
    Iterator<Entry<String, Collection<String>>> iter = repositoryToContainingGroups.entrySet().iterator();

    // cache of group members to avoid repeatedly calling allMembers
    Map<String, Set<String>> allMembersCache = new HashMap<>();

    while (iter.hasNext()) {
      Entry<String, Collection<String>> entry = iter.next();
      String repoName = entry.getKey();
      Collection<String> containingRepositories = entry.getValue();
      if (containingRepositories.contains(modifiedRepositoryName)) {
        log.debug("Found {} contained by {}", repoName, containingRepositories);
        containingRepositories.remove(modifiedRepositoryName);
        if (containingRepositories.isEmpty()) {
          iter.remove();
        }
        else {
          // Check other groups to determine whether this repository still belongs to the list
          containingRepositories.stream()
              .map(repositoryManager::get)
              .filter(Objects::nonNull)
              .forEach(groupRepository -> {
                String groupRepositoryName = groupRepository.getName();
                Set<String> groupMembers = allMembersCache.computeIfAbsent(groupRepositoryName,
                    __ -> allMembers(groupRepository, 5)
                        .stream()
                        .map(Repository::getName)
                        .collect(Collectors.toSet()));
                log.debug("Group repository {} contains {}", groupRepositoryName, groupMembers);
                if (!groupMembers.contains(repoName)) {
                  containingRepositories.remove(groupRepositoryName);
                }
              });
          log.debug("Computed {} contains {}", repoName, containingRepositories);
        }
      }
    }
  }

  private void maybeCompute() {
    if (!built) {
      try {
        if (lock.tryLock(5, TimeUnit.MINUTES)) {
          try {
            for (Repository repository : repositoryManager.browse()) {
              allMembers(repository, 5).stream()
                  .map(Repository::getName)
                  .map(member -> repositoryToContainingGroups.computeIfAbsent(member,
                      __ -> ConcurrentHashMap.newKeySet()))
                  .forEach(containingGroups -> containingGroups.add(repository.getName()));
            }
            log.debug("Computed {}", repositoryToContainingGroups);
            built = true;
          }
          finally {
            lock.unlock();
          }
        }
      }
      catch (InterruptedException e) {
        log.warn("Unable to build group member cache", log.isDebugEnabled() ? e : null);
      }
    }
  }

  private static List<Repository> allMembers(final Repository repository, final int retries) {
    return withRetry(() -> repository.optionalFacet(GroupFacet.class).map(GroupFacet::allMembers).orElseGet(List::of),
        List::of, retries);
  }

  private static Optional<GroupFacet> groupFacet(final Repository repository, final int retries) {
    return withRetry(() -> repository.optionalFacet(GroupFacet.class), Optional::empty, retries);
  }

  /*
   * Retries with a slight delay when an action fails with InvalidStateException. Failure states do not trigger retries
   */
  @VisibleForTesting
  static <T> T withRetry(final Supplier<T> supplier, final Supplier<T> defaultSupplier, final int retries) {
    if (retries > 0) {
      try {
        try {
          return supplier.get();
        }
        catch (InvalidStateException e) {
          if (!RepositoryImpl.State.DELETED.equals(e.getInvalidState())
              && !RepositoryImpl.State.DESTROYED.equals(e.getInvalidState())
              && !RepositoryImpl.State.FAILED.equals(e.getInvalidState())) {
            Thread.sleep(5);
            return withRetry(supplier, defaultSupplier, retries - 1);
          }
        }
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return defaultSupplier.get();
  }
}
