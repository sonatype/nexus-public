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
package org.sonatype.nexus.internal.log;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.SAXParserFactory;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.FileReplacer;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.log.LoggerLevel;

import ch.qos.logback.classic.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Logback {@link LoggerOverrides} implementation.
 *
 * Special handling for {@code ROOT} logger, which is persisted as {@code root.level} property.
 *
 * @since 2.7
 */
@Named
@Singleton
public class LogbackLoggerOverrides
    extends ComponentSupport
    implements LoggerOverrides
{
  private final File file;

  private final Map<String, LoggerLevel> loggerLevels = new HashMap<>();

  @Inject
  public LogbackLoggerOverrides(final ApplicationDirectories applicationDirectories) {
    checkNotNull(applicationDirectories);
    this.file = new File(applicationDirectories.getWorkDirectory("etc/logback"), "logback-overrides.xml");
    log.info("File: {}", file);
  }

  @VisibleForTesting
  LogbackLoggerOverrides(final File file) {
    this.file = checkNotNull(file);
  }

  @Override
  public synchronized void load() {
    log.debug("Load");

    loggerLevels.clear();
    if (file.exists()) {
      try {
        loggerLevels.putAll(read(file));
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }

  @Override
  public synchronized void save() {
    log.debug("Save");

    try {
      write(file, loggerLevels);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public synchronized void reset() {
    log.debug("Reset");

    loggerLevels.clear();
    try {
      write(file, loggerLevels);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public synchronized void set(final String name, final LoggerLevel level) {
    log.debug("Set: {}={}", name, level);

    loggerLevels.put(name, level);
  }

  @Override
  @Nullable
  public synchronized LoggerLevel get(final String name) {
    return loggerLevels.get(name);
  }

  @Override
  @Nullable
  public synchronized LoggerLevel remove(final String name) {
    log.debug("Remove: {}", name);

    return loggerLevels.remove(name);
  }

  @Override
  public synchronized boolean contains(final String name) {
    return loggerLevels.containsKey(name);
  }

  @Override
  public synchronized Iterator<Entry<String, LoggerLevel>> iterator() {
    return ImmutableMap.copyOf(loggerLevels).entrySet().iterator();
  }

  //
  // Logback xml-format I/O
  //

  /**
   * Read logger levels from logback.xml formatted include file.
   */
  private Map<String, LoggerLevel> read(final File inputFile) throws Exception {
    final Map<String, LoggerLevel> result = Maps.newHashMap();

    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    parserFactory.setValidating(false);
    parserFactory.setNamespaceAware(true);
    parserFactory.newSAXParser().parse(inputFile, new DefaultHandler()
    {
      @Override
      public void startElement(final String uri,
                               final String localName,
                               final String qName,
                               final Attributes attributes) throws SAXException
      {
        // NOTE: ATM we are ignoring 'property' elements, this is needed for root, but is only needed
        // NOTE: to persist as a property for use in top-level logback.xml file

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
  private void write(final File outputFile, final Map<String, LoggerLevel> overrides) throws Exception {
    final FileReplacer fileReplacer = new FileReplacer(outputFile);
    fileReplacer.setDeleteBackupFile(true);
    fileReplacer.replace(output -> {
      try (final PrintWriter out = new PrintWriter(output)) {
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.println();
        out.println("<!--");
        out.println("DO NOT EDIT - Automatically generated; User-customized logging levels");
        out.println("-->");
        out.println();
        out.println("<included>");
        for (Entry<String, LoggerLevel> entry : overrides.entrySet()) {
          if (Logger.ROOT_LOGGER_NAME.equals(entry.getKey())) {
            out.format("  <property name='root.level' value='%s'/>%n", entry.getValue());
          }
          else {
            out.format("  <logger name='%s' level='%s'/>%n", entry.getKey(), entry.getValue());
          }
        }
        out.println("</included>");
      }
    });
  }
}
