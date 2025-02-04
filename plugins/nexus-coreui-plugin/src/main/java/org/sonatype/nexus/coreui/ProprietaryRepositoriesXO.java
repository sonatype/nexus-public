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
package org.sonatype.nexus.coreui;

import java.util.List;

/**
 * Proprietary Repositories Settings exchange object.
 *
 * @since 3.30
 */
public class ProprietaryRepositoriesXO
{
  private List<String> enabledRepositories;

  public List<String> getEnabledRepositories() {
    return enabledRepositories;
  }

  public void setEnabledRepositories(List<String> enabledRepositories) {
    this.enabledRepositories = enabledRepositories;
  }

  @Override
  public String toString() {
    return "ProprietaryRepositoriesXO{" +
        "enabledRepositories=" + enabledRepositories +
        '}';
  }
}
