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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeRepositoryTargetPropertyDescriptor;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.proxy.targets.TargetMatch;
import org.sonatype.nexus.proxy.targets.TargetRegistry;
import org.sonatype.nexus.proxy.targets.TargetSet;
import org.sonatype.nexus.threads.FakeAlmightySubject;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.authorization.xml.SecurityXmlAuthorizationManager;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
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

  private final TargetRegistry targetRegistry;

  private final AuthorizationManager defaultAuthorizationManager;

  private final boolean authorizeByPrivilegedTargets;

  @VisibleForTesting
  static final String ADMIN_PRIVILEGE_ID = "1000";

  @Inject
  public DefaultNexusItemAuthorizer(final SecuritySystem securitySystem,
                                    final RepositoryRegistry repoRegistry,
                                    final TargetRegistry targetRegistry,
                                    @Named(SecurityXmlAuthorizationManager.SOURCE)
                                        final AuthorizationManager defaultAuthorizationManager,
                                    @Named("${defaultNexusItemAuthorizer.authorizeByPrivilegedTargets:-true}")
                                        boolean authorizeByPrivilegedTargets)
  {
    this.securitySystem = securitySystem;
    this.repoRegistry = repoRegistry;
    this.targetRegistry = targetRegistry;
    this.defaultAuthorizationManager = defaultAuthorizationManager;
    this.authorizeByPrivilegedTargets = authorizeByPrivilegedTargets;
  }

  public boolean authorizePath(final Repository repository, final ResourceStoreRequest request, final Action action) {
    // NEXUS-21281 - try to skip scanning all targets by using the users privileged targets
    if (authorizeByPrivilegedTargets) {
      if (isAuthorizeByPrivilegedTargets(repository, request, action)) {
        return true;
      }
    }
    else {
      // check repo only first, if there is directly assigned matching target permission, we're good
      final TargetSet matched = repository.getTargetsForRequest(request);
      if (matched != null && authorizePath(matched, action)) {
        return true;
      }
    }

    // if we are here, we need to check cascading permissions, where this repository is contained in group
    return authorizePathCascade(repository, request, action);
  }

  private boolean isAuthorizeByPrivilegedTargets(final Repository repository,
                                                 final ResourceStoreRequest request,
                                                 final Action action)
  {
    Subject subject = securitySystem.getSubject();

    //typically task subject where no user in context
    if (subject instanceof FakeAlmightySubject) {
      return true;
    }

    User user = getUser(subject);
    if (user != null) {
      Set<String> assignedPrivileges = getAssignedPrivileges(user);

      if (hasAdminPrivilege(assignedPrivileges)) {
        return true;
      }

      return hasRequiredRepoTargetPrivilege(assignedPrivileges, request, repository, action);
    }

    return false;
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

  private User getUser(final Subject subject) {
    try {
      if (subject != null) {
        return securitySystem.getUser((String) subject.getPrincipal());
      }
      else {
        log.debug("Attempt to authenticate with no Subject.");
      }
    }
    catch (UserNotFoundException e) {
      log.debug("Unable to find user: {}", e.getMessage());
    }

    return null;
  }

  private Set<String> getPrivilegedTargets(final Set<String> assignedPrivilegeIds) {
    Set<String> assignedTargetIds = new HashSet<>();

    assignedPrivilegeIds.forEach(privilegeId -> addPrivilege(assignedTargetIds, privilegeId));

    return assignedTargetIds;
  }

  private Set<String> getAssignedPrivileges(final User user) {
    Set<String> assignedPrivileges = new HashSet<>();
    user.getRoles().forEach(roleIdentifier -> {
      try {
        assignedPrivileges.addAll(
            getAllPrivilegesFromRole(defaultAuthorizationManager.getRole(roleIdentifier.getRoleId()),
                defaultAuthorizationManager));
      }
      catch (NoSuchRoleException e) {
        log.debug("Unable to find Role: '{}' for User: '{}'. Because of: {}", roleIdentifier, user, e.getMessage());
      }
    });
    return assignedPrivileges;
  }

  private Set<String> getAllPrivilegesFromRole(final Role role, final AuthorizationManager authorizationManager) {
    Set<String> privileges = new HashSet<>(role.getPrivileges());
    role.getRoles().forEach(childRole -> {
      try {
        privileges.addAll(getAllPrivilegesFromRole(authorizationManager.getRole(childRole), authorizationManager));
      }
      catch (NoSuchRoleException e) {
        log.debug("Unable to find Role: '{}'. Because of: {}", childRole, e.getMessage());
      }
    });
    return privileges;
  }

  private void addPrivilege(final Set<String> assignedTargetIds,
                            final String privilegeId)
  {
    try {
      Privilege privilege = defaultAuthorizationManager.getPrivilege(privilegeId);
      if (TargetPrivilegeDescriptor.TYPE.equals(privilege.getType())) {
        assignedTargetIds.add(privilege.getPrivilegeProperty(TargetPrivilegeRepositoryTargetPropertyDescriptor.ID));
      }
    }
    catch (NoSuchPrivilegeException e) {
      log.debug("Unable to find Privilege: '{}'. Because of: {}", privilegeId, e.getMessage());
    }
  }

  private boolean hasAdminPrivilege(final Set<String> assignedPrivileges) {
    //admin priv, only non target priv that can give access
    return assignedPrivileges.contains(ADMIN_PRIVILEGE_ID);
  }

  private boolean hasRequiredRepoTargetPrivilege(
      final Set<String> assignedPrivileges,
      final ResourceStoreRequest request,
      final Repository repository,
      final Action action)
  {
    Set<String> assignedTargetIds = getPrivilegedTargets(assignedPrivileges);
    TargetSet targetSet = new TargetSet();
    for (String targetId : assignedTargetIds) {
      Target target = targetRegistry.getRepositoryTarget(targetId);
      if (target != null) {
        if (target.isPathContained(repository.getRepositoryContentClass(), request.getRequestPath())) {
          targetSet.addTargetMatch(new TargetMatch(target, repository));
        }
      }
      else {
        log.debug("Unable to find Repository Target: '{}'.", targetId);
      }
    }
    if (targetSet.getMatches().size() > 0) {
      return authorizePath(targetSet, action);
    }
    return false;
  }
}
