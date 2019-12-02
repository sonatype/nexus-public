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
package org.sonatype.nexus.repository.upgrade

import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat
import static org.mockito.Mockito.when
import static org.sonatype.nexus.repository.upgrade.ConfigDatabaseUpgrade_1_4.C_HEALTHCHECKCONFIG
import static org.sonatype.nexus.repository.upgrade.ConfigDatabaseUpgrade_1_4.PROPERTY_QUERY
import static org.sonatype.nexus.repository.upgrade.ConfigDatabaseUpgrade_1_4.P_PROPERTY_VALUE

/**
 * Unit tests for {@link ConfigDatabaseUpgrade_1_4}.
 */
@RunWith(MockitoJUnitRunner.class)
class ConfigDatabaseUpgrade_1_4Test
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('test')

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder()

  @Mock
  ApplicationDirectories applicationDirectories

  private ConfigDatabaseUpgrade_1_4 upgrade

  private File etc

  @Before
  void setup() {
    etc = temporaryFolder.newFolder()
    when(applicationDirectories.getWorkDirectory('etc')).thenReturn(etc)

    upgrade = new ConfigDatabaseUpgrade_1_4(database.instanceProvider, applicationDirectories)
  }

  @After
  void teardown() {
    // empty out the folder in between test methods
    etc.delete()
  }

  @Test
  void 'healthcheck properties file does not exist'() {
    upgrade.apply()

    assertThat(countRows(), equalTo(0L))
  }

  @Test
  void 'healthcheck properties properly imported'() {
    Properties properties = new Properties()
    properties.setProperty('maven-central.enabled', 'true')
    properties.setProperty('scan.rate', '47')

    saveProperties(properties)

    upgrade.apply()

    assertThat(countRows(), equalTo(2L))
    assertThat(getPropertyValue('maven-central.enabled'), equalTo('true'))
    assertThat(getPropertyValue('scan.rate'), equalTo('47'))
  }

  @Test
  void 'healthcheck properties properly imported multiple times'() {
    Properties properties = new Properties()
    properties.setProperty('maven-central.enabled', 'true')
    properties.setProperty('scan.rate', '47')

    saveProperties(properties)

    upgrade.apply()

    assertThat(countRows(), equalTo(2L))
    assertThat(getPropertyValue('maven-central.enabled'), equalTo('true'))
    assertThat(getPropertyValue('scan.rate'), equalTo('47'))

    properties.setProperty('scan.rate', '42')
    saveProperties(properties)

    upgrade.apply()

    assertThat(countRows(), equalTo(2L))
    assertThat(getPropertyValue('maven-central.enabled'), equalTo('true'))
    assertThat(getPropertyValue('scan.rate'), equalTo('42'))
  }

  @Test
  void 'healthcheck properties already contains migrated property'() {
    Properties properties = new Properties()
    properties.setProperty(ConfigDatabaseUpgrade_1_4.MIGRATED, 'true')
    properties.setProperty('maven-central.enabled', 'true')
    properties.setProperty('scan.rate', '47')

    saveProperties(properties)
    upgrade.apply()

    assertThat(countRows(), equalTo(0L))
  }

  @Test
  void 'healthcheck properties contains entries that differ only by case'() {
    Properties properties = new Properties()
    properties.setProperty('maven-central.enabled', 'true')
    properties.setProperty('Maven-Central.enabled', 'true')

    saveProperties(properties)

    upgrade.apply()

    assertThat(countRows(), equalTo(1L))
  }

  def saveProperties(Properties properties) {
    File file = new File(etc, 'healthcheck.properties')
    properties.store(new FileWriter(file), '')
  }

  long countRows() {
    database.instanceProvider.get().connect().withCloseable { db ->
      return db.countClass(C_HEALTHCHECKCONFIG)
    }
  }

  String getPropertyValue(final String propertyName) {
    database.instanceProvider.get().connect().withCloseable { db ->
      List<ODocument> results = db.command(PROPERTY_QUERY).execute(propertyName)
      return results ? results[0].field(P_PROPERTY_VALUE, OType.STRING) : null
    }
  }
}
