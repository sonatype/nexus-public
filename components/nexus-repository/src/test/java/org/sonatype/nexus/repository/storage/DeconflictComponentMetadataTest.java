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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.orient.entity.ConflictHook;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createComponent;

/**
 * Tests for {@link DeconflictComponentMetadata}.
 */
public class DeconflictComponentMetadataTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private ComponentEntityAdapter componentEntityAdapter;

  private Bucket bucket;

  private Component component;

  private ODocument initialComponentRecord;

  @Before
  public void setUp() {
    BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();

    ComponentFactory componentFactory = new ComponentFactory(emptySet());
    componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter, componentFactory, emptySet());

    ConflictHook conflictHook = new ConflictHook(true);
    componentEntityAdapter.enableConflictHook(conflictHook);

    componentEntityAdapter.setDeconflictSteps(asList(new DeconflictComponentMetadata()));

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      bucketEntityAdapter.register(db);
      componentEntityAdapter.register(db);
      bucket = new Bucket();
      bucket.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
      bucket.setRepositoryName("test-repo");
      bucketEntityAdapter.addEntity(db, bucket);
    }

    // first create our test component with some default values
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();

      component = createComponent(bucket, "some-group", "some-component", "1.0");
      initialComponentRecord = componentEntityAdapter.addEntity(db, component);

      db.commit();
    }

    // update the component, so it moves on from the initial record
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();

      component.attributes().child("test").set("test-component-key", "test-component-value");
      componentEntityAdapter.readFields(componentEntityAdapter.editEntity(db, component), component);

      db.commit();
    }
  }

  @Test
  public void lastUpdatedDifferencesAreDeconflicted() throws Exception {
    DateTime lastUpdated = component.lastUpdated();

    Thread.sleep(100);
    assertTrue(tryUpdate(component));

    assertThat(component.lastUpdated(), is(greaterThan(lastUpdated)));
  }

  private boolean tryUpdate(final Component component) {
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();

      ODocument copy = initialComponentRecord.copy();
      componentEntityAdapter.writeFields(copy, component);
      copy.save();

      try {
        db.commit();
        return true;
      }
      catch (OConcurrentModificationException e) {
        logger.debug("Update denied due to conflict", e);
        return false;
      }
    }
    finally {
      try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
        componentEntityAdapter.readFields(db.load(componentEntityAdapter.recordIdentity(component)), component);
      }
    }
  }
}
