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
package org.sonatype.nexus.repository.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Determines which requested group members the current user is not authorized to add. This is the single choke point
 * shared by every repository write path (REST API and the ExtDirect UI services) so that the CWE-862 authorization
 * check cannot be bypassed by writing {@code group.memberNames} through a path that skips it.
 *
 * <p>
 * A member is rejected when it is <em>newly added</em> to the group and either the caller cannot read or browse it or
 * it does not resolve to an existing repository (fail closed). Members already stored on the group are grandfathered so
 * existing group configurations remain editable, and the
 * comparison is case-insensitive to match {@link RepositoryManager#get(String)}, which resolves names case-folded
 * even though repository names may contain uppercase characters.
 */
@Component
public class GroupMemberAuthorizationChecker
{
  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  @Autowired
  public GroupMemberAuthorizationChecker(
      final RepositoryManager repositoryManager,
      final RepositoryPermissionChecker repositoryPermissionChecker)
  {
    this.repositoryManager = requireNonNull(repositoryManager);
    this.repositoryPermissionChecker = requireNonNull(repositoryPermissionChecker);
  }

  /**
   * Returns the requested member names that are newly added to the named group and that the current user is not
   * permitted to add - either because it cannot read or browse the member, or because the name does not resolve to an
   * existing repository (fail closed, so an unresolvable name is never treated as authorized). Members already stored
   * on the group (compared case-insensitively) are grandfathered and omitted. The REST path additionally reports
   * non-existent members with a dedicated message before delegating here; the UI paths rely on this fail-closed
   * behavior to reject unresolvable names.
   *
   * @param groupRepositoryName the group being created or updated (may not yet exist on create)
   * @param requestedMemberNames the member names submitted in the request
   * @return the unauthorized newly-added member names, in request order; empty if there are none
   */
  public List<String> findUnauthorizedNewMembers(
      final String groupRepositoryName,
      final Collection<String> requestedMemberNames)
  {
    if (requestedMemberNames == null || requestedMemberNames.isEmpty()) {
      return Collections.emptyList();
    }
    Set<String> currentMemberNames = currentMemberNamesLowerCased(groupRepositoryName);
    List<String> unauthorized = new ArrayList<>();
    for (String memberName : requestedMemberNames) {
      if (memberName == null || currentMemberNames.contains(memberName.toLowerCase())) {
        continue;
      }
      Repository member = repositoryManager.get(memberName);
      // Fail closed: an unresolvable name (member == null) must never be treated as authorized, otherwise the UI
      // write paths - which do not pre-validate member existence - could persist a non-existent name that would then
      // be grandfathered forever, including after a real repository is later created under that name (NEXUS-53747).
      if (member == null || !repositoryPermissionChecker.userCanReadOrBrowse(member)) {
        unauthorized.add(memberName);
      }
    }
    return unauthorized;
  }

  /**
   * Returns the member names currently stored on the group, lower-cased, or an empty set if the group does not yet
   * exist (create) or has no stored members.
   */
  private Set<String> currentMemberNamesLowerCased(final String groupRepositoryName) {
    Repository existingGroup = repositoryManager.get(groupRepositoryName);
    if (existingGroup == null) {
      return Collections.emptySet();
    }
    Collection<String> existingMemberNames = existingGroup.getConfiguration()
        .attributes("group")
        .get("memberNames", new TypeToken<Collection<String>>()
        {
        });
    if (existingMemberNames == null) {
      return Collections.emptySet();
    }
    return existingMemberNames.stream()
        .filter(Objects::nonNull)
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
  }
}
