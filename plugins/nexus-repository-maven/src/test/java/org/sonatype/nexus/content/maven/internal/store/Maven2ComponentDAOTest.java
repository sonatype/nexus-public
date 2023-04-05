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
package org.sonatype.nexus.content.maven.internal.store;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.content.maven.store.GAV;
import org.sonatype.nexus.content.maven.store.Maven2ComponentDAO;
import org.sonatype.nexus.content.maven.store.Maven2ComponentData;
import org.sonatype.nexus.content.maven.store.Maven2ContentRepositoryDAO;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.BlobRefTypeHandler;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.datastore.mybatis.CombUUID.combUUID;

public class Maven2ComponentDAOTest
    extends TestSupport
{
  private ContentRepositoryData contentRepository;

  private int repositoryId;

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME)
      .handle(new BlobRefTypeHandler())
      .access(Maven2ContentRepositoryDAO.class)
      .access(Maven2ComponentDAO.class);

  @Before
  public void setupContent() {
    contentRepository = new ContentRepositoryData();
    contentRepository.setConfigRepositoryId(new EntityUUID(combUUID()));
    contentRepository.setAttributes(newAttributes("repository"));

    createContentRepository(contentRepository);

    repositoryId = contentRepository.contentRepositoryId();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      Maven2ComponentDAO dao = session.access(Maven2ComponentDAO.class);
      generateComponent(1, "1.0.0", "1.0.0", dao);
      generateComponent(2, "1.0.0-20200914.113032-16", "1.0.0-SNAPSHOT", dao);
      generateComponent(3, "1.0.0-20201014.113032-17", "1.0.0-SNAPSHOT", dao);
      generateComponent(4, "1.0.0-20201114.113032-18", "1.0.0-SNAPSHOT", dao);
      session.getTransaction().commit();
    }
  }

  @Test
  public void findComponent() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      Maven2ComponentDAO dao = session.access(Maven2ComponentDAO.class);
      Optional<Component> component = dao.readComponent(1);
      assertTrue(component.isPresent());
    }
  }

  @Test
  public void findGavsWithSnaphots() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      Maven2ComponentDAO dao = session.access(Maven2ComponentDAO.class);
      Set<GAV> gavsWithSnaphots = dao.findGavsWithSnaphots(repositoryId, 2);
      assertThat(gavsWithSnaphots.size(), is(1));
      GAV gav = gavsWithSnaphots.stream().findFirst().get();
      assertThat(gav.group, is("group"));
      assertThat(gav.name, is("artifact"));
      assertThat(gav.baseVersion, is("1.0.0-SNAPSHOT"));
    }
  }

  @Test
  public void findGavsWithSnaphotsLessMinimum() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      Maven2ComponentDAO dao = session.access(Maven2ComponentDAO.class);

      Set<GAV> gavsWithSnaphots = dao.findGavsWithSnaphots(repositoryId, 3);
      assertThat(gavsWithSnaphots.size(), is(0));
    }
  }

  @Test
  public void findComponentsForGav() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      Maven2ComponentDAO dao = session.access(Maven2ComponentDAO.class);
      List<Maven2ComponentData> componentsForGav =
          dao.findComponentsForGav(repositoryId, "artifact", "group", "1.0.0-SNAPSHOT", "1.0.0");
      assertThat(componentsForGav.size(), is(3));
      List<String> componentVersions =
          componentsForGav.stream().map(component -> component.version()).collect(Collectors.toList());
      assertThat(componentVersions.contains("1.0.0-20200914.113032-16"), is(true));
      assertThat(componentVersions.contains("1.0.0-20201014.113032-17"), is(true));
      assertThat(componentVersions.contains("1.0.0-20201114.113032-18"), is(true));
    }
  }

  @Test
  public void selectSnapshotsAfterRelease() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      Maven2ComponentDAO dao = session.access(Maven2ComponentDAO.class);
      int[] componentIds = dao.selectSnapshotsAfterRelease(repositoryId, 0);
      assertThat(componentIds.length, is(3));

      List<Integer> ids = Arrays.stream(componentIds).boxed().collect(Collectors.toList());
      assertThat(ids.contains(2), is(true));
      assertThat(ids.contains(3), is(true));
      assertThat(ids.contains(4), is(true));
    }
  }

  @Test
  public void selectSnapshotsAfterReleaseInGracePeriod() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      Maven2ComponentDAO dao = session.access(Maven2ComponentDAO.class);
      int[] componentIds = dao.selectSnapshotsAfterRelease(repositoryId, 1);
      assertThat(componentIds.length, is(0));
    }
  }

  private void createContentRepository(final ContentRepositoryData contentRepository) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(Maven2ContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }
  }

  protected NestedAttributesMap newAttributes(final String key) {
    return new NestedAttributesMap("attributes", new HashMap<>(ImmutableMap.of(key, "test-value")));
  }

  private void generateComponent(final int componentId,
                                 final String version,
                                 final String baseVersion,
                                 final Maven2ComponentDAO dao)
  {
    Maven2ComponentData component = new Maven2ComponentData();
    component.setComponentId(componentId);
    component.setNamespace("group");
    component.setName("artifact");
    component.setVersion(version);
    component.setBaseVersion(baseVersion);
    component.setAttributes(newAttributes("component"));
    component.setKind("aKind");
    component.setRepositoryId(repositoryId);
    dao.createComponent(component, false);
    dao.updateBaseVersion(component);
  }
}
