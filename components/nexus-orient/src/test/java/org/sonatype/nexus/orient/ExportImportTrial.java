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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.sonatype.goodies.testsupport.TestSupport;

import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Trials of using OrientDB export and import functions.
 */
public class ExportImportTrial
    extends TestSupport
{
  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private File basedir;

  @Before
  public void setUp() throws Exception {
    this.basedir = util.createTempDir("db");
  }

  private ODatabaseDocumentTx createDatabase(final String name) {
    File dir = new File(basedir, name);
    return new ODatabaseDocumentTx("plocal:" + dir).create();
  }

  private ODatabaseDocumentTx openDatabase(final String name) {
    File dir = new File(basedir, name);
    return new ODatabaseDocumentTx("plocal:" + dir).open("admin", "admin");
  }

  private ODocument createPerson(final ODatabaseDocumentTx db) {
    ODocument doc = db.newInstance("Person");
    doc.field("name", "Luke");
    doc.field("surname", "Skywalker");
    doc.field("city", new ODocument("City")
        .field("name", "Rome")
        .field("country", "Italy"));
    doc.save();
    return doc;
  }

  @Test
  public void backupDatabase() throws Exception {
    try (ODatabaseDocumentTx db = createDatabase("test")) {
      ODocument doc = createPerson(db);

      // Backup makes ZIP files
      File file = File.createTempFile("export-", ".zip", util.getTmpDir());
      log("Exporting to: {}", file);

      try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
        OCommandOutputListener listener = new OCommandOutputListener()
        {
          @Override
          public void onMessage(final String text) {
            log("> {}", text.trim());
          }
        };
        db.backup(output, null, null, listener, 9, 16384);
      }
    }
  }

  @Test
  public void exportImportNew() throws Exception {
    File file;

    try (ODatabaseDocumentTx db = createDatabase("test")) {
      ODocument doc = createPerson(db);

      file = File.createTempFile("export-", ".gz", util.getTmpDir());
      log("Exporting to: {}", file);

      ODatabaseExport exporter = new ODatabaseExport(db, file.getPath(), new OCommandOutputListener() {
        @Override
        public void onMessage(final String text) {
          log("> {}", text.trim());
        }
      });
      exporter.exportDatabase();
    }

    try (ODatabaseDocumentTx db = createDatabase("test2")) {
      log("Importing from: {}", file);

      ODatabaseImport importer = new ODatabaseImport(db, file.getPath(), new OCommandOutputListener() {
        @Override
        public void onMessage(final String text) {
          log("> {}", text.trim());
        }
      });
      importer.importDatabase();
    }
  }
}
