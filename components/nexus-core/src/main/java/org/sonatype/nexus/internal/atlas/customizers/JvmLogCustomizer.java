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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.text.Strings2.MASK;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.LOW;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.LOG;

/**
 * Adds jvm log file to support bundle.
 * Masks sensitive data passed as JVM arguments.
 */
@Named
@Singleton
public class JvmLogCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private static final List<String> SENSITIVE_FIELD_NAMES =
      Arrays.asList("password", "secret", "token", "sign", "auth", "cred", "key", "pass");

  private final LogManager logManager;

  @Inject
  public JvmLogCustomizer(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    supportBundle.add(new GeneratedContentSourceSupport(LOG, "log/jvm.log", LOW)
    {
      @Override
      protected void generate(final File file) {
        File logFile = logManager.getLogFile("jvm.log");

        if (logFile != null) {
          try (BufferedReader reader = new BufferedReader(new FileReader(logFile));
               BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            String line;
            while ((line = reader.readLine()) != null) {
              String redactedLine = maybeMaskSensitiveData(line);
              writer.write(redactedLine);
              writer.newLine();
            }
          } catch (IOException e) {
            log.debug("Unable to include jvm.log file", e);
          }
        }
        else {
          log.debug("Not including missing jvm.log file");
        }
      }

      private String maybeMaskSensitiveData(final String input) {
        String result = input;
        for (String name : SENSITIVE_FIELD_NAMES) {
          result =  result.replaceAll(name + "=\\S*", name + "=" + MASK);
        }
        return result;
      }
    });
  }
}
