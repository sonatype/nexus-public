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
package org.sonatype.nexus.yum.internal.capabilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;
import org.sonatype.aether.version.VersionScheme;
import org.sonatype.nexus.capability.support.CapabilitySupport;
import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.nexus.plugins.capabilities.Evaluable;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.internal.task.CommandLineExecutor;

import org.jetbrains.annotations.NonNls;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * YUM capability.
 *
 * @since yum 3.0
 */
@Named(YumCapabilityDescriptor.TYPE_ID)
public class YumCapability
    extends CapabilitySupport<YumCapabilityConfiguration>
{
  @NonNls
  public static final String NL = System.getProperty("line.separator");

  private final YumRegistry yumRegistry;

  private final CommandLineExecutor commandLineExecutor;

  private String binariesLog;

  private String binariesNotAvailableMessage;

  @Inject
  public YumCapability(final YumRegistry yumRegistry,
                       final CommandLineExecutor commandLineExecutor)
  {
    this.yumRegistry = checkNotNull(yumRegistry);
    this.commandLineExecutor = checkNotNull(commandLineExecutor);
  }

  @Override
  protected YumCapabilityConfiguration createConfig(final Map<String, String> properties) throws Exception {
    return new YumCapabilityConfiguration(properties);
  }

  @Override
  protected void onActivate(final YumCapabilityConfiguration config) throws Exception {
    yumRegistry.setMaxNumberOfParallelThreads(config.maxNumberParallelThreads());
    yumRegistry.setCreaterepoPath(config.getCreaterepoPath());
    yumRegistry.setMergerepoPath(config.getMergerepoPath());
  }

  /**
   * Only activate if create/mergerepo are available (running with --version executes successfully).
   *
   * @since 2.11
   */
  @Override
  public Condition activationCondition() {
    return conditions().capabilities().evaluable(
        new Evaluable()
        {
          @Override
          public boolean isSatisfied() {
            StringBuilder message = new StringBuilder();
            StringBuilder verificationLog = new StringBuilder();

            validate("createrepo", getConfig().getCreaterepoPath(), "[0.9.9,)", message, verificationLog);
            verificationLog.append(NL);
            validate("mergerepo", getConfig().getMergerepoPath(), "[0.1,)", message, verificationLog);

            binariesLog = verificationLog.toString().replace(NL, "<br/>");
            binariesNotAvailableMessage = null;
            if (message.length() > 0) {
              binariesNotAvailableMessage = message.toString();
            }
            return binariesNotAvailableMessage == null;
          }

          @Override
          public String explainSatisfied() {
            return "\"createrepo\" and \"mergerepo\" are available";
          }

          @Override
          public String explainUnsatisfied() {
            return binariesNotAvailableMessage;
          }

          private void validate(final String type, final String path,
                                final String versionConstraint,
                                final StringBuilder message, final StringBuilder verificationLog)
          {
            String and = message.length() > 0 ? " and " : "";
            verificationLog.append("\"").append(type).append("\" version:").append(NL);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
              if (commandLineExecutor.exec(path, "--version", baos, baos) == 0) {
                String versionOutput = new String(baos.toByteArray());
                verificationLog.append(versionOutput);
                ensureVersionCompatible(type, versionOutput, versionConstraint, message, verificationLog);
              }
              else {
                message.append(and).append("\"").append(type).append("\" not available");
                verificationLog.append(new String(baos.toByteArray()));
              }
            }
            catch (IllegalAccessException e) {
              log.debug("path supplied {} not allowed to run", path, e);
              message.append(and).append("\"").append(type).append("\" not allowed");
              verificationLog.append(new String(baos.toByteArray()));
              verificationLog.append(e.getMessage()).append(NL);
            }
            catch (IOException e) {
              message.append(and).append("\"").append(type).append("\" not available");
              verificationLog.append(new String(baos.toByteArray()));
              verificationLog.append(e.getMessage()).append(NL);
            }
          }

          private void ensureVersionCompatible(final String type, final String rawVersion,
                                               final String versionConstraint,
                                               final StringBuilder message, final StringBuilder verificationLog)
          {
            String version = parseVersion(type, rawVersion);
            if (version == null) {
              verificationLog.append("WARNING: Could not determine version!").append(NL);
            }
            else {
              try {
                VersionScheme scheme = new GenericVersionScheme();
                VersionConstraint constraint = scheme.parseVersionConstraint(versionConstraint);
                Version _version = scheme.parseVersion(version);
                if (!constraint.containsVersion(_version)) {
                  message.append("\"").append(type).append("\" incompatible version");
                  verificationLog.append(NL);
                  verificationLog.append("Incompatible ").append(type).append(" version detected:").append(NL);
                  verificationLog.append("Detected version: ").append(_version).append(NL);
                  verificationLog.append("Compatible version constraint: ").append(constraint).append(NL);
                }
              }
              catch (Exception e) {
                log.debug("Could not parse version from " + version, e);
                verificationLog.append("WARNING: Could not parse version from ").append(version).append("!").append(NL);
              }
            }
          }

          private String parseVersion(final String type, final String rawVersion) {
            try {
              String[] lines = rawVersion.split("\n");
              for (String line : lines) {
                line = line.trim();

                if (line.startsWith(type + " ")) { // trailing space in string on purpose
                  String[] parts = line.split("\\s");
                  // ignore the build #
                  return parts[1];
                }
              }
            }
            catch (Exception e) {
              log.debug("Could not determine version", e);
            }
            return null;
          }
        }
    );
  }

  /**
   * Show output of create/mergerepo --version.
   *
   * @since 2.11
   */
  @Override
  protected String renderStatus() {
    return binariesLog;
  }
}
