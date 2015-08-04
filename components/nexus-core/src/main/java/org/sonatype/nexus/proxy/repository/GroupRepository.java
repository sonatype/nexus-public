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

import java.util.List;

import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageItem;

/**
 * A group repository is simply as it's name says, a repository that is backed by a group of other repositories. There
 * is one big constraint, they are READ ONLY. Usually, if you try a write/delete operation against this kind of
 * repository, you are doing something wrong. Deploys/writes and deletes should be done directly against the
 * hosted/proxied repositories, not against these "aggregated" ones.
 *
 * @author cstamas
 */
@RepositoryType(pathPrefix = "groups")
public interface GroupRepository
    extends Repository
{
  /**
   * Returns the unmodifiable ID list of the members of this group.
   */
  List<String> getMemberRepositoryIds();

  /**
   * Sets the members of this group.
   */
  void setMemberRepositoryIds(List<String> repositories)
      throws NoSuchRepositoryException, InvalidGroupingException;

  /**
   * Adds a member to this group.
   */
  void addMemberRepositoryId(String repositoryId)
      throws NoSuchRepositoryException, InvalidGroupingException;

  /**
   * Removes a member from this group.
   */
  void removeMemberRepositoryId(String repositoryId);

  /**
   * Returns the unmodifiable list of Repositories that are group members in this GroupRepository. The repo order
   * within list is repo rank (the order how they will be processed), so processing is possible by simply iterating
   * over resulting list.
   *
   * @return a List<Repository>
   */
  List<Repository> getMemberRepositories();

  /**
   * Returns the unmodifiable list of Transitive Repositories that are group members in this GroupRepository. This
   * method differs from {@link #getMemberRepositories()} by resolving all inner groups member as well. <b>The
   * resulting list won't contain any GroupRepository.</b>
   *
   * @return a List<Repository>
   */
  List<Repository> getTransitiveMemberRepositories();

  /**
   * Returns the unmodifiable ID list of the transitive members of this group. This method differs from
   * {@link #getMemberRepositoryIds()} by resolving all inner groups member as well. <b>The resulting list won't
   * contain any GroupRepository.</b>
   *
   * @return a List<Repository>
   */
  List<String> getTransitiveMemberRepositoryIds();

  /**
   * Returns the list of available items in the group for same path. The resulting list keeps the order of reposes
   * queried for path. Never returns {@code null}, if nothing found, {@link GroupItemNotFoundException} is thrown.
   */
  List<StorageItem> doRetrieveItems(ResourceStoreRequest request)
      throws GroupItemNotFoundException, StorageException;
}
