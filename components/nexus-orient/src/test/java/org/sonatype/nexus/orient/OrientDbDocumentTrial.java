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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.io.Hex;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.storage.ORecordMetadata;
import org.junit.After;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Trials of using OrientDB using document-api.
 */
public class OrientDbDocumentTrial
    extends TestSupport
{
  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private ODatabaseDocumentTx createDatabase() {
    return new ODatabaseDocumentTx("memory:testdb").create();
  }

  private ODatabaseDocumentTx openDatabase() {
    return new ODatabaseDocumentTx("memory:testdb").open("admin", "admin");
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

  @After
  public void tearDown() throws Exception {
    try (ODatabaseDocumentTx db = openDatabase()) {
      db.drop();
    }
  }

  @Test
  public void documentTx() throws Exception {
    try (ODatabaseDocumentTx db = createDatabase()) {
      log("DB: {}", db);
      log("DB size: {}", db.getSize());

      // NOTE: this throws IAE since there are no "Person" classes yet
      //log("Person count: {}", db.countClass("Person"));

      ODocument doc = createPerson(db);
      log("Document: {}", doc);
      log("Document size: {}", doc.getSize());
      log("Document (default) JSON: {}", doc.toJSON());
      log("Document (custom) JSON: {}", doc.toJSON("rid,version,class,type,attribSameRow,keepTypes,alwaysFetchEmbedded,prettyPrint,fetchPlan:*:0"));

      log("DB size: {}", db.getSize());
      log("Person count: {}", db.countClass("Person"));
    }

    log("reopen");
    try (ODatabaseDocumentTx db = openDatabase()) {
      log("DB: {}", db);
      log("DB size: {}", db.getSize());
      log("Person count: {}", db.countClass("Person"));

      log("delete all");
      db.command(new OCommandSQL("delete from Person")).execute();
      log("DB: {}", db);
      log("DB size: {}", db.getSize());
      log("Person count: {}", db.countClass("Person"));
    }
  }

  @Test
  public void globalPool() throws Exception {
    // first ensure the database is created, and close the connection
    createDatabase().close();

    // now we should be able to get a pooled connection
    try (ODatabaseDocumentTx db = ODatabaseDocumentPool.global().acquire("memory:testdb", "admin", "admin")) {
      log(db);
      ODocument doc = createPerson(db);
      log(doc);
    }
  }

  @Test
  public void recordIdEncoding() throws Exception {
    try (ODatabaseDocumentTx db = createDatabase()) {
      ODocument doc = createPerson(db);
      log("New Document: {}", doc);

      ORID rid = doc.getIdentity();
      log("RID: {}", rid);

      String encoded = Hex.encode(rid.toStream());
      log("Hex Encoded: {}", encoded);

      ORID decoded = new ORecordId().fromStream(Hex.decode(encoded));
      log("Decoded RID: {}", decoded);

      assertThat(decoded, is(rid));

      doc = db.getRecord(decoded);
      log("Fetched Document: {}", doc);
    }
  }

  @Test
  public void documentExistance() throws Exception {
    try (ODatabaseDocumentTx db = createDatabase()) {
      ODocument doc = createPerson(db);
      log("Document: {}", doc);

      ORID rid = doc.getIdentity();
      log("RID: {}", rid);

      ORecordMetadata md = db.getRecordMetadata(rid);
      log("Metadata: {}", md);
      assertThat(md, notNullValue());
    }
  }

  @Test
  public void loadNonExistingDocument() throws Exception {
    try (ODatabaseDocumentTx db = createDatabase()) {
      ORID rid = new ORecordId("#1:2"); // NOTE: #1:1 will return a record, #1:2 will return null
      log("RID: {}", rid);

      ORecordMetadata md = db.getRecordMetadata(rid);
      log("Metadata: {}", md);
      assertThat(md, nullValue());

      ORecordInternal record = db.load(rid);
      log("Record: {}", record);
      assertThat(record, nullValue());
    }
  }

  @Test
  public void schema() throws Exception {
    try (ODatabaseDocumentTx db = createDatabase()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass eventData = schema.createClass("EventData");
      eventData.createProperty("type", OType.STRING);
      eventData.createProperty("timestamp", OType.LONG);
      eventData.createProperty("userId", OType.STRING);
      eventData.createProperty("sessionId", OType.STRING);
      eventData.createProperty("attributes", OType.EMBEDDEDMAP);

      log("Class: {}", eventData);
      log("Properties: {}", eventData.properties());

      // can count since we have defined schema
      log("Count: {}", db.countClass("EventData"));
    }

    try (ODatabaseDocumentTx db = openDatabase()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass eventData = schema.getClass("EventData");

      log("Class: {}", eventData);
      log("Properties: {}", eventData.properties());
    }
  }
}
