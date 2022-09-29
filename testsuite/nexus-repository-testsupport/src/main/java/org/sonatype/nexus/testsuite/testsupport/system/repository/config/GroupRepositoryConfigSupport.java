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
package org.sonatype.nexus.testsuite.testsupport.system.repository.config;

import java.util.function.Function;

import org.sonatype.nexus.repository.Repository;

public abstract class GroupRepositoryConfigSupport<THIS>
    extends RepositoryConfigSupport<THIS>
    implements GroupRepositoryConfig<THIS>
{
  private static final String GROUP_RECIPE_SUFFIX = "-group";

  private String[] members;

  private String groupWriteMember = "None";

  public GroupRepositoryConfigSupport(final Function<THIS, Repository> factory) {
    super(factory);
  }

  @Override
  public String getRecipe() {
    return getFormat() + GROUP_RECIPE_SUFFIX;
  }

  @Override
  public THIS withMembers(final String... members) {
    this.members = members;
    return toTHIS();
  }

  @Override
  public String[] getMembers() {
    return members;
  }

  @Override
  public THIS withGroupWriteMember(final String groupWriteMember) {
    this.groupWriteMember = groupWriteMember;
    return toTHIS();
  }

  @Override
  public String getGroupWriteMember() {
    return groupWriteMember;
  }
}
