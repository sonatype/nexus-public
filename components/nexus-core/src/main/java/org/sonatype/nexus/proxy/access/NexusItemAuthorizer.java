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

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.targets.TargetSet;

/**
 * Authorizes the Repository requests against permissions.
 *
 * @author cstamas
 */
public interface NexusItemAuthorizer
{
  public static final String VIEW_REPOSITORY_KEY = "repository";

  /**
   * Authorizes TargetSet.
   *
   * @deprecated Use {@link #authorizePath(Repository, ResourceStoreRequest, Action)} method instead.
   */
  @Deprecated
  public boolean authorizePath(TargetSet matched, Action action);

  /**
   * Returns groups for target set.
   *
   * @deprecated Use {@link #authorizePath(Repository, ResourceStoreRequest, Action)} method instead.
   */
  @Deprecated
  public TargetSet getGroupsTargetSet(Repository repository, ResourceStoreRequest request);

  /**
   * Authorizes a repository level path against an action. Use when you have a repository path, ie. filtering of
   * search results or feeds with links to repository.
   */
  boolean authorizePath(Repository repository, ResourceStoreRequest request, Action action);

  /**
   * A shorthand for "view" permission.
   */
  boolean isViewable(String objectType, String objectId);


  /**
   * Used to authorize a simple permission string.
   */
  boolean authorizePermission(String permission);

}
