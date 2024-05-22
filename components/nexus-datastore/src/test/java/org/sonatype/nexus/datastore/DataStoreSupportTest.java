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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.common.stateguard.StateGuardModule;
import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.mockito.ArgumentCaptor;

import static com.google.inject.Guice.createInjector;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * {@link DataStoreSupport} tests.
 */
public class DataStoreSupportTest
    extends TestSupport
{
  @Rule
  public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  static class TestDataStore
      extends DataStoreSupport<DataSession<?>>
  {
    @Override
    public void register(final Class<? extends DataAccess> accessType) {
      // no-op
    }

    @Override
    public void unregister(final Class<? extends DataAccess> accessType) {
      // no-op
    }

    @Override
    public DataSession<?> openSession() {
      return mock(DataSession.class);
    }

    @Override
    public Connection openConnection() {
      return mock(Connection.class);
    }

    @Override
    public DataSource getDataSource() {
      return mock(DataSource.class);
    }

    @Override
    protected void doStart(final String storeName, final Map<String, String> attributes) throws Exception {
      // do nothing
    }

    @Override
    public void freeze() {
      // do nothing
    }

    @Override
    public void unfreeze() {
      // do nothing
    }

    @Override
    public boolean isFrozen() {
      return false;
    }

    @Override
    public void backup(final String location) throws SQLException {
      // do nothing
    }
  }

  private DataStoreSupport<?> underTest;

  @Before
  public void setUp() throws Exception {
    Injector injector = createInjector(new StateGuardModule());
    underTest = spy(injector.getInstance(TestDataStore.class));
  }

  private DataStoreConfiguration newDataStoreConfiguration(final String name) {
    DataStoreConfiguration config = new DataStoreConfiguration();
    config.setName(name);
    config.setType("testType");
    config.setSource("local");
    config.setAttributes(ImmutableMap.of("jdbcUrl", "jdbc:h2:${karaf.data}/db/${storeName}"));
    return config;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void attributesAreInterpolatedAtStart() throws Exception {
    System.setProperty("karaf.data", "/tmp/example");
    underTest.setConfiguration(newDataStoreConfiguration("testStore"));
    underTest.start();

    ArgumentCaptor<Map> captor = forClass(Map.class);
    verify(underTest).doStart(eq("testStore"), captor.capture());

    assertThat((Map<String, String>) captor.getValue(), hasEntry("jdbcUrl", "jdbc:h2:/tmp/example/db/testStore"));
    assertThat(underTest.getConfiguration().getAttributes(), hasEntry("jdbcUrl", "jdbc:h2:${karaf.data}/db/${storeName}"));
  }

  @Test
  public void shutdownStoreCannotBeRestarted() throws Exception {
    underTest.setConfiguration(newDataStoreConfiguration("testStore"));

    underTest.start();
    underTest.stop();

    underTest.start();
    underTest.shutdown();

    try {
      underTest.start();
      fail("Expected InvalidStateException");
    }
    catch (InvalidStateException e) {
      assertThat(e.getMessage(), containsString("Invalid state: SHUTDOWN"));
    }
  }

  @Test
  public void sensitiveAttributesAreRedacted() throws Exception {
    DataStoreConfiguration testConfig = new DataStoreConfiguration();

    testConfig.setName("testData");
    testConfig.setType("testType");
    testConfig.setSource("local");
    testConfig.setAttributes(new HashMap<>());
    testConfig.getAttributes().put("jdbcUrl", "jdbc:h2:${karaf.data}/db/test");
    testConfig.getAttributes().put("userCredentials", "a1");
    testConfig.getAttributes().put("userPasswd", "b2");
    testConfig.getAttributes().put("userTokenKey", "c3");
    underTest.setConfiguration(testConfig);

    assertThat(underTest.toString(), containsString("userCredentials=**REDACTED**"));
    assertThat(underTest.toString(), containsString("userPasswd=**REDACTED**"));
    assertThat(underTest.toString(), containsString("userTokenKey=**REDACTED**"));
  }
}
