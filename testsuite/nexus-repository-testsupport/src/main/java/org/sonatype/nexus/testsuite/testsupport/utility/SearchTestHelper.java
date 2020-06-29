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
package org.sonatype.nexus.testsuite.testsupport.utility;

import javax.ws.rs.client.WebTarget;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.query.SearchQueryService;

/**
 * This interface was introduced so as to allow us to continue to use Search for assertions when running the ITs in
 * Orient mode. This is due to Search not being available for SQL mode at the time the DataStore/SQL version
 * of Maven Hosted was implemented.
 */
public interface SearchTestHelper
{
  void waitForSearch() throws Exception;

  void verifyComponentExists(
      WebTarget nexusSearchWebTarget,
      Repository repository,
      String name,
      String version,
      boolean exists)
      throws Exception;

  SearchQueryService queryService();
}
