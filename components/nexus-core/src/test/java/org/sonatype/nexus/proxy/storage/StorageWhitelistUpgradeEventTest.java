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
package org.sonatype.nexus.proxy.storage;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Properties;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mock;

import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.Files.readAllLines;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.fest.util.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.proxy.storage.StorageWhitelist.PROP_NAME;

/**
 * @since 2.14.15
 */
public class StorageWhitelistUpgradeEventTest
    extends TestSupport
{
  @Rule
  public final TestName testName = new TestName();

  @Mock
  private ApplicationStatusSource applicationStatusSource;

  @Mock
  private SystemStatus systemStatus;

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private File installDir;

  @Mock
  private RepositoryRegistry repositoryRegistry;

  @Mock
  private AbstractRepository repository;

  @Mock
  private CRepositoryCoreConfiguration coreConfig;

  @Mock
  private CRepository repositoryConfig;

  @Mock
  private CLocalStorage localStorage;

  @Mock
  private StorageWhitelist storageWhitelist;

  @Mock
  private NexusStartedEvent nexusStartedEvent;

  private Path testTempDir;

  private Path nexusPropPath;

  private StorageWhitelistUpgradeEvent underTest;

  @Before
  public void setup() throws Exception {
    underTest = new StorageWhitelistUpgradeEvent(applicationStatusSource, applicationDirectories, repositoryRegistry,
        storageWhitelist);

    when(applicationStatusSource.getSystemStatus()).thenReturn(systemStatus);
    when(applicationDirectories.getInstallDirectory()).thenReturn(installDir);
    when(repositoryRegistry.getRepositories()).thenReturn(newArrayList(repository));
    when(repository.getCurrentCoreConfiguration()).thenReturn(coreConfig);
    when(coreConfig.getConfiguration(anyBoolean())).thenReturn(repositoryConfig);
    when(repositoryConfig.getLocalStorage()).thenReturn(localStorage);

    testTempDir = createTempDirectory(testName.getMethodName());
    createDirectory(testTempDir.resolve("conf"));
    nexusPropPath = testTempDir.resolve("conf/nexus.properties");
    when(installDir.getAbsolutePath()).thenReturn(testTempDir.toAbsolutePath().toString());
  }

  @After
  public void cleanup() throws Exception {
    deleteDirectory(testTempDir.toFile());
  }

  @Test
  public void doesNothingOnNonUpgradeStartup() {
    when(systemStatus.isInstanceUpgraded()).thenReturn(false);

    underTest.inspect(nexusStartedEvent);

    verifyZeroInteractions(storageWhitelist, applicationDirectories, repositoryRegistry);
  }

  @Test
  public void createsWhitelistIfNotPresentOnUpgrade() throws Exception {
    String storageUrl = "/foo/bar";
    when(systemStatus.isInstanceUpgraded()).thenReturn(true);
    when(localStorage.getUrl()).thenReturn(storageUrl);

    underTest.inspect(nexusStartedEvent);

    validateProps(1, storageUrl);
  }

  @Test
  public void addsMultipleLocations() throws Exception {
    when(repositoryRegistry.getRepositories()).thenReturn(newArrayList(repository, repository, repository));
    when(localStorage.getUrl()).thenReturn("/foo", "/bar", "/baz");
    when(systemStatus.isInstanceUpgraded()).thenReturn(true);

    underTest.inspect(nexusStartedEvent);

    validateProps(3, "/foo,/bar,/baz");
  }

  @Test
  public void doesNotOverridePropertyIfExists() throws Exception {
    Properties props = new Properties();
    props.setProperty(PROP_NAME, "/foo");
    try (OutputStream os = newOutputStream(nexusPropPath, CREATE_NEW)) {
      props.store(os, EMPTY);
    }

    when(systemStatus.isInstanceUpgraded()).thenReturn(true);
    when(localStorage.getUrl()).thenReturn("/bar");

    underTest.inspect(nexusStartedEvent);

    verifyZeroInteractions(storageWhitelist, repositoryRegistry);

    validateProps(0, "/foo");
  }

  @Test
  public void doesNotIncludeBlankLocations() throws Exception {
    when(repositoryRegistry.getRepositories()).thenReturn(newArrayList(repository, repository, repository));
    when(localStorage.getUrl()).thenReturn(EMPTY, null, " ");
    when(systemStatus.isInstanceUpgraded()).thenReturn(true);

    underTest.inspect(nexusStartedEvent);

    validateProps(0, EMPTY);
  }

  @Test
  public void preservesExistingPropertyFileFormat() throws Exception {
    Properties props = new Properties();
    props.setProperty("foo", "bar");
    try (OutputStream os = newOutputStream(nexusPropPath, CREATE_NEW)) {
      props.store(os, "pre-existing-comment");
    }

    when(systemStatus.isInstanceUpgraded()).thenReturn(true);
    when(localStorage.getUrl()).thenReturn("/baz");

    underTest.inspect(nexusStartedEvent);

    validateProps(1, "/baz");

    props.load(newInputStream(nexusPropPath, READ));
    assertThat("Existing properties should still be present", props.get("foo"), is("bar"));
    assertThat("Existing comments should still be present", readAllLines(nexusPropPath),
        hasItem("#pre-existing-comment"));
  }

  private void validateProps(final int numWhitelistAdd, final String expectedValue) throws Exception {
    verify(storageWhitelist, times(numWhitelistAdd)).addWhitelistPath(anyString());
    assertThat("nexus.properties file should have been created", exists(nexusPropPath), is(true));

    Properties props = new Properties();
    try (InputStream is = newInputStream(nexusPropPath, READ)) {
      props.load(is);
    }

    assertThat("Whitelist property should have been written to nexus.properties", props.containsKey(PROP_NAME));
    assertThat("Whitelist property did not have the expected value", props.getProperty(PROP_NAME), is(expectedValue));
  }
}
