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
package org.sonatype.nexus.internal.log.overrides;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.sonatype.nexus.common.io.FileReplacer;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.common.io.SafeXml;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.internal.log.LogbackLevels;
import org.sonatype.nexus.internal.log.LoggerOverrides;

import ch.qos.logback.classic.LoggerContext;
import com.google.common.collect.Maps;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support logback files operations for {@link LoggerOverrides}
 */
public abstract class LogbackLoggerOverridesSupport
    extends StateGuardLifecycleSupport
{
  private static final String LOGBACK_OVERRIDES_FILENAME = "logback-overrides.xml";

  private final File logbackFile;

  protected LogbackLoggerOverridesSupport(final ApplicationDirectories applicationDirectories) {
    this(new File(checkNotNull(applicationDirectories)
        .getWorkDirectory("etc/logback"), LOGBACK_OVERRIDES_FILENAME));
  }

  protected LogbackLoggerOverridesSupport(final File logbackFile) {
    this.logbackFile = checkNotNull(logbackFile);
    log.info("File: {}", logbackFile);
  }

  protected boolean logbackFileExists() {
    return logbackFile.exists();
  }

  /**
   * Applies the given logger-level overrides directly to the live logback context. Each store calls this
   * from its own lifecycle phase (when its persisted overrides are available), so no earlier-phase component
   * has to load or apply a later-phase store's overrides on its behalf.
   */
  protected void applyToLogback(final Map<String, LoggerLevel> overrides) {
    LoggerContext context = loggerContext();
    overrides.forEach((name, level) -> context.getLogger(name).setLevel(LogbackLevels.convert(level)));
  }

  private static LoggerContext loggerContext() {
    ILoggerFactory factory = LoggerFactory.getILoggerFactory();
    if (factory instanceof LoggerContext) {
      return (LoggerContext) factory;
    }
    // Nexus runs on Spring Boot with logback-classic as the SLF4J binding, so the factory is always a logback
    // LoggerContext. Fail with a clear message if that ever stops being true rather than a bare ClassCastException.
    throw new IllegalStateException(
        "Expected the SLF4J binding to be logback, but found: " + factory.getClass().getName());
  }

  /**
   * Read logger levels from logback.xml formatted include file.
   */
  protected Map<String, LoggerLevel> readFromFile() throws Exception {
    final Map<String, LoggerLevel> result = Maps.newHashMap();

    SAXParserFactory parserFactory = SafeXml.newSaxParserFactory();
    parserFactory.setValidating(false);
    parserFactory.setNamespaceAware(true);
    SAXParser parser = parserFactory.newSAXParser();
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    parser.parse(logbackFile, new DefaultHandler()
    {
      @Override
      public void startElement(
          final String uri,
          final String localName,
          final String qName,
          final Attributes attributes) throws SAXException
      {
        if ("logger".equals(localName)) {
          String name = attributes.getValue("name");
          String level = attributes.getValue("level");
          result.put(name, LoggerLevel.valueOf(level));
        }
      }
    });
    return result;
  }

  /**
   * Write logger levels and root property to logback.xml formatted include file.
   */
  protected void writeToFile(final Map<String, LoggerLevel> overrides) throws Exception {
    final FileReplacer fileReplacer = new FileReplacer(logbackFile);
    fileReplacer.setDeleteBackupFile(true);
    fileReplacer.replace(output -> {
      try (final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
        out.write("<?xml version='1.0' encoding='UTF-8'?>");
        out.newLine();
        out.newLine();
        out.write("<!--");
        out.newLine();
        out.write("DO NOT EDIT - Automatically generated; User-customized logging levels");
        out.newLine();
        out.write("-->");
        out.newLine();
        out.newLine();
        out.write("<included>");
        out.newLine();
        for (Entry<String, LoggerLevel> entry : overrides.entrySet()) {
          out.write(String.format("  <logger name='%s' level='%s'/>%n", entry.getKey(), entry.getValue()));
        }
        out.write("</included>");
        out.newLine();
      }
    });
  }
}
