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
package org.sonatype.nexus.proxy.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.proxy.repository.GroupRepository;

/**
 * Fired when a group repository members changed and change is commited to configuration (is not rolled back).
 *
 * @author cstamas
 */
public class RepositoryGroupMembersChangedEvent
    extends RepositoryEvent
{

  /**
   * Enum describing the changes that might happen against a group members list. Every enum describes one of the
   * possible type of the change, but they might happen all together too.
   *
   * @author cstamas
   * @since 2.0
   */
  public enum MemberChange
  {
    /**
     * Member(s) added to group.
     */
    MEMBER_ADDED,
    /**
     * Member(s) removed from group.
     */
    MEMBER_REMOVED,
    /**
     * Member(s) reordered within a group.
     */
    MEMBER_REORDERED;
  }

  /**
   * List of member IDs that were set before this member change.
   */
  private final List<String> oldRepositoryMemberIds;

  /**
   * List of member IDs that is set after this member change.
   */
  private final List<String> newRepositoryMemberIds;

  /**
   * List of member IDs that were removed from the group.
   */
  private final List<String> removedRepositoryIds;

  /**
   * List of member IDs that were added ti the group.
   */
  private final List<String> addedRepositoryIds;

  /**
   * List of member IDs that were reordered within the group.
   */
  private final List<String> reorderedRepositoryIds;

  /**
   * Set of change enums, describing the changes in short and consise way that happened against the given group.
   */
  private final Set<MemberChange> memberChangeSet;

  public RepositoryGroupMembersChangedEvent(final GroupRepository repository, final List<String> currentMemberIds,
                                            final List<String> newMemberIds)
  {
    super(repository);
    // we need to copy these to "detach" them for sure from config
    this.oldRepositoryMemberIds = new ArrayList<String>(currentMemberIds);
    this.newRepositoryMemberIds = new ArrayList<String>(newMemberIds);

    // simple calculations
    this.removedRepositoryIds = new ArrayList<String>(oldRepositoryMemberIds);
    removedRepositoryIds.removeAll(newRepositoryMemberIds);
    this.addedRepositoryIds = new ArrayList<String>(newRepositoryMemberIds);
    addedRepositoryIds.removeAll(oldRepositoryMemberIds);
    this.reorderedRepositoryIds = new ArrayList<String>();

    // ordering detection
    final List<String> currentTrimmed = new ArrayList<String>(oldRepositoryMemberIds);
    currentTrimmed.removeAll(removedRepositoryIds);
    currentTrimmed.removeAll(addedRepositoryIds);
    final List<String> newTrimmed = new ArrayList<String>(newRepositoryMemberIds);
    newTrimmed.removeAll(removedRepositoryIds);
    newTrimmed.removeAll(addedRepositoryIds);

    if (!currentTrimmed.equals(newTrimmed) && currentTrimmed.size() > 0) {
      Iterator<String> i1 = currentTrimmed.iterator();
      Iterator<String> i2 = newTrimmed.iterator();

      while (i1.hasNext()) {
        final String oldEl = i1.next();
        final String newEl = i2.next();
        if (!oldEl.equals(newEl)) {
          reorderedRepositoryIds.add(newEl);
        }
      }
    }

    this.memberChangeSet = EnumSet.noneOf(MemberChange.class);
    if (!reorderedRepositoryIds.isEmpty()) {
      memberChangeSet.add(MemberChange.MEMBER_REORDERED);
    }
    if (!removedRepositoryIds.isEmpty()) {
      memberChangeSet.add(MemberChange.MEMBER_REMOVED);
    }
    if (!addedRepositoryIds.isEmpty()) {
      memberChangeSet.add(MemberChange.MEMBER_ADDED);
    }
  }

  /**
   * Returns the group repository instance being reconfigured.
   */
  public GroupRepository getGroupRepository() {
    return (GroupRepository) getEventSender();
  }

  /**
   * Returns the set of enums describing the changes happened against group repository.
   *
   * @since 2.0
   */
  public Set<MemberChange> getMemberChangeSet() {
    return memberChangeSet;
  }

  /**
   * Returns the list of member repository IDs as it was set before the configuration change.
   *
   * @since 2.0
   */
  public List<String> getOldRepositoryMemberIds() {
    return Collections.unmodifiableList(oldRepositoryMemberIds);
  }

  /**
   * Returns the list of member repository IDs as it was set after the configuration change.
   *
   * @since 2.0
   */
  public List<String> getNewRepositoryMemberIds() {
    return Collections.unmodifiableList(newRepositoryMemberIds);
  }

  /**
   * Returns the list of repository IDs that were removed from the group by the configuration change.
   *
   * @since 2.0
   */
  public List<String> getRemovedRepositoryIds() {
    return Collections.unmodifiableList(removedRepositoryIds);
  }

  /**
   * Returns the list of repository IDs that were added to the group by the configuration change.
   *
   * @since 2.0
   */
  public List<String> getAddedRepositoryIds() {
    return Collections.unmodifiableList(addedRepositoryIds);
  }

  /**
   * Returns the list of repository IDs that were reordered within the group by the configuration change.
   *
   * @since 2.0
   */
  public List<String> getReorderedRepositoryIds() {
    return Collections.unmodifiableList(reorderedRepositoryIds);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "repositoryId=" + getRepository().getId() +
        ", addedMembers=" + getAddedRepositoryIds() +
        ", removedMembers=" + getRemovedRepositoryIds() +
        ", reorderedMembers=" + getReorderedRepositoryIds() +
        '}';
  }
}
