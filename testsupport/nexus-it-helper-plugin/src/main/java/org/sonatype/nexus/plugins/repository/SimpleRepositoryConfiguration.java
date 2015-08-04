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
package org.sonatype.nexus.plugins.repository;

import org.sonatype.nexus.proxy.repository.AbstractRepositoryConfiguration;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class SimpleRepositoryConfiguration
    extends AbstractRepositoryConfiguration
{
  public static final String HELLO_SAID_COUNT = "helloSaidCount";

  public SimpleRepositoryConfiguration(Xpp3Dom configuration) {
    super(configuration);
  }

  public int getSaidHelloCount() {
    return Integer.parseInt(getNodeValue(getRootNode(), HELLO_SAID_COUNT, "0"));
  }

  public void setSaidHelloCount(int cnt) {
    setNodeValue(getRootNode(), HELLO_SAID_COUNT, String.valueOf(cnt));
  }
}
