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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

/**
 * Thrown by the {@link GroupRepository#retrieveItem(ResourceStoreRequest)},
 * {@link GroupRepository#retrieveItem(boolean, ResourceStoreRequest)} and
 * {@link GroupRepository#doRetrieveItems(ResourceStoreRequest)} methods only, when all the members and group
 * repository
 * itself failed to retrieve item corresponding to request in non-hard-fail (ie. some internal error or some other
 * condition that stops group processing immediately) way.
 *
 * @author cstamas
 * @since 2.1
 */
public class GroupItemNotFoundException
    extends ItemNotFoundException
{
  private static final long serialVersionUID = -863009398540333419L;

  private final Map<Repository, Throwable> memberReasons;

  /**
   * Constructor for group thrown "not found" exception providing information about whole tree being processed and
   * reasons why the grand total result is "not found.
   */
  public GroupItemNotFoundException(final ItemNotFoundInRepositoryReason reason,
                                    final Map<Repository, Throwable> memberReasons)
  {
    super(reason);
    // copy it and make it unmodifiable
    this.memberReasons = Collections.unmodifiableMap(new HashMap<Repository, Throwable>(memberReasons));
  }

  /**
   * Constructor for group thrown "not found" exception providing information about whole tree being processed and
   * reasons why the grand total result is "not found.
   */
  public GroupItemNotFoundException(final ResourceStoreRequest request, final GroupRepository repository,
                                    final Map<Repository, Throwable> memberReasons)
  {
    this(reasonFor(request, repository, "Path %s not found in group repository %s.",
        request.getRequestPath(), RepositoryStringUtils.getHumanizedNameString(repository)), memberReasons);
  }

  @Override
  public ItemNotFoundInRepositoryReason getReason() {
    return (ItemNotFoundInRepositoryReason) super.getReason();
  }

  /**
   * Returns the map of reasons ({@link Throwable} instances) per Repository.
   *
   * @return the map of reasons.
   */
  public Map<Repository, Throwable> getMemberReasons() {
    return memberReasons;
  }
}
