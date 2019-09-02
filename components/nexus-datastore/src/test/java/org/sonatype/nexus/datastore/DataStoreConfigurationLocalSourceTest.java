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
package org.sonatype.nexus.datastore;

import java.io.File;
import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static java.nio.file.Files.readAllLines;
import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.DataStoreConfigurationLocalSource.LOCAL;

/**
 * {@link DataStoreConfigurationLocalSource} tests.
 */
public class DataStoreConfigurationLocalSourceTest
    extends TestSupport
{
  @Rule
  public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private ApplicationDirectories directories;

  private DataStoreConfigurationSource underTest;

  @Before
  public void setUp() {
    when(directories.getWorkDirectory("etc/fabric")).thenReturn(temporaryFolder.getRoot());
    underTest = new DataStoreConfigurationLocalSource(directories);
  }

  private DataStoreConfiguration newDataStoreConfiguration(final String name) {
    DataStoreConfiguration config = new DataStoreConfiguration();
    config.setName(name);
    config.setType("testType");
    config.setSource(LOCAL);
    config.setAttributes(ImmutableMap.of("jdbc.port", "8888", "jdbc.token", "#12345678"));
    return config;
  }

  @Test
  public void includesDefaultStores() {
    assertThat(underTest.browseStoreNames(), contains("config", "content"));
  }

  @Test
  public void includesStoresListedInFabricWorkFolder() throws IOException {
    temporaryFolder.newFile("test-store.properties");
    temporaryFolder.newFile("-test-store.properties");
    temporaryFolder.newFile(".badname-store.properties");
    temporaryFolder.newFile("_badname-store.properties");
    temporaryFolder.newFile("!badname-store.properties");

    assertThat(underTest.browseStoreNames(), containsInAnyOrder("config", "content", "-test", "test"));
  }

  @Test
  public void defaultStoresHaveSaneDefaults() {
    DataStoreConfiguration config = underTest.load("config");

    assertThat(config.getName(), is("config"));
    assertThat(config.getSource(), is(LOCAL));
    assertThat(config.getType(), is("jdbc"));
    assertThat(config.getAttributes().keySet(), hasSize(1));
    assertThat(config.getAttributes(), hasEntry("jdbcUrl", "jdbc:h2:file:${karaf.data}/db/${storeName}"));
  }

  @Test
  public void customStoresHaveSaneDefaults() {
    DataStoreConfiguration config = underTest.load("exampleStore");

    assertThat(config.getName(), is("exampleStore"));
    assertThat(config.getSource(), is(LOCAL));
    assertThat(config.getType(), is("jdbc"));
    assertThat(config.getAttributes().keySet(), hasSize(1));
    assertThat(config.getAttributes(), hasEntry("jdbcUrl", "jdbc:h2:file:${karaf.data}/db/${storeName}"));
  }

  @Test
  public void defaultStoresCanBeConfiguredViaSystemProperties() {
    System.setProperty("nexus.datastore.config.type", "customProps");
    System.setProperty("nexus.datastore.config.user", "dba");
    System.setProperty("nexus.datastore.config.user.token", "secret");

    DataStoreConfiguration config = underTest.load("config");

    assertThat(config.getName(), is("config"));
    assertThat(config.getSource(), is(LOCAL));
    assertThat(config.getType(), is("customProps"));
    assertThat(config.getAttributes().keySet(), hasSize(2));
    assertThat(config.getAttributes(), allOf(hasEntry("user", "dba"), hasEntry("user.token", "secret")));
  }

  @Test
  public void defaultStoresCanBeConfiguredViaFabricWorkFolder() throws IOException {
    File configStorePropertiesFile = temporaryFolder.newFile("config-store.properties");
    write(configStorePropertiesFile.toPath(),
        ImmutableList.of("name=config", "type=customFile", "user=admin", "user.pass=admin!"));

    DataStoreConfiguration config = underTest.load("config");

    assertThat(config.getName(), is("config"));
    assertThat(config.getSource(), is(LOCAL));
    assertThat(config.getType(), is("customFile"));
    assertThat(config.getAttributes().keySet(), hasSize(2));
    assertThat(config.getAttributes(), hasEntry("user", "admin"));
    assertThat(config.getAttributes(), hasEntry("user.pass", "admin!"));
  }

  @Test
  public void cannotLoadStoresWithInvalidNames() throws IOException {
    try {
      underTest.load(".badname");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString(".badname is not a valid data store name"));
    }
  }

  @Test
  public void cannotLoadStoresWithMismatchedNames() throws IOException {
    File customStorePropertiesFile = temporaryFolder.newFile("custom-store.properties");
    write(customStorePropertiesFile.toPath(), ImmutableList.of("name=example"));

    try {
      underTest.load("custom");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("is for a different data store"));
    }
  }

  @Test
  public void canCreateConfigurations() {
    File testStorePropertiesFile = new File(temporaryFolder.getRoot(), "config-store.properties");

    assertThat(testStorePropertiesFile.exists(), is(false));
    underTest.save(newDataStoreConfiguration("config"));
    assertThat(testStorePropertiesFile.exists(), is(true));
  }

  @Test
  public void canUpdateConfigurations() throws IOException {
    File testStorePropertiesFile = temporaryFolder.newFile("config-store.properties");
    write(testStorePropertiesFile.toPath(), ImmutableList.of("name=config"));

    assertThat(testStorePropertiesFile.exists(), is(true));
    underTest.save(newDataStoreConfiguration("config"));
    assertThat(testStorePropertiesFile.exists(), is(true));

    assertThat(readAllLines(testStorePropertiesFile.toPath()),
        hasItems("name=config", "type=testType", "jdbc.port=8888", "jdbc.token=\\#12345678"));
  }

  @Test
  public void canDeleteConfigurations() throws IOException {
    File testStorePropertiesFile = temporaryFolder.newFile("config-store.properties");
    write(testStorePropertiesFile.toPath(), ImmutableList.of("name=config"));

    DataStoreConfiguration testConfig = underTest.load("config");
    assertThat(testStorePropertiesFile.exists(), is(true));
    underTest.delete(testConfig);
    assertThat(testStorePropertiesFile.exists(), is(false));
  }
}
