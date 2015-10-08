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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParserFactory;

import org.sonatype.goodies.common.FileReplacer;
import org.sonatype.goodies.common.FileReplacer.ContentWriter;
import org.sonatype.nexus.common.log.LoggerLevel;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Utility class for reading/writing {@code logback-overrides.xml}.
 *
 * @since 2.7
 */
public class LogbackOverrides
{
  private LogbackOverrides() {
    // empty
  }

  /**
   * Reads loggers/levels from logback-overrides.xml.
   */
  static Map<String, LoggerLevel> read(final File overridesXml) {
    try {
      final Map<String, LoggerLevel> loggers = Maps.newHashMap();

      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setValidating(false);
      spf.setNamespaceAware(true);
      spf.newSAXParser().parse(overridesXml, new DefaultHandler()
      {
        @Override
        public void startElement(final String uri,
                                 final String localName,
                                 final String qName,
                                 final Attributes attributes) throws SAXException
        {
          if ("logger".equals(localName)) {
            String name = attributes.getValue("name");
            String level = attributes.getValue("level");
            loggers.put(name, LoggerLevel.valueOf(level));
          }
        }
      });
      return loggers;
    }
    catch (Exception e) {
      // TODO shall we just log and continue?
      throw Throwables.propagate(e);
    }
  }

  /**
   * Writes loggers/levels to logback-overrides.xml.
   */
  static void write(final File overridesXml, final Map<String, LoggerLevel> overrides) {
    try {
      final FileReplacer fileReplacer = new FileReplacer(overridesXml);
      fileReplacer.setDeleteBackupFile(true);
      fileReplacer.replace(new ContentWriter()
      {
        @Override
        public void write(final BufferedOutputStream output)
            throws IOException
        {
          try (final PrintWriter out = new PrintWriter(output)) {
            out.println("<?xml version='1.0' encoding='UTF-8'?>");
            out.println();
            out.println("<!--");
            out.println("DO NOT EDIT - Automatically generated; User-customized logging levels");
            out.println("-->");
            out.println();
            out.println("<included>");
            for (Entry<String, LoggerLevel> entry : overrides.entrySet()) {
              if ("ROOT".equals(entry.getKey())) {
                out.format("  <property name='root.level' level='%s'/>%n", entry.getValue());
              }
              else {
                out.format("  <logger name='%s' level='%s'/>%n", entry.getKey(), entry.getValue());
              }
            }
            out.println("</included>");
          }
        }
      });
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
