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
package org.sonatype.nexus.client.rest.jersey;

import java.net.MalformedURLException;

import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.condition.EditionConditions;
import org.sonatype.nexus.client.core.condition.LogicalConditions;
import org.sonatype.nexus.client.core.condition.NexusStatusConditions;
import org.sonatype.nexus.client.core.condition.VersionConditions;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.rest.BaseUrl;
import org.sonatype.nexus.client.rest.NexusClientFactory;
import org.sonatype.sisu.goodies.testsupport.group.External;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(External.class) // This test contacts external servers
public class BasicJerseyNexusClientIT
    extends JerseyNexusClientTestSupport
{

  @Test
  public void createWithGoodUrl()
      throws MalformedURLException
  {
    final NexusClientFactory factory = new JerseyNexusClientFactory();
    final NexusClient client = factory.createFor(BaseUrl.baseUrlFrom("https://repository.sonatype.org/"));
    Assert.assertNotNull(client.getNexusStatus());
  }

  @Test(expected = IllegalStateException.class)
  public void createWithGoodUrlButNotAcceptableCriteria1()
      throws MalformedURLException
  {
    try {
      // RSO as NOT any modern instance? Impossible.
      final NexusClientFactory factory =
          new JerseyNexusClientFactory(LogicalConditions.not(NexusStatusConditions.anyModern()));
      final NexusClient client = factory.createFor(BaseUrl.baseUrlFrom("https://repository.sonatype.org/"));
    }
    catch (IllegalStateException e) {
      e.printStackTrace();
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void createWithGoodUrlButNotAcceptableCriteria2()
      throws MalformedURLException
  {
    try {
      // RSO with version 1.0? No chance, those times has passed.
      final NexusClientFactory factory =
          new JerseyNexusClientFactory(LogicalConditions.and(EditionConditions.anyEdition(),
              VersionConditions.withVersion("[1.0]")));
      final NexusClient client = factory.createFor(BaseUrl.baseUrlFrom("https://repository.sonatype.org/"));
    }
    catch (IllegalStateException e) {
      e.printStackTrace();
      throw e;
    }
  }

  @Test(expected = NexusClientHandlerException.class)
  public void createWithWrongBaseUrlNotExists()
      throws MalformedURLException
  {
    final NexusClientFactory factory = new JerseyNexusClientFactory();
    // this will fail, nonexistent hostname
    factory.createFor(BaseUrl.baseUrlFrom("https://foobar123.sonatype.org/"));
  }

}
