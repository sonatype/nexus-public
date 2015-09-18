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
package org.sonatype.nexus.mime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse mime type extensions and overrides from classpath.
 *
 * This class will look up '/builtin-mimetypes.properties' and '/nexus.mimetypes' from classpath.
 * These property files must have the following format:
 * <ul>
 * <li><em>additional mimetypes:</em>
 * A mapping from file name extension to a comma separated list of mime types (e.g. "bz2:
 * application/x-bzip2,application/x-bzip").
 * No whitespace is allowed in the list of mime types.</li>
 *
 * <li><em>overriding mimetypes:</em>
 * The format is the same as adding mime types, but prefixed with 'override.' (e.g. "override.html:
 * text/xhtml,application/xml").
 * These mime type definitions will override the builtin mime types.
 * The first listed mimetype is the 'primary' mime type and will be used by Nexus as the downstream content type.
 * </li>
 * </ul>
 *
 * @since 2.3
 */
public class NexusMimeTypes
{

  private static Logger log = LoggerFactory.getLogger(NexusMimeTypes.class);

  public static final String BUILTIN_MIMETYPES_FILENAME = "builtin-mimetypes.properties";

  public static final String MIMETYPES_FILENAME = "nexus.mimetypes";

  private Map<String, MimeRule> extensions = Maps.newHashMap();

  public NexusMimeTypes() {
    load(BUILTIN_MIMETYPES_FILENAME);
    load(MIMETYPES_FILENAME);
  }

  private void load(final String filename) {
    final InputStream stream = this.getClass().getResourceAsStream("/" + filename);
    if (stream != null) {
      final Properties properties = new Properties();
      try {
        properties.load(stream);
        initMimeTypes(properties);
      }
      catch (IOException e) {
        if (log.isDebugEnabled()) {
          log.warn("Could not load " + MIMETYPES_FILENAME, e);
        }
        else {
          log.warn("Could not load " + MIMETYPES_FILENAME + ": {}", e.getMessage());
        }
      }
    }
  }

  @VisibleForTesting
  void initMimeTypes(final Properties properties) {
    final Set<String> keys = properties.stringPropertyNames();
    final Map<String, List<String>> overrides = Maps.newHashMap();
    final Map<String, List<String>> additional = Maps.newHashMap();

    for (String key : keys) {
      if (key.startsWith("override.")) {
        overrides.put(key.substring("override.".length()), types(properties.getProperty(key, null)));
      }
      else {
        additional.put(key, types(properties.getProperty(key, null)));
      }
    }

    for (String extension : overrides.keySet()) {
      final List<String> mimetypes = overrides.get(extension);

      if (additional.containsKey(extension)) {
        mimetypes.addAll(additional.get(extension));
        additional.remove(extension);
      }
      this.extensions.put(extension, new MimeRule(true, mimetypes));
    }

    for (String extension : additional.keySet()) {
      final List<String> mimetypes = additional.get(extension);
      this.extensions.put(extension, new MimeRule(false, mimetypes));
    }
  }

  private List<String> types(final String value) {
    if (value == null) {
      return Collections.emptyList();
    }
    else {
      return Lists.newArrayList(Splitter.on(",").split(value));
    }
  }

  /**
   * Method returning {@link MimeRule} for given extension (it must not contain leading dot!). If no rule exists, {@code
   * null} is returned.
   */
  @Nullable
  public MimeRule getMimeRuleForExtension(String extension) {
    while (!extension.isEmpty()) {
      if (extensions.containsKey(extension)) {
        return extensions.get(extension);
      }
      extension = Files.getFileExtension(extension);
    }
    return null;
  }
}
