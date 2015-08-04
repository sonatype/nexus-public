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
package org.sonatype.nexus.proxy.utils;

import org.sonatype.nexus.proxy.repository.Repository;

public class RepositoryStringUtils
{
  public static String getFormattedMessage(final String string, final Repository repository) {
    return String.format(string, getHumanizedNameString(repository));
  }

  public static String getHumanizedNameString(final Repository repository) {
    return String.format("\"%s\" [id=%s]", repository.getName(), repository.getId());
  }

  public static String getFullHumanizedNameString(final Repository repository) {
    return String.format("%s[contentClass=%s][mainFacet=%s]", getHumanizedNameString(repository),
        repository.getRepositoryContentClass().getName(), repository.getRepositoryKind().getMainFacet().getName());
  }
}
