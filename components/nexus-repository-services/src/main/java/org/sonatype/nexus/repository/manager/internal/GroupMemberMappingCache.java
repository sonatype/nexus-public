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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import com.google.common.eventbus.Subscribe;

/**
 * Maintain mapping of which groups a member repository is contained in
 *
 * @since 3.16
 */
@Singleton
@Named
public class GroupMemberMappingCache
  extends ComponentSupport
    implements EventAware
{
  private volatile Map<String, List<String>> memberInGroupsMap;

  private RepositoryManager repositoryManager;

  @Subscribe
  public void on(final RepositoryCreatedEvent event) {
    log.debug("Handling repository create event for {}", event.getRepository().getName());
    if (event.getRepository().optionalFacet(GroupFacet.class).isPresent()) {
      memberInGroupsMap = null;
    }
  }

  @Subscribe
  public void on(final RepositoryUpdatedEvent event) {
    log.debug("Handling repository updated event for {}", event.getRepository().getName());
    if (event.getRepository().optionalFacet(GroupFacet.class).isPresent()) {
      memberInGroupsMap = null;
    }
  }

  @Subscribe
  public void on(final RepositoryDeletedEvent event) {
    log.debug("Handling repository deleted event for {}", event.getRepository().getName());
    memberInGroupsMap = null;
  }

  List<String> getGroups(String member) {
    return new ArrayList<>(getCache().getOrDefault(member, Collections.emptyList()));
  }

  void init(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }

  private Map<String, List<String>> getCache() {
    Map<String, List<String>> cache = memberInGroupsMap;

    if (cache == null) {
      synchronized (this) {
        if (memberInGroupsMap == null) {
          memberInGroupsMap = populateCache();
        }
        cache = memberInGroupsMap;
      }
    }

    return cache;
  }

  private Map<String,List<String>> populateCache() {
    Map<String, List<String>> cache = new HashMap<>();

    repositoryManager.browse().forEach(repository -> {
      if (!repository.optionalFacet(GroupFacet.class).isPresent()) {
        TreeMap<Integer, List<String>> groupNamesByLevel = new TreeMap<>();

        findContainingGroups(repository.getName(), groupNamesByLevel, 0);

        cache.put(repository.getName(),
            groupNamesByLevel.values().stream().peek(groupNames -> groupNames.sort(null)).flatMap(Collection::stream)
                .collect(Collectors.toList()));
      }
    });

    return cache;
  }

  private void findContainingGroups(final String name,
                                    final SortedMap<Integer, List<String>> groupNamesByLevel,
                                    final int level)
  {
    final List<String> newContainingGroups = new ArrayList<>();

    //find any groups that directly contain the desired repository name (and make sure to only include each name
    //once to save processing time)
    repositoryManager.browse()
        .forEach(repository -> repository.optionalFacet(GroupFacet.class).ifPresent(groupFacet -> {
          String groupName = repository.getName();
          if (groupFacet.member(name) && groupNamesByLevel.values().stream()
              .noneMatch(groupNames -> groupNames.contains(groupName))) {
            newContainingGroups.add(groupName);
          }
        }));

    List<String> groupNames = groupNamesByLevel.computeIfAbsent(level, newLevel -> new ArrayList<>());

    groupNames.addAll(newContainingGroups);

    //now process each group we found and check if any groups contain it
    newContainingGroups.forEach( newName -> findContainingGroups(newName, groupNamesByLevel, level + 1));
  }
}
