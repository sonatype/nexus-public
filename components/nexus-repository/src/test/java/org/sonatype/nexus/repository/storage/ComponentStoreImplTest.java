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
package org.sonatype.nexus.repository.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndexCursor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createComponent;

public class ComponentStoreImplTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private ComponentEntityAdapter entityAdapter;

  private ComponentStoreImpl underTest;

  private Bucket bucket;

  @Before
  public void setUp() {
    BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();
    ComponentFactory componentFactory = new ComponentFactory(emptySet());
    entityAdapter = new ComponentEntityAdapter(bucketEntityAdapter, componentFactory, emptySet());

    underTest = new ComponentStoreImpl(database.getInstanceProvider(), entityAdapter);

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      bucketEntityAdapter.register(db);
      entityAdapter.register(db);

      bucket = new Bucket();
      bucket.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
      bucket.setRepositoryName("test-repo");
      bucketEntityAdapter.addEntity(db, bucket);
    }
  }

  @Test
  public void getNextPageReturnsEntriesByPages() {
    int limit = 2;

    Component entity1 = createComponent(bucket, "group1", "name1", "version1");
    Component entity2 = createComponent(bucket, "group2", "name2", "version2");
    Component entity3 = createComponent(bucket, "group3", "name3", "version3");

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.addEntity(db, entity1);
      entityAdapter.addEntity(db, entity2);
      entityAdapter.addEntity(db, entity3);
    }

    OIndexCursor cursor = underTest.getIndex(ComponentEntityAdapter.I_GROUP_NAME_VERSION_INSENSITIVE).cursor();

    List<Entry<OCompositeKey, EntityId>> page1 = underTest.getNextPage(cursor, limit);
    List<Entry<OCompositeKey, EntityId>> page2 = underTest.getNextPage(cursor, limit);
    assertThat(page1.size(), is(2));
    assertThat(page1.get(0).getValue(), is(EntityHelper.id(entity1)));
    assertThat(page1.get(1).getValue(), is(EntityHelper.id(entity2)));
    assertThat(page2.size(), is(1));
    assertThat(page2.get(0).getValue(), is(EntityHelper.id(entity3)));
  }

  @Test
  public void getNextPageStopsAtNullEntry() {
    int limit = 2;

    Component entity1 = createComponent(bucket, "group1", "name1", "version1");

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.addEntity(db, entity1);
    }

    OIndexCursor cursor = underTest.getIndex(ComponentEntityAdapter.I_GROUP_NAME_VERSION_INSENSITIVE).cursor();

    List<Entry<OCompositeKey, EntityId>> page1 = underTest.getNextPage(cursor, limit);

    assertThat(page1.size(), is(1));
    assertThat(page1.get(0).getValue(), is(EntityHelper.id(entity1)));
  }
}
