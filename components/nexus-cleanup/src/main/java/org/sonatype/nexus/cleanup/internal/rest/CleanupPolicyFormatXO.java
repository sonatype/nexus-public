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
package org.sonatype.nexus.cleanup.internal.rest;

import java.util.Set;

/**
 * @since 3.29
 */
public class CleanupPolicyFormatXO
{
  private String id;

  private String name;

  private Set<String> availableCriteria;

  public CleanupPolicyFormatXO(final String id, final String name, final Set<String> availableCriteria) {
    this.id = id;
    this.name = name;
    this.availableCriteria = availableCriteria;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setAvailableCriteria(final Set<String> availableCriteria) {
    this.availableCriteria = availableCriteria;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Set<String> getAvailableCriteria() {
    return availableCriteria;
  }
}
