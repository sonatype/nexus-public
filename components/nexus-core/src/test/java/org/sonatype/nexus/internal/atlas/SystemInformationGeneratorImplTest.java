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
package org.sonatype.nexus.internal.atlas;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.SystemInformationHelper;
import org.sonatype.nexus.common.node.DeploymentAccess;
import org.sonatype.nexus.common.node.NodeAccess;
import org.apache.commons.lang.SystemUtils;
import org.apache.karaf.bundle.core.BundleService;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SystemInformationGeneratorImpl}
 */
public class SystemInformationGeneratorImplTest extends TestSupport
{

  public static final Map<String, Boolean> UNAVAILABLE = SystemInformationGeneratorImpl.getUNAVAILABLE();

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private BundleContext bundleContext;

  @Mock
  private BundleService bundleService;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private DeploymentAccess deploymentAccess;

  @Mock
  private Map<String, SystemInformationHelper> systemInformationHelpers;

  /**
   * reportFileStores runs successfully using FileSystem.default
   */
  @Test
  public void reportFileStoresRunsSuccessfullyUsingFileSystemDefault() {
    SystemInformationGeneratorImpl generator = mockSystemInformationGenerator();

    Map<String, Object> data = new HashMap<>();
    for (FileStore fs : FileSystems.getDefault().getFileStores()) {
      data.put(fs.toString(), generator.reportFileStore(fs));
    }

    for (Map.Entry<String, Object> entry : data.entrySet()) {
      assertThat(entry.getKey(), not(isEmptyString()));
      assertThat(entry.getValue(), notNullValue());
    }
  }

  /**
   * reportFileStores handles IOException gracefully
   */
  @Test
  public void reportFileStoresHandlesIOExceptionGracefully() throws IOException {
    SystemInformationGeneratorImpl generator = mockSystemInformationGenerator();


    FileStore fs = Mockito.mock(FileStore.class);
    when(fs.toString()).thenReturn("description");
    when(fs.type()).thenReturn("brokenfstype");
    when(fs.name()).thenReturn("brokenfsname");
    when(fs.getTotalSpace()).thenThrow(new IOException("testing"));

    Map<String, Boolean> fsReport = (Map<String, Boolean>) generator.reportFileStore(fs);

    Assert.assertEquals(UNAVAILABLE, fsReport);
  }

  /**
   * reportNetwork runs successfully using NetworkInterface.networkInterfaces
   */
  @Test
  public void reportNetworkRunsSuccessfullyUsingNetworkInterfaceNetworkInterfaces() throws SocketException {
    SystemInformationGeneratorImpl generator = mockSystemInformationGenerator();

    List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
    Map<String, Object> data = new HashMap<>();
    for (NetworkInterface ni : networkInterfaces) {
      data.put(ni.getName(), generator.reportNetworkInterface(ni));
    }

    for (Map.Entry<String, Object> entry : data.entrySet()) {
      assertThat(entry.getKey(), not(isEmptyString()));
      assertThat(entry.getValue(), notNullValue());
    }
  }

  /**
   * reportNetwork handles SocketException gracefully
   */
  @Test
  public void reportNetworkHandlesSocketExceptionGracefully() throws SocketException {
    SystemInformationGeneratorImpl generator = mockSystemInformationGenerator();
    NetworkInterface intf = Mockito.mock(NetworkInterface.class);
    when(intf.getDisplayName()).thenReturn("brokenintf");
    when(intf.supportsMulticast()).thenThrow(new SocketException("testing"));

    Map<String, Boolean> report = (Map<String, Boolean>) generator.reportNetworkInterface(intf);

    Assert.assertEquals(UNAVAILABLE, report);
  }

  private SystemInformationGeneratorImpl mockSystemInformationGenerator() {
    return new SystemInformationGeneratorImpl(
        applicationDirectories,
        applicationVersion,
        Collections.emptyMap(),
        bundleContext,
        bundleService,
        nodeAccess,
        deploymentAccess,
        systemInformationHelpers
    );
  }

  /**
   * environment sensitive data is hidden
   */
  @Test
  public void environmentSensitiveDataIsHidden() throws IOException {
    SystemInformationGeneratorImpl generator = mockSystemInformationGenerator();
    // we need to exec some command to set up environment variables.
    String cmd = "mvn";
    if (SystemUtils.IS_OS_WINDOWS) {
      cmd = "mvn.cmd";
    }
    ProcessBuilder processBuilder = new ProcessBuilder(cmd, "version");
    processBuilder.environment().put("AZURE_CLIENT_SECRET", "azureSecretValue");
    processBuilder.environment().put("AZURE_TOKEN", "azureTokenValue");
    processBuilder.environment().put("MY_PASSWORD_FOR_NXRM", "admin123");
    processBuilder.start();

    Map<String, Object> report = generator.report();

    Map<String, String> systemEnvs = (Map<String, String>) report.get("system-environment");
    assertThat(systemEnvs.get("AZURE_CLIENT_SECRET"), not("azureSecretValue"));
    assertThat(systemEnvs.get("AZURE_TOKEN"), not("azureTokenValue"));
    assertThat(systemEnvs.get("MY_PASSWORD_FOR_NXRM"), not("admin123"));
  }

  /**
   * jvm variable sensitive data is hidden
   */
  @Test
  public void jvmVariableSensitiveDataIsHidden() {
    SystemInformationGeneratorImpl generator = new SystemInformationGeneratorImpl(
        applicationDirectories,
        applicationVersion,
        Collections.singletonMap("sun.java.command", "test.variable=1 -Dnexus.password=nxrm -Dnexus.token=123456"),
        bundleContext,
        bundleService,
        nodeAccess,
        deploymentAccess,
        systemInformationHelpers
    );

    Map<String, Object> report = generator.report();

    Map<String, Object> nexusProps = (Map<String, Object>) report.get("nexus-properties");
    assertThat(nexusProps.get("sun.java.command").toString(),
        equalTo("test.variable=1 -Dnexus.password=**** -Dnexus.token=****"));
  }

  /**
   * INSTALL4J_ADD_VM_PARAMS sensitive data is hidden
   */
  @Test
  public void install4jAddVmParamsSensitiveDataIsHidden() {
    SystemInformationGeneratorImpl generator = new SystemInformationGeneratorImpl(
        applicationDirectories,
        applicationVersion,
        Collections.singletonMap("INSTALL4J_ADD_VM_PARAMS", "test.variable=1 -Dnexus.password=nxrm -Dnexus.token=123456"),
        bundleContext,
        bundleService,
        nodeAccess,
        deploymentAccess,
        systemInformationHelpers
    );

    Map<String, Object> report = generator.report();

    Map<String, Object> nexusProps = (Map<String, Object>) report.get("nexus-properties");
    assertThat(nexusProps.get("INSTALL4J_ADD_VM_PARAMS").toString(),
        equalTo("test.variable=1 -Dnexus.password=**** -Dnexus.token=****"));
  }

}
