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
package org.sonatype.nexus.internal.atlas.customizers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.supportzip.FileContentSourceSupport;
import org.sonatype.nexus.supportzip.SanitizedXmlSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.common.jdbc.JdbcUrlRedactor.redactPassword;
import static org.sonatype.nexus.supportzip.PasswordSanitizing.REPLACEMENT;
import static org.sonatype.nexus.supportzip.PasswordSanitizing.SENSITIVE_FIELD_NAMES;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.HIGH;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.CONFIG;

/**
 * Adds installation directory configuration files to support bundle.
 *
 * @since 3.0
 */
@Named
@Singleton
public class InstallConfigurationCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private static final String INSTALL_ETC = "install/etc";

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public InstallConfigurationCustomizer(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    File installDir = applicationDirectories.getInstallDirectory();
    if (installDir != null) {
      File etcDir = new File(installDir, "etc");
      includeFileIfExists(supportBundle, new File(etcDir, "nexus-default.properties"), INSTALL_ETC, HIGH);
      includeAllFilesInDirIfExists(supportBundle, new File(etcDir, "fabric"), INSTALL_ETC, HIGH);
      includeAllFilesInDirIfExists(supportBundle, new File(etcDir, "jetty"), INSTALL_ETC, HIGH);
      includeAllFilesInDirIfExists(supportBundle, new File(etcDir, "karaf"), INSTALL_ETC, HIGH);
      includeAllFilesInDirIfExists(supportBundle, new File(etcDir, "logback"), INSTALL_ETC, HIGH);
    }

    File workDir = applicationDirectories.getWorkDirectory();
    if (workDir != null) {
      File etcDir = new File(workDir, "etc");
      includeFileIfExists(supportBundle, new File(etcDir, "nexus.properties"), INSTALL_ETC, HIGH);
      includeAllFilesInDirIfExists(supportBundle, new File(etcDir, "fabric"), INSTALL_ETC, HIGH);
      includeAllFilesInDirIfExists(supportBundle, new File(etcDir, "logback"), INSTALL_ETC, HIGH);
    }
  }

  private void includeFileIfExists(
      final SupportBundle supportBundle,
      final File file,
      final String prefixDir,
      final Priority priority)
  {
    if (file != null && file.isFile()) {
      log.debug("Including file: {}", file);
      String filePath = String.join("/", prefixDir, file.getName());
      try {
        switch (file.getName()) {
          case "jetty-https.xml":
            supportBundle.add(new SanitizedJettyFileSource(CONFIG, filePath, file, priority));
            break;
          case "hazelcast.xml":
          case "hazelcast-network.xml":
            supportBundle.add(new SanitizedHazelcastFileSource(CONFIG, filePath, file, priority));
            break;
          case "store.properties":
            supportBundle.add(new SanitizedDataStoreFileSource(CONFIG, filePath, file, priority));
            break;
          case "nexus.properties":
            supportBundle.add(new SanitizedNexusFileSource(CONFIG, filePath, file, priority));
            break;
          default:
            supportBundle.add(new FileContentSourceSupport(CONFIG, filePath, file, priority));
            break;
        }
      }
      catch (IOException e) {
        log.warn("Failed to sanitize {}", file, e);
      }
    }
    else {
      log.warn("Skipping: {}", file);
    }
  }

  private void includeAllFilesInDirIfExists(
      final SupportBundle supportBundle,
      final File directory,
      final String prefixDir,
      final Priority priority)
  {
    if (directory != null && directory.isDirectory()) {
      log.debug("Including dir: {}", directory);
      stream(directory.listFiles()).forEach(
          file -> includeFileIfExists(supportBundle, file, String.join("/", prefixDir, directory.getName()), priority));
    }
    else {
      log.warn("Skipping: {}", directory);
    }
  }

  /**
   * Ad-hoc subclass that encapsulates the XSLT transformation of a Jetty configuration file, removing passwords.
   */
  protected static class SanitizedJettyFileSource
      extends SanitizedXmlSourceSupport
  {
    /**
     * Constructor.
     */
    public SanitizedJettyFileSource(final Type type, final String path, final File file, final Priority priority)
        throws IOException
    {
      super(type, path, file, priority,
          IOUtils.toString(checkNotNull(SanitizedJettyFileSource.class.getResourceAsStream("jetty-stylesheet.xml")),
              UTF_8));
    }
  }

  /**
   * Removes AWS credentials from hazelcast.xml, if present.
   */
  protected static class SanitizedHazelcastFileSource
      extends SanitizedXmlSourceSupport
  {
    public SanitizedHazelcastFileSource(final Type type, final String path, final File file, final Priority priority)
        throws IOException
    {
      super(type, path, file, priority,
          IOUtils.toString(checkNotNull(SanitizedJettyFileSource.class.getResourceAsStream("hazelcast-stylesheet.xml")),
              UTF_8));
    }
  }

  /**
   * Removes JDBC credentials from *-store.properties, if present.
   */
  protected static class SanitizedDataStoreFileSource
      extends FileContentSourceSupport
  {
    public SanitizedDataStoreFileSource(final Type type, final String path, final File file, final Priority priority) {
      super(CONFIG, path, file, priority);
    }

    @Override
    public InputStream getContent() throws Exception {
      PropertiesFile dataStoreConfiguration = new PropertiesFile(file);
      dataStoreConfiguration.load();
      dataStoreConfiguration.forEach((k, v) -> {
        if (SENSITIVE_FIELD_NAMES.contains(k)) {
          dataStoreConfiguration.replace(k, REPLACEMENT);
        }
        else if ("jdbcUrl".equals(k)) {
          dataStoreConfiguration.put(k, redactPassword((String) v));
        }
      });
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      dataStoreConfiguration.store(outputStream, null);
      return new ByteArrayInputStream(outputStream.toByteArray());
    }
  }

  protected static class SanitizedNexusFileSource
    extends FileContentSourceSupport
  {
        public SanitizedNexusFileSource(final Type type, final String path, final File file, final Priority priority) {
          super(CONFIG, path, file, priority);
        }

        @Override
        public InputStream getContent() throws Exception {
          PropertiesFile dataStoreConfiguration = new PropertiesFile(file);
          dataStoreConfiguration.load();
          dataStoreConfiguration.forEach((k, v) -> {
                if (SENSITIVE_FIELD_NAMES.contains(k)) {
                  dataStoreConfiguration.replace(k, REPLACEMENT);
                }
                else if ("nexus.datastore.nexus.jdbcUrl".equals(k)) {
                  dataStoreConfiguration.put(k, redactPassword((String) v));
                }
          });
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          dataStoreConfiguration.store(outputStream, null);
          return new ByteArrayInputStream(outputStream.toByteArray());
        }
  }
}
