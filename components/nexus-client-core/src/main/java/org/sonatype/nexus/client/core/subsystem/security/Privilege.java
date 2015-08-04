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
package org.sonatype.nexus.client.core.subsystem.security;

import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.client.core.subsystem.Entity;

/**
 * A Nexus privilege.
 *
 * @since 2.4
 */
public interface Privilege
    extends Entity<Privilege>
{

  String name();

  Privilege withName(String value);

  String description();

  Privilege withDescription(String value);

  String type();

  String repositoryId();

  Privilege withRepositoryId(String repositoryId);

  List<String> methods();

  Privilege withMethods(String... methods);

  String targetId();

  Privilege withTargetId(String targetId);

  String repositoryGroupId();

  Privilege withRepositoryGroupId(String groupId);

  Collection<Privilege> create();
}
