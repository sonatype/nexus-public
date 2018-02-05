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
package org.sonatype.nexus.orient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Exercises backup/restore and export/import features.
 */
public class DatabaseExternalizerTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inFilesystem("test");

  @Test
  public void backupDatabase() throws Exception {
    File backupZip = temporaryFolder.newFile("backup.zip");

    createSampleDb();

    try (OutputStream out = new FileOutputStream(backupZip)) {
      database.getManager().externalizer("test").backup(out);
    }

    dropDb();

    try (InputStream in = new FileInputStream(backupZip)) {
      database.getManager().externalizer("test").restore(in);
    }

    assertLocation("Rome", "Italy");
  }

  @Test
  public void restoreDatabase() throws Exception {
    File backupZip = temporaryFolder.newFile("backup.zip");

    createSampleDb();

    try (OutputStream out = new FileOutputStream(backupZip)) {
      database.getManager().externalizer("test").backup(out);
    }

    updateSampleDb();

    assertLocation("Tataouine", "Tunisia");

    // by default restore doesn't overwrite existing DB
    try (InputStream in = new FileInputStream(backupZip)) {
      database.getManager().externalizer("test").restore(in);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
      assertThat(e.getMessage(), containsString("Database already exists"));
    }

    assertLocation("Tataouine", "Tunisia");

    // force overwrite
    try (InputStream in = new FileInputStream(backupZip)) {
      database.getManager().externalizer("test").restore(in, true);
    }

    assertLocation("Rome", "Italy");
  }

  @Test
  public void exportDatabase() throws Exception {
    File exportJson = temporaryFolder.newFile("export.json");

    createSampleDb();

    try (OutputStream out = new FileOutputStream(exportJson)) {
      database.getManager().externalizer("test").export(out);
    }

    dropDb();

    try (InputStream in = new FileInputStream(exportJson)) {
      database.getManager().externalizer("test").import_(in);
    }

    assertLocation("Rome", "Italy");
  }

  @Test
  public void exportDatabaseExcludingClass() throws Exception {
    File exportJson = temporaryFolder.newFile("export.json");

    createSampleDb();

    try (OutputStream out = new FileOutputStream(exportJson)) {
      database.getManager().externalizer("test").export(out, new HashSet<>(Arrays.asList("City")));
    }

    dropDb();

    try (InputStream in = new FileInputStream(exportJson)) {
      database.getManager().externalizer("test").import_(in);
    }

    assertLocationEmpty();
  }

  @Test
  public void importDatabase() throws Exception {
    File exportJson = temporaryFolder.newFile("export.json");

    createSampleDb();

    try (OutputStream out = new FileOutputStream(exportJson)) {
      database.getManager().externalizer("test").export(out);
    }

    updateSampleDb();

    assertLocation("Tataouine", "Tunisia");

    // by default import doesn't overwrite existing DB
    try (InputStream in = new FileInputStream(exportJson)) {
      database.getManager().externalizer("test").import_(in);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
      assertThat(e.getMessage(), containsString("Database already exists"));
    }

    assertLocation("Tataouine", "Tunisia");

    // force overwrite
    try (InputStream in = new FileInputStream(exportJson)) {
      database.getManager().externalizer("test").import_(in, true);
    }

    assertLocation("Rome", "Italy");
  }

  private void createSampleDb() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      ODocument doc = db.newInstance("Person");
      doc.field("name", "Luke")
          .field("surname", "Skywalker")
          .field("city", new ODocument("City")
              .field("name", "Rome")
              .field("country", "Italy"));
      doc.save();
    }
  }

  private void updateSampleDb() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      ODocument doc = db.browseClass("Person").current().getRecord();
      doc.field("city", new ODocument("City")
          .field("name", "Tataouine")
          .field("country", "Tunisia"));
      doc.save();
    }
  }

  private void assertLocation(String expectedName, String expectedCountry) {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      ODocument doc = db.browseClass("Person").current().getRecord();
      assertThat(doc.field("city.name"), is(expectedName));
      assertThat(doc.field("city.country"), is(expectedCountry));
    }
  }

  private void assertLocationEmpty() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      ODocument doc = db.browseClass("Person").current().getRecord();
      assertFalse(doc.containsField("city.name"));
      assertFalse(doc.containsField("city.country"));
    }
  }

  private void dropDb() {
    ODatabaseDocumentTx db = database.getInstance().connect();
    db.drop();
    assertFalse(db.exists());
  }
}
