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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.schema.clusterselection.ODefaultClusterSelectionStrategy;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.After;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Trials of using OrientDB using clusters.
 */
public class OrientDbClusterTrial
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

  @After
  public void tearDown() throws Exception {
    try (ODatabaseDocumentTx db = openDatabase()) {
      db.drop();
    }
  }

  @Test
  public void maxClusters() throws Exception {
    log(" * Create database");
    try (ODatabaseDocumentTx db = createDatabase()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.createClass("Message");
      type.setClusterSelection(ODefaultClusterSelectionStrategy.NAME);
      type.createProperty("text", OType.STRING);
    }

    try (ODatabaseDocumentTx db = openDatabase()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.getClass("Message");
      int previousClusterId = type.getDefaultClusterId();

      for (int c = 0; c < 32_767 - 10; c++) {
        log("Previous cluster-id: {}", previousClusterId);

        log(" * Create new cluster #{}", c);

        // create new cluster
        String newClusterName = String.format("Message_%s", System.currentTimeMillis());
        type.addCluster(newClusterName);
        int newClusterId = db.getClusterIdByName(newClusterName);
        type.setDefaultClusterId(newClusterId);
        log("New cluster; id: {}, name: {}", newClusterId, newClusterName);

        log(" * create records");
        for (int i = 0; i < 1; i++) {
          db.newInstance("Message")
              .field("text", String.format("Hi %s", System.currentTimeMillis()))
              .save();
        }

        log(" * truncating cluster: id: {}", previousClusterId);
        type.removeClusterId(previousClusterId);
        db.dropCluster(previousClusterId, false);

        previousClusterId = newClusterId;
      }
    }

    log(" * dump clusters");
    try (ODatabaseDocumentTx db = openDatabase()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.getClass("Message");
      for (int clusterId : type.getClusterIds()) {
        String clusterName = db.getClusterNameById(clusterId);
        log("Cluster; id: {}, name: {}", clusterId, clusterName);
        for (ODocument doc : db.browseCluster(clusterName)) {
          log("Document: {}", doc);
        }
      }
    }

    log(" * dump records");
    try (ODatabaseDocumentTx db = openDatabase()) {
      for (ODocument doc : db.browseClass("Message")) {
        log("Document: {}", doc);
      }
    }
  }

  @Test
  public void clusterReUse() throws Exception {
    log(" * Create database");
    try (ODatabaseDocumentTx db = createDatabase()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.createClass("Message");
      type.setClusterSelection(ODefaultClusterSelectionStrategy.NAME);
      type.createProperty("text", OType.STRING);
    }

    for (int c = 0; c < 4; c++) {
      log(" * Create new cluster");
      try (ODatabaseDocumentTx db = openDatabase()) {
        OSchema schema = db.getMetadata().getSchema();
        OClass type = schema.getClass("Message");

        // create new cluster
        String newClusterName = String.format("Message_%s", System.currentTimeMillis());
        type.addCluster(newClusterName);
        int newClusterId = db.getClusterIdByName(newClusterName);
        type.setDefaultClusterId(newClusterId);
        log("New cluster; id: {}, name: {}", newClusterId, newClusterName);
      }

      log(" * create records");
      for (int i = 0; i < 4; i++) {
        try (ODatabaseDocumentTx db = openDatabase()) {
          db.newInstance("Message")
              .field("text", String.format("Hi %s", System.currentTimeMillis()))
              .save();
        }
      }
    }

    log(" * dump clusters");
    try (ODatabaseDocumentTx db = openDatabase()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.getClass("Message");
      for (int clusterId : type.getClusterIds()) {
        String clusterName = db.getClusterNameById(clusterId);
        log("Cluster; id: {}, name: {}", clusterId, clusterName);
        for (ODocument doc : db.browseCluster(clusterName)) {
          log("Document: {}", doc);
        }
      }
    }

    log(" * dump records");
    try (ODatabaseDocumentTx db = openDatabase()) {
      for (ODocument doc : db.browseClass("Message")) {
        log("Document: {}", doc);
      }
    }
  }
}
