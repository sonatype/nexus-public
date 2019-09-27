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
package org.sonatype.nexus.quartz.orient;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE.UNIQUE;
import static com.orientechnologies.orient.core.metadata.schema.OType.BINARY;
import static com.orientechnologies.orient.core.metadata.schema.OType.BOOLEAN;
import static com.orientechnologies.orient.core.metadata.schema.OType.DECIMAL;
import static com.orientechnologies.orient.core.metadata.schema.OType.INTEGER;
import static com.orientechnologies.orient.core.metadata.schema.OType.LONG;
import static com.orientechnologies.orient.core.metadata.schema.OType.SHORT;
import static com.orientechnologies.orient.core.metadata.schema.OType.STRING;

/**
 * Populate an OrientDB database with the schema required for use as a Quartz JDBC job store.
 *
 * @since 3.19
 */
public class OrientQuartzSchema {

  private static final String SCHED_NAME = "SCHED_NAME";

  private static final String CALENDAR_NAME = "CALENDAR_NAME";

  private static final String TRIGGER_NAME = "TRIGGER_NAME";

  private static final String TRIGGER_GROUP = "TRIGGER_GROUP";

  private static final String INSTANCE_NAME = "INSTANCE_NAME";

  private static final String JOB_NAME = "JOB_NAME";

  private static final String JOB_GROUP = "JOB_GROUP";

  private OrientQuartzSchema() {
    // static class
  }

  /**
   * Create the classes necessary for the Quartz JDBC job store.
   *
   * @since 3.19
   */
  public static void register(final ODatabaseDocumentTx db) {
    checkNotNull(db);

    OSchema schema = db.getMetadata().getSchema();

    calendars(schema);

    cronTriggers(schema);

    firedTriggers(schema);

    pausedTriggerGrps(schema);

    schedulerState(schema);

    locks(schema);

    jobDetails(schema);

    simpleTriggers(schema);

    simpropTriggers(schema);

    blobTriggers(schema);

    triggers(schema);
  }

  private static void calendars(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_CALENDARS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, CALENDAR_NAME, STRING, true);
    maybeCreateProperty(type, "CALENDAR", BINARY, true);
    maybeCreateIndex(type, "PK_QRTZ_CALENDARS", SCHED_NAME, CALENDAR_NAME);
  }

  private static void cronTriggers(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_CRON_TRIGGERS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_GROUP, STRING, true);
    maybeCreateProperty(type, "CRON_EXPRESSION", STRING, true);
    maybeCreateProperty(type, "TIME_ZONE_ID", STRING, false);
    maybeCreateIndex(type, "PK_QRTZ_CRON_TRIGGERS", SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
  }

  private static void firedTriggers(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_FIRED_TRIGGERS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, "ENTRY_ID", STRING, true);
    maybeCreateProperty(type, TRIGGER_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_GROUP, STRING, true);
    maybeCreateProperty(type, INSTANCE_NAME, STRING, true);
    maybeCreateProperty(type, "FIRED_TIME", LONG, true);
    maybeCreateProperty(type, "SCHED_TIME", LONG, true);
    maybeCreateProperty(type, "PRIORITY", INTEGER, true);
    maybeCreateProperty(type, "STATE", STRING, true);
    maybeCreateProperty(type, JOB_NAME, STRING, false);
    maybeCreateProperty(type, JOB_GROUP, STRING, false);
    maybeCreateProperty(type, "IS_NONCONCURRENT", BOOLEAN, false);
    maybeCreateProperty(type, "REQUESTS_RECOVERY", BOOLEAN, false);
    maybeCreateIndex(type, "PK_QRTZ_FIRED_TRIGGERS", SCHED_NAME, "ENTRY_ID");
  }

  private static void pausedTriggerGrps(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_PAUSED_TRIGGER_GRPS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_GROUP, STRING, true);
    maybeCreateIndex(type, "PK_QRTZ_PAUSED_TRIGGER_GRPS", SCHED_NAME, TRIGGER_GROUP);
  }

  private static void schedulerState(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_SCHEDULER_STATE");
    maybeCreateProperty(type, INSTANCE_NAME, STRING, true);
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, "LAST_CHECKIN_TIME", LONG, true);
    maybeCreateProperty(type, "CHECKIN_INTERVAL", LONG, true);
    maybeCreateIndex(type, "PK_QRTZ_SCHEDULER_STATE", SCHED_NAME, INSTANCE_NAME);
  }

  private static void locks(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_LOCKS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, "LOCK_NAME", STRING, true);
    maybeCreateIndex(type, "PK_QRTZ_LOCKS", SCHED_NAME, "LOCK_NAME");
  }

  private static void jobDetails(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_JOB_DETAILS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, JOB_NAME, STRING, true);
    maybeCreateProperty(type, JOB_GROUP, STRING, true);
    maybeCreateProperty(type, "DESCRIPTION", STRING, false);
    maybeCreateProperty(type, "JOB_CLASS_NAME", STRING, true);
    maybeCreateProperty(type, "IS_DURABLE", BOOLEAN, true);
    maybeCreateProperty(type, "IS_NONCONCURRENT", BOOLEAN, true);
    maybeCreateProperty(type, "IS_UPDATE_DATA", BOOLEAN, true);
    maybeCreateProperty(type, "REQUESTS_RECOVERY", BOOLEAN, true);
    maybeCreateProperty(type, "JOB_DATA", BINARY, false);
    maybeCreateIndex(type, "PK_QRTZ_JOB_DETAILS", SCHED_NAME, JOB_NAME, JOB_GROUP);
  }

  private static void simpleTriggers(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_SIMPLE_TRIGGERS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_GROUP, STRING, true);
    maybeCreateProperty(type, "REPEAT_COUNT", LONG, true);
    maybeCreateProperty(type, "REPEAT_INTERVAL", LONG, true);
    maybeCreateProperty(type, "TIMES_TRIGGERED", LONG, true);
    maybeCreateIndex(type, "PK_QRTZ_SIMPLE_TRIGGERS", SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
  }

  private static void simpropTriggers(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_SIMPROP_TRIGGERS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_GROUP, STRING, true);
    maybeCreateProperty(type, "STR_PROP_1", STRING, false);
    maybeCreateProperty(type, "STR_PROP_2", STRING, false);
    maybeCreateProperty(type, "STR_PROP_3", STRING, false);
    maybeCreateProperty(type, "INT_PROP_1", INTEGER, false);
    maybeCreateProperty(type, "INT_PROP_2", INTEGER, false);
    maybeCreateProperty(type, "LONG_PROP_1", LONG, false);
    maybeCreateProperty(type, "LONG_PROP_2", LONG, false);
    maybeCreateProperty(type, "DEC_PROP_1", DECIMAL, false);
    maybeCreateProperty(type, "DEC_PROP_2", DECIMAL, false);
    maybeCreateProperty(type, "BOOL_PROP_1", BOOLEAN, false);
    maybeCreateProperty(type, "BOOL_PROP_2", BOOLEAN, false);
    maybeCreateIndex(type, "PK_QRTZ_SIMPROP_TRIGGERS", SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
  }

  private static void blobTriggers(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_BLOB_TRIGGERS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_GROUP, STRING, true);
    maybeCreateProperty(type, "BLOB_DATA", BINARY, false);
    maybeCreateIndex(type, "PK_QRTZ_BLOB_TRIGGERS", SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
  }

  private static void triggers(final OSchema schema) {
    OClass type = maybeCreateClass(schema, "QRTZ_TRIGGERS");
    maybeCreateProperty(type, SCHED_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_NAME, STRING, true);
    maybeCreateProperty(type, TRIGGER_GROUP, STRING, true);
    maybeCreateProperty(type, JOB_NAME, STRING, true);
    maybeCreateProperty(type, JOB_GROUP, STRING, true);
    maybeCreateProperty(type, "DESCRIPTION", STRING, false);
    maybeCreateProperty(type, "NEXT_FIRE_TIME", LONG, false);
    maybeCreateProperty(type, "PREV_FIRE_TIME", LONG, false);
    maybeCreateProperty(type, "PRIORITY", INTEGER, false);
    maybeCreateProperty(type, "TRIGGER_STATE", STRING, true);
    maybeCreateProperty(type, "TRIGGER_TYPE", STRING, true);
    maybeCreateProperty(type, "START_TIME", LONG, true);
    maybeCreateProperty(type, "END_TIME", LONG, false);
    maybeCreateProperty(type, CALENDAR_NAME, STRING, false);
    maybeCreateProperty(type, "MISFIRE_INSTR", SHORT, false);
    maybeCreateProperty(type, "JOB_DATA", BINARY, false);
    maybeCreateIndex(type, "PK_QRTZ_TRIGGERS", SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
  }

  private static OClass maybeCreateClass(final OSchema schema, final String className) {
    OClass clazz = schema.getClass(className);
    if (clazz == null) {
      clazz = schema.createClass(className);
    }
    return clazz;
  }

  private static OProperty maybeCreateProperty(final OClass clazz,
                                               final String name,
                                               final OType type,
                                               final boolean notNull)
  {
    OProperty property = clazz.getProperty(name);
    if (property == null) {
      property = clazz.createProperty(name, type);
      if (notNull) {
        property = property.setMandatory(true).setNotNull(true);
      }
    }
    return property;
  }

  private static OIndex<?> maybeCreateIndex(final OClass clazz, final String name, final String... props) {
    OIndex<?> index = clazz.getClassIndex(name);
    if (index == null) {
      index = clazz.createIndex(name, UNIQUE, props);
    }
    return index;
  }
}
