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
package org.sonatype.nexus.repository.content.store;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;

import org.junit.Test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Test {@link ContentRepositoryDAO}.
 */
public class ContentRepositoryDAOTest
    extends ExampleContentTestSupport
{
  @Test
  public void testCrudOperations() throws InterruptedException {

    ContentRepositoryData contentRepository1 = randomContentRepository();
    ContentRepositoryData contentRepository2 = randomContentRepository();

    EntityId configRepositoryId1 = contentRepository1.configRepositoryId();
    EntityId configRepositoryId2 = contentRepository2.configRepositoryId();

    ContentRepository tempResult;

    // CREATE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);

      assertThat(dao.browseContentRepositories(), emptyIterable());

      dao.createContentRepository(contentRepository1);

      assertThat(dao.browseContentRepositories(),
          contains(allOf(sameConfigRepository(contentRepository1), sameAttributes(contentRepository1))));

      dao.createContentRepository(contentRepository2);

      assertThat(dao.browseContentRepositories(),
          contains(allOf(sameConfigRepository(contentRepository1), sameAttributes(contentRepository1)),
              allOf(sameConfigRepository(contentRepository2), sameAttributes(contentRepository2))));

      session.getTransaction().commit();
    }

    // TRY CREATE AGAIN

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);

      ContentRepositoryData duplicate = new ContentRepositoryData();
      duplicate.repositoryId = contentRepository1.repositoryId;
      duplicate.setConfigRepositoryId(contentRepository1.configRepositoryId());
      duplicate.setAttributes(newAttributes("duplicate"));
      dao.createContentRepository(duplicate);

      session.getTransaction().commit();
      fail("Cannot create the same repository twice");
    }
    catch (DuplicateKeyException e) {
      logger.debug("Got expected exception", e);
    }

    // READ

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);

      assertFalse(dao.readContentRepository(new EntityUUID(new UUID(0, 0))).isPresent());

      tempResult = dao.readContentRepository(configRepositoryId1).get();
      assertThat(tempResult, sameConfigRepository(contentRepository1));
      assertThat(tempResult, sameAttributes(contentRepository1));

      tempResult = dao.readContentRepository(configRepositoryId2).get();
      assertThat(tempResult, sameConfigRepository(contentRepository2));
      assertThat(tempResult, sameAttributes(contentRepository2));
    }

    // UPDATE

    Thread.sleep(2); // NOSONAR make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);

      tempResult = dao.readContentRepository(configRepositoryId1).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();

      contentRepository1.attributes("custom-section-1").set("custom-key-1", "more-test-values-1");
      dao.updateContentRepositoryAttributes(contentRepository1);

      tempResult = dao.readContentRepository(configRepositoryId1).get();
      assertThat(tempResult, sameConfigRepository(contentRepository1));
      assertThat(tempResult, sameAttributes(contentRepository1));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed

      tempResult = dao.readContentRepository(configRepositoryId2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      contentRepository2.repositoryId = null; // check a 'detached' entity with no internal id can be updated
      contentRepository2.attributes("custom-section-2").set("custom-key-2", "more-test-values-2");
      dao.updateContentRepositoryAttributes(contentRepository2);

      tempResult = dao.readContentRepository(configRepositoryId2).get();
      assertThat(tempResult, sameConfigRepository(contentRepository2));
      assertThat(tempResult, sameAttributes(contentRepository2));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed

      session.getTransaction().commit();
    }

    // UPDATE AGAIN

    Thread.sleep(2); // NOSONAR make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);

      tempResult = dao.readContentRepository(configRepositoryId1).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();

      contentRepository1.attributes("custom-section-1").set("custom-key-1", "more-test-values-again");
      dao.updateContentRepositoryAttributes(contentRepository1);

      tempResult = dao.readContentRepository(configRepositoryId1).get();
      assertThat(tempResult, sameConfigRepository(contentRepository1));
      assertThat(tempResult, sameAttributes(contentRepository1));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes changed again

      tempResult = dao.readContentRepository(configRepositoryId2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      dao.updateContentRepositoryAttributes(contentRepository2);

      tempResult = dao.readContentRepository(configRepositoryId2).get();
      assertThat(tempResult, sameConfigRepository(contentRepository2));
      assertThat(tempResult, sameAttributes(contentRepository2));
      assertThat(tempResult.created(), is(oldCreated));
      assertThat(tempResult.lastUpdated(), is(oldLastUpdated)); // won't have changed as attributes haven't changed

      session.getTransaction().commit();
    }

    // DELETE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      ContentRepositoryData candidate = new ContentRepositoryData();

      candidate.setConfigRepositoryId(configRepositoryId1);
      assertTrue(dao.deleteContentRepository(candidate));

      assertThat(dao.browseContentRepositories(),
          contains(allOf(sameConfigRepository(contentRepository2), sameAttributes(contentRepository2))));

      candidate.setConfigRepositoryId(configRepositoryId2);
      assertTrue(dao.deleteContentRepository(candidate));

      assertThat(dao.browseContentRepositories(), emptyIterable());

      candidate.setConfigRepositoryId(new EntityUUID(new UUID(0, 0)));
      assertFalse(dao.deleteContentRepository(candidate));
    }
  }
}
