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

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.eclipse.sisu.Parameters;
import org.osgi.framework.BundleContext;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Iso8601Date;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.SystemInformationHelper;
import org.sonatype.nexus.common.atlas.SystemInformationGenerator;
import org.sonatype.nexus.common.node.DeploymentAccess;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.text.Strings2;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.text.Strings2.MASK;

/**
 * Default {@link SystemInformationGenerator}.
 *
 * @since 2.7
 */
@Named
@Singleton
public class SystemInformationGeneratorImpl
    extends ComponentSupport
    implements SystemInformationGenerator
{
  private final ApplicationDirectories applicationDirectories;

  private final ApplicationVersion applicationVersion;

  private final Map<String, String> parameters;

  private final BundleContext bundleContext;

  private final BundleService bundleService;

  private final NodeAccess nodeAccess;

  private final DeploymentAccess deploymentAccess;

  private final Map<String, SystemInformationHelper> systemInformationHelpers;

  static final Map<String, Object> UNAVAILABLE = ImmutableMap.of("unavailable", true);

  private static final List<String> SENSITIVE_FIELD_NAMES =
      ImmutableList.of("password", "secret", "token", "sign", "auth", "cred", "key", "pass");

  private static final List<String> SENSITIVE_CREDENTIALS_KEYS =
      ImmutableList.of("sun.java.command", "INSTALL4J_ADD_VM_PARAMS");

  @Inject
  public SystemInformationGeneratorImpl(
      ApplicationDirectories applicationDirectories,
      ApplicationVersion applicationVersion,
      @Parameters Map<String, String> parameters,
      BundleContext bundleContext,
      BundleService bundleService,
      NodeAccess nodeAccess,
      DeploymentAccess deploymentAccess,
      Map<String, SystemInformationHelper> systemInformationHelpers)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.applicationVersion = checkNotNull(applicationVersion);
    this.parameters = checkNotNull(parameters);
    this.bundleContext = checkNotNull(bundleContext);
    this.bundleService = checkNotNull(bundleService);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.deploymentAccess = checkNotNull(deploymentAccess);
    this.systemInformationHelpers = checkNotNull(systemInformationHelpers);
  }

  @Override
  public Map<String, Object> report() {
    log.info("Generating system information report");

    Map<String, Object> sections = ImmutableMap.<String, Object>builder()
        .put("system-time", reportTime())
        .put("system-properties", reportObfuscatedProperties(System.getProperties()))
        .put("system-environment", reportObfuscatedProperties(System.getenv()))
        .put("system-runtime", reportRuntime())
        .put("system-network", reportNetwork())
        .put("system-filestores", reportFileStores())
        .put("nexus-status", reportNexusStatus())
        .put("nexus-node", reportNexusNode())
        .put("nexus-properties", reportObfuscatedProperties(parameters))
        .put("nexus-configuration", reportNexusConfiguration())
        .put("nexus-bundles", reportNexusBundles())
        .putAll(systemInformationHelpers.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getValue())))
        .build();

    return sections;
  }

  private Map<String, Object> reportTime() {
    Date now = new Date();
    return ImmutableMap.of(
        "timezone", TimeZone.getDefault().getID(),
        "current", now.getTime(),
        "iso8601", Iso8601Date.format(now));
  }

  private Map<String, Object> reportRuntime() {
    Runtime runtime = Runtime.getRuntime();
    return ImmutableMap.of(
        "availableProcessors", runtime.availableProcessors(),
        "freeMemory", runtime.freeMemory(),
        "totalMemory", runtime.totalMemory(),
        "maxMemory", runtime.maxMemory(),
        "threads", Thread.activeCount());
  }

  private Map<String, Object> reportFileStores() {
    Map<String, Object> fileStores = new HashMap<>();
    int counter = 1;
    for (FileStore store : FileSystems.getDefault().getFileStores()) {
      String key = store.name();
      while (fileStores.containsKey(key)) {
        key = store.name() + "-" + counter++;
      }
      fileStores.put(key, reportFileStore(store));
    }
    return ImmutableMap.copyOf(fileStores);
  }

  private Map<String, Object> reportNetwork() {
    try {
      return Collections.list(NetworkInterface.getNetworkInterfaces())
          .stream()
          .collect(Collectors.toMap(NetworkInterface::getName, this::reportNetworkInterface));
    }
    catch (SocketException e) {
      log.error("Could not add report to support zip for network interfaces", e);
      return UNAVAILABLE;
    }
  }

  private Map<String, Object> reportNexusStatus() {
    return ImmutableMap.of(
        "version", applicationVersion.getVersion(),
        "edition", applicationVersion.getEdition(),
        "buildRevision", applicationVersion.getBuildRevision(),
        "buildTimestamp", applicationVersion.getBuildTimestamp());
  }

  private Map<String, Object> reportNexusNode() {
    return ImmutableMap.of(
        "node-id", nodeAccess.getId(),
        "deployment-id", deploymentAccess.getId());
  }

  private Map<String, Object> reportNexusConfiguration() {
    return ImmutableMap.of(
        "installDirectory", fileref(applicationDirectories.getInstallDirectory()),
        "workingDirectory", fileref(applicationDirectories.getWorkDirectory()),
        "temporaryDirectory", fileref(applicationDirectories.getTemporaryDirectory()));
  }

  private Map<String, Object> reportNexusBundles() {
    return Arrays.stream(bundleContext.getBundles())
        .collect(Collectors.toMap(
            bundle -> Long.toString(bundleService.getInfo(bundle).getBundleId()),
            bundle -> {
              BundleInfo info = bundleService.getInfo(bundle);
              // name is not set for groovy bundles
              String name = info.getName() == null ? "" : info.getName();
              return ImmutableMap.of(
                  "bundleId", info.getBundleId(),
                  "name", name,
                  "symbolicName", info.getSymbolicName(),
                  "location", info.getUpdateLocation(),
                  "version", info.getVersion(),
                  "state", info.getState().name(),
                  "startLevel", info.getStartLevel(),
                  "fragment", info.isFragment());
            }));
  }

  private String fileref(File file) {
    try {
      return file != null ? file.getCanonicalPath() : null;
    }
    catch (IOException e) {
      log.error("Could not get canonical path for file {}", file, e);
      return null;
    }
  }

  Map<String, Object> reportFileStore(FileStore store) {
    try {
      return ImmutableMap.of(
          "description", store.toString(),
          "type", store.type(),
          "totalSpace", store.getTotalSpace(),
          "usableSpace", store.getUsableSpace(),
          "unallocatedSpace", store.getUnallocatedSpace(),
          "readOnly", store.isReadOnly());
    }
    catch (IOException e) {
      log.error("Could not add report to support zip for file store {}", store.name(), e);
      return UNAVAILABLE;
    }
  }

  Map<String, Object> reportNetworkInterface(NetworkInterface intf) {
    try {
      return ImmutableMap.of(
          "displayName", intf.getDisplayName(),
          "up", intf.isUp(),
          "virtual", intf.isVirtual(),
          "multicast", intf.supportsMulticast(),
          "loopback", intf.isLoopback(),
          "ptp", intf.isPointToPoint(),
          "mtu", intf.getMTU(),
          "addresses", Collections.list(intf.getInetAddresses())
              .stream()
              .map(Object::toString)
              .collect(Collectors.joining(",")));
    }
    catch (SocketException e) {
      log.error("Could not add report to support zip for network interface {}", intf.getDisplayName(), e);
      return UNAVAILABLE;
    }
  }

  private Map<String, String> reportObfuscatedProperties(Properties properties) {
    Map<String, String> map = new HashMap<>();
    for (String key : properties.stringPropertyNames()) {
      map.put(key, properties.getProperty(key));
    }
    return reportObfuscatedProperties(map);
  }

  private Map<String, String> reportObfuscatedProperties(Map<String, String> properties) {
    return properties.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              String key = entry.getKey();
              String value = entry.getValue();
              for (String sensitiveName : SENSITIVE_FIELD_NAMES) {
                if (key.toLowerCase(Locale.US).contains(sensitiveName)) {
                  value = Strings2.mask(value);
                }
                if (SENSITIVE_CREDENTIALS_KEYS.contains(key) && value.contains(sensitiveName)) {
                  value = value.replaceAll(sensitiveName + "=\\S*", sensitiveName + "=" + MASK);
                }
              }
              return value;
            }));
  }
}
