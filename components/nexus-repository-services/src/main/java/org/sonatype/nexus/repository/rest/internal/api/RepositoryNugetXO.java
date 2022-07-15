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
package org.sonatype.nexus.repository.rest.internal.api;

import java.util.Collection;

public class RepositoryNugetXO extends RepositoryDetailXO
{
  private final String nugetVersion;
  private final Collection<String> memberNames;

  public RepositoryNugetXO(
          final String name,
          final String type,
          final String format,
          final String url,
          final RepositoryStatusXO status,
          final String nugetVersion,
          final Collection<String> memberNames)
  {
    super(name, type, format, url, status);
    this.nugetVersion = nugetVersion;
    this.memberNames = memberNames;
  }

  public String getNugetVersion() {
    return nugetVersion;
  }

  public Collection<String> getMemberNames() {
    return memberNames;
  }
}
