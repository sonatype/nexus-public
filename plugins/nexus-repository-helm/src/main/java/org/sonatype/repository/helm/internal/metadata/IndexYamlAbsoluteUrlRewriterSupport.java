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
package org.sonatype.repository.helm.internal.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.repository.helm.internal.util.YamlParser;

import org.apache.http.client.utils.URIBuilder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.events.CollectionEndEvent;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ScalarEvent;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

/**
 * Removes absolute URL entries from index.yaml
 *
 * @since 3.28
 */
public class IndexYamlAbsoluteUrlRewriterSupport
    extends ComponentSupport
{
  private final YamlParser yamlParser;

  @Inject
  public IndexYamlAbsoluteUrlRewriterSupport(final YamlParser yamlParser) {
    this.yamlParser = checkNotNull(yamlParser);
  }

  private static final String URLS = "urls";

  protected void updateUrls(final InputStream is,
                            final OutputStream os)
  {
    try (Reader reader = new InputStreamReader(is);
         Writer writer = new OutputStreamWriter(os)) {
      Yaml yaml = new Yaml(new SafeConstructor());
      Emitter emitter = new Emitter(writer, new DumperOptions());
      boolean rewrite = false;
      for (Event event : yaml.parse(reader)) {
        if (event instanceof ScalarEvent) {
          ScalarEvent scalarEvent = (ScalarEvent) event;
          if (rewrite) {
            event = maybeSetAbsoluteUrlAsRelative(scalarEvent);
          }
          else if (URLS.equals(scalarEvent.getValue())) {
            rewrite = true;
          }
        }
        else if (event instanceof CollectionStartEvent) {
          // NOOP
        }
        else if (event instanceof CollectionEndEvent) {
          rewrite = false;
        }
        emitter.emit(event);
      }
    }
    catch (IOException ex) {
      log.error("Error rewriting urls in index.yaml", ex);
    }
  }

  protected Event maybeSetAbsoluteUrlAsRelative(ScalarEvent scalarEvent) {
    String oldUrl = scalarEvent.getValue();
    try {
      URI uri = new URIBuilder(oldUrl).build();
      if (uri.isAbsolute()) {
        String fileName = uri.getPath();
        // Rewrite absolute paths to relative
        if (!fileName.isEmpty()) {
          fileName = Paths.get(fileName).getFileName().toString();
        }
        scalarEvent = new ScalarEvent(scalarEvent.getAnchor(), scalarEvent.getTag(),
            scalarEvent.getImplicit(), fileName, scalarEvent.getStartMark(),
            scalarEvent.getEndMark(), scalarEvent.getStyle());
      }
    }
    catch (URISyntaxException ex) {
      log.error("Invalid URI in index.yaml", ex);
    }
    return scalarEvent;
  }

  @SuppressWarnings("unchecked")
  public Optional<String> getFirstUrl(final Content indexYaml, final String chartName, final String chartVersion) {
    checkNotNull(chartName);
    checkNotNull(chartVersion);

    try (InputStream inputStream = indexYaml.openInputStream()) {
      Map<String, Object> index = yamlParser.load(inputStream);
      Map<String, Object> entries = (Map<String, Object>) index.get("entries");
      List<Map<String, Object>> charsOfName = (List<Map<String, Object>>) entries.getOrDefault(chartName, emptyList());
      Optional<Map<String, Object>> chartOfVersion = charsOfName.stream()
          .filter(chart -> Objects.equals(chartVersion, chart.get("version")))
          .findFirst();

      return chartOfVersion
          .flatMap(chart -> ((List<String>) chart.get(URLS)).stream().findFirst());
    }
    catch (IOException e) {
      log.error("Error reading index.yaml");
      throw new UncheckedIOException(e);
    }
  }
}
