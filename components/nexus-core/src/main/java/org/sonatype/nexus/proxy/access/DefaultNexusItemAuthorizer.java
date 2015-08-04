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
package org.sonatype.nexus.proxy.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.targets.TargetMatch;
import org.sonatype.nexus.proxy.targets.TargetSet;
import org.sonatype.security.SecuritySystem;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.shiro.subject.Subject;

/**
 * Default implementation of Nexus Authorizer, that relies onto JSecurity.
 */
@Named
@Singleton
public class DefaultNexusItemAuthorizer
    extends ComponentSupport
    implements NexusItemAuthorizer
{
  private final SecuritySystem securitySystem;

  private final RepositoryRegistry repoRegistry;

  @Inject
  public DefaultNexusItemAuthorizer(final SecuritySystem securitySystem,
                                    final RepositoryRegistry repoRegistry)
  {
    this.securitySystem = securitySystem;
    this.repoRegistry = repoRegistry;
  }

  public boolean authorizePath(final Repository repository, final ResourceStoreRequest request, final Action action) {
    // check repo only first, if there is directly assigned matching target permission, we're good
    final TargetSet matched = repository.getTargetsForRequest(request);
    if (matched != null && authorizePath(matched, action)) {
      return true;
    }
    // if we are here, we need to check cascading permissions, where this repository is contained in group
    return authorizePathCascade(repository, request, action);
  }

  private boolean authorizePathCascade(final Repository repository, final ResourceStoreRequest request,
                                       final Action action)
  {
    final List<GroupRepository> groups = repoRegistry.getGroupsOfRepository(repository);
    for (GroupRepository group : groups) {
      if (authorizePath(group, request, action)) {
        return true;
      }
    }
    return false;
  }

  public boolean authorizePermission(final String permission) {
    return isPermitted(Collections.singletonList(permission));
  }

  // ===

  public TargetSet getGroupsTargetSet(final Repository repository, final ResourceStoreRequest request) {
    final TargetSet targetSet = new TargetSet();
    for (Repository group : getListOfGroups(repository.getId())) {
      // are the perms transitively inherited from the groups where it is member?
      // !group.isExposed()
      if (true) {
        final TargetSet groupMatched = group.getTargetsForRequest(request);
        targetSet.addTargetSet(groupMatched);
        // now that we have groups of groups, this needs to be a recursive check
        targetSet.addTargetSet(getGroupsTargetSet(group, request));
      }
    }
    return targetSet;
  }

  public boolean authorizePath(final TargetSet matched, final Action action) {
    // did we hit repositories at all?
    if (matched.getMatchedRepositoryIds().size() > 0) {
      // we had reposes affected, check the targets
      // make perms from TargetSet
      return isPermitted(getTargetPerms(matched, action));
    }
    else {
      // we hit no repos, it is a virtual path, allow access
      return true;
    }
  }

  public boolean isViewable(final String objectType, final String objectId) {
    return authorizePermission("nexus:view:" + objectType + ":" + objectId);
  }

  // ==

  protected List<Repository> getListOfGroups(final String repositoryId) {
    final List<Repository> groups = new ArrayList<Repository>();
    final List<String> groupIds = repoRegistry.getGroupsOfRepository(repositoryId);
    for (String groupId : groupIds) {
      try {
        groups.add(repoRegistry.getRepository(groupId));
      }
      catch (NoSuchRepositoryException e) {
        // ignored
      }
    }
    return groups;
  }

  protected List<String> getTargetPerms(final TargetSet matched, final Action action) {
    final List<String> perms = new ArrayList<String>(matched.getMatches().size());
    // nexus : 'target' + targetId : repoId : read
    for (TargetMatch match : matched.getMatches()) {
      perms.add("nexus:target:" + match.getTarget().getId() + ":" + match.getRepository().getId() + ":" + action);
    }
    return perms;
  }

  protected boolean isPermitted(final List<String> perms) {
    boolean trace = log.isTraceEnabled();

    Subject subject = securitySystem.getSubject();

    if (trace) {
      log.trace("Subject: {}", subject);
    }

    if (subject == null) {
      if (trace) {
        log.trace("Subject is not authenticated; rejecting");
      }
      return false;
    }

    if (trace) {
      log.trace("Checking if subject '{}' has one of these permissions: {}", subject.getPrincipal(), perms);
    }
    for (String perm : perms) {
      if (subject.isPermitted(perm)) {
        // TODO: we should remember/cache these decisions per-thread and not re-evaluate it always from Security
        if (trace) {
          log.trace("Subject '{}' has permission: {}; allowing", subject.getPrincipal(), perm);
        }
        return true;
      }
    }

    if (trace) {
      log.trace("Subject '{}' is missing required permissions; rejecting", subject.getPrincipal());
    }

    return false;
  }
}
