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
package org.sonatype.nexus.proxy;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDelete;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDeleteItem;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDeleteRoot;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

import com.google.common.eventbus.Subscribe;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * NXCM-3747 Test: implementation separates the events sent out when deleting a collection, making a distinction about
 * "root" (where actual delete action is invoked) versus all the "collateral damage" that a recursive deletion would
 * make if action is made against a collection that has non-collection siblings (ie. a folder that has subfolders and
 * some files in it).
 *
 * @author cstamas
 */
public class DeleteEventsTest
    extends AbstractProxyTestEnvironment
{
  private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);
    this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder(ss);
    return jettyTestsuiteEnvironmentBuilder;
  }

  @Test
  public void deleteCollectionWithSiblings()
      throws Exception
  {
    final ProxyRepository repo1 = getRepositoryRegistry().getRepositoryWithFacet("repo1", ProxyRepository.class);
    repo1.retrieveItem(new ResourceStoreRequest("/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.pom"));

    // install listener
    final DeleteEventsListener listener = new DeleteEventsListener();
    eventBus().register(listener);

    // perform delete
    repo1.deleteItem(new ResourceStoreRequest("/org"));

    // 3 events: the actual collection being deleted + 2 non-collections discovered by diving in recursively (POM +
    // SHA1)
    assertThat(listener.getDeleteEvents().size(), equalTo(3));

    assertThat(listener.getDeleteEvents().get(0).getClass().getName(),
        equalTo(RepositoryItemEventDeleteRoot.class.getName()));
    assertThat(listener.getDeleteEvents().get(1).getClass().getName(),
        equalTo(RepositoryItemEventDeleteItem.class.getName()));
    assertThat(listener.getDeleteEvents().get(2).getClass().getName(),
        equalTo(RepositoryItemEventDeleteItem.class.getName()));
  }

  @Test
  public void deleteEmptyCollection()
      throws Exception
  {
    final ProxyRepository repo1 = getRepositoryRegistry().getRepositoryWithFacet("repo1", ProxyRepository.class);
    repo1.retrieveItem(new ResourceStoreRequest("/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.pom"));

    // perform delete a bit lower
    repo1.deleteItem(new ResourceStoreRequest("/org/slf4j"));

    // install listener
    final DeleteEventsListener listener = new DeleteEventsListener();
    eventBus().register(listener);

    // perform delete
    repo1.deleteItem(new ResourceStoreRequest("/org"));

    // 1 events: the actual collection being deleted
    assertThat(listener.getDeleteEvents().size(), equalTo(1));
    assertThat(listener.getDeleteEvents().get(0).getClass().getName(),
        equalTo(RepositoryItemEventDeleteRoot.class.getName()));
  }

  @Test
  public void deleteNonCollection()
      throws Exception
  {
    final ProxyRepository repo1 = getRepositoryRegistry().getRepositoryWithFacet("repo1", ProxyRepository.class);
    repo1.retrieveItem(new ResourceStoreRequest("/spoof/spoof/1.0/spoof-1.0.txt"));
    repo1.retrieveItem(new ResourceStoreRequest("/spoof/maven-metadata.xml"));

    // install listener
    final DeleteEventsListener listener = new DeleteEventsListener();
    eventBus().register(listener);

    // perform delete
    repo1.deleteItem(new ResourceStoreRequest("/spoof/maven-metadata.xml"));

    // 1 events: the actual non-collection being deleted, no recursion happens
    assertThat(listener.getDeleteEvents().size(), equalTo(1));
    assertThat(listener.getDeleteEvents().get(0).getClass().getName(),
        equalTo(RepositoryItemEventDeleteRoot.class.getName()));
  }

  // ==

  public static class DeleteEventsListener
  {
    private final List<RepositoryItemEventDelete> deleteEvents;

    public DeleteEventsListener() {
      this.deleteEvents = new ArrayList<RepositoryItemEventDelete>();
    }

    public List<RepositoryItemEventDelete> getDeleteEvents() {
      return deleteEvents;
    }

    @Subscribe
    public void onEvent(RepositoryItemEventDelete evt) {
      deleteEvents.add((RepositoryItemEventDelete) evt);
    }
  }

}
