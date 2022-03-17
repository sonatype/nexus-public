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
package org.sonatype.nexus.yum.internal.support;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.configuration.application.DefaultGlobalRestApiSettings;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsHiddenAttribute;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.yum.internal.task.CommandLineExecutor;
import org.sonatype.sisu.goodies.testsupport.TestTracer;
import org.sonatype.sisu.goodies.testsupport.TestUtil;
import org.sonatype.sisu.goodies.testsupport.junit.TestDataRule;
import org.sonatype.sisu.goodies.testsupport.junit.TestIndexRule;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.Timeout;
import com.google.common.collect.ObjectArrays;
import com.google.inject.Binder;
import com.google.inject.Module;
import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.component.annotations.Requirement;
import org.freecompany.redline.Builder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.freecompany.redline.header.Architecture.NOARCH;
import static org.freecompany.redline.header.Os.LINUX;
import static org.freecompany.redline.header.RpmType.BINARY;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YumNexusTestSupport
    extends NexusAppTestSupport
{

  private String javaTmpDir;

  public static final String TMP_DIR_KEY = "java.io.tmpdir";

  protected final TestUtil util = new TestUtil(this);

  @Rule
  public final TestTracer tracer = new TestTracer(this);

  @Rule
  public TestIndexRule testIndex = new TestIndexRule(
      util.resolveFile("target/ut-reports"), util.resolveFile("target/ut-data")
  );

  @Rule
  public TestDataRule testData = new TestDataRule(util.resolveFile("src/test/ut-resources"));

  @Rule
  public final TestName testName = new TestName();

  @Inject
  private DefaultGlobalRestApiSettings globalRestApiSettings;

  @Inject
  private ApplicationDirectories applicationDirectories;

  protected File rpmsDir() {
    return testData.resolveFile("rpms");
  }

  protected File cacheDir() {
    return testIndex.getDirectory("cache");
  }

  protected File repositoryDir(final String repositoryId) {
    return testIndex.getDirectory("repository/" + repositoryId);
  }

  protected File randomDir() {
    return testIndex.getDirectory(RandomStringUtils.randomAlphabetic(20));
  }

  @Before
  public void setBaseUrl()
      throws ConfigurationException
  {
    globalRestApiSettings.setBaseUrl("http://localhost:8080/nexus");
    globalRestApiSettings.commitChanges();
  }

  @After
  public void resetJavaTmpDir() {
    System.setProperty(TMP_DIR_KEY, javaTmpDir);
  }

  @After
  public void recordSurefireAndFailsafeInfo() {
    {
      final String name = "target/surefire-reports/" + getClass().getName();
      testIndex.recordLink("surefire result", util.resolveFile(name + ".txt"));
      testIndex.recordLink("surefire output", util.resolveFile(name + "-output.txt"));
    }
  }

  protected void waitFor(Condition condition)
      throws TimeoutException, InterruptedException
  {
    waitOrTimeout(condition, Timeout.timeout(seconds(60)), new ThreadSleep(millis(30)));
  }

  @Override
  protected void customizeContainerConfiguration(final ContainerConfiguration configuration) {
    super.customizeContainerConfiguration(configuration);
    configuration.setClassPathScanning(PlexusConstants.SCANNING_ON);
  }

  @Override
  protected Module[] getTestCustomModules() {
    Module[] modules = super.getTestCustomModules();
    if (modules == null) {
      modules = new Module[0];
    }
    modules = ObjectArrays.concat(modules, new Module()
    {
      @Override
      public void configure(final Binder binder) {
        binder.bind(CommandLineExecutor.class)
            .toInstance(new CommandLineExecutor(applicationDirectories)
        {
          @Override
          public int exec(final String command, final String params) throws IOException {
            // do nothing
            return 0;
          }
        });
      }
    });
    return modules;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    javaTmpDir = System.getProperty(TMP_DIR_KEY);
    System.setProperty(TMP_DIR_KEY, cacheDir().getAbsolutePath());
    super.setUp();
    lookup(NexusConfiguration.class).loadConfiguration(true);
    injectFields();
  }

  private void injectFields()
      throws Exception
  {
    for (Field field : getAllFields()) {
      if (field.getAnnotation(Inject.class) != null) {
        lookupField(field, "");
        continue;
      }

      Requirement requirement = field.getAnnotation(Requirement.class);
      if (requirement != null) {
        lookupField(field, requirement.hint());
      }
    }
  }

  private void lookupField(Field field, String hint)
      throws Exception
  {
    Object value = lookup(field.getType(), hint);
    if (!field.isAccessible()) {
      field.setAccessible(true);
      field.set(this, value);
      field.setAccessible(false);
    }
  }

  private List<Field> getAllFields() {
    List<Field> fields = new ArrayList<Field>();
    Class<?> clazz = getClass();
    do {
      List<? extends Field> classFields = getFields(clazz);
      fields.addAll(classFields);
      clazz = clazz.getSuperclass();
    }
    while (!Object.class.equals(clazz));
    return fields;
  }

  private List<? extends Field> getFields(Class<?> clazz) {
    return asList(clazz.getDeclaredFields());
  }

  public static File createDummyRpm(String name, String version, File outputDirectory)
      throws NoSuchAlgorithmException, IOException
  {
    Builder rpmBuilder = new Builder();
    rpmBuilder.setVendor("IS24");
    rpmBuilder.setGroup("is24");
    rpmBuilder.setPackager("maven - " + System.getProperty("user.name"));
    try {
      rpmBuilder.setBuildHost(InetAddress.getLocalHost().getHostName());
    }
    catch (UnknownHostException e) {
      throw new RuntimeException("Could not determine hostname.", e);
    }
    rpmBuilder.setPackage(name, version, "1");
    rpmBuilder.setPlatform(NOARCH, LINUX);
    rpmBuilder.setType(BINARY);
    rpmBuilder.setSourceRpm("dummy-source-rpm-because-yum-needs-this");

    outputDirectory.mkdirs();

    String filename = rpmBuilder.build(outputDirectory);
    return new File(outputDirectory, filename);
  }

  public File copyToTempDir(File srcDir)
      throws IOException
  {
    final File destDir = randomDir();
    copyDirectory(srcDir, destDir);
    return destDir;
  }

  public MavenRepository createRepository(final boolean isMavenHostedRepository) {
    return createRepository(isMavenHostedRepository, testName.getMethodName());
  }

  public MavenRepository createRepository(final boolean isMavenHostedRepository,
                                          final String repositoryId)
  {
    final RepositoryKind kind = mock(RepositoryKind.class);
    when(kind.isFacetAvailable(HostedRepository.class)).thenReturn(true);
    when(kind.isFacetAvailable(MavenHostedRepository.class)).thenReturn(isMavenHostedRepository);

    final MavenHostedRepository repository = mock(MavenHostedRepository.class);
    when(repository.getRepositoryKind()).thenReturn(kind);
    when(repository.getId()).thenReturn(repositoryId);
    when(repository.getProviderRole()).thenReturn(Repository.class.getName());
    when(repository.getProviderHint()).thenReturn("maven2");
    final RepositoryItemUid uid = mock(RepositoryItemUid.class);
    when(uid.getLock()).thenReturn(mock(RepositoryItemUidLock.class));
    when(repository.createUid(anyString())).thenReturn(uid);

    if (isMavenHostedRepository) {
      when(repository.adaptToFacet(HostedRepository.class)).thenReturn(repository);
      when(repository.adaptToFacet(MavenRepository.class)).thenReturn(repository);
    }
    else {
      when(repository.adaptToFacet(HostedRepository.class)).thenThrow(new ClassCastException());
    }

    when(repository.getLocalUrl()).thenReturn(repositoryDir(repositoryId).toURI().toString());

    return repository;
  }

  public static StorageItem createItem(final String version, final String filename) {
    final StorageItem item = mock(StorageItem.class);
    final RepositoryItemUid uid = mock(RepositoryItemUid.class);

    when(item.getPath()).thenReturn("foo/" + version + "/" + filename);
    when(item.getParentPath()).thenReturn("foo/" + version);
    when(item.getItemContext()).thenReturn(new RequestContext());
    when(item.getRepositoryItemUid()).thenReturn(uid);
    when(uid.getBooleanAttributeValue(IsHiddenAttribute.class)).thenReturn(true);

    return item;
  }

  // for Windows builds, since win file paths aren't valid URLs
  protected String osIndependentUri(File file) throws IOException {
    return file.getCanonicalFile().toURI().toString();
  }

}
