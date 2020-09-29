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
import java.io.UncheckedIOException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.thread.io.StreamCopier;
import org.sonatype.repository.helm.internal.util.YamlParser;

import com.google.common.io.ByteStreams;

/**
 * Removes absolute URL entries from index.yaml
 *
 * @since 3.28
 */
@Named
@Singleton
public class IndexYamlAbsoluteUrlRewriter
    extends IndexYamlAbsoluteUrlRewriterSupport
{
  private static final String YAML_CONTENT_TYPE = "text/x-yaml";

  @Inject
  public IndexYamlAbsoluteUrlRewriter(final YamlParser yamlParser) {
    super(yamlParser);
  }

  @Nullable
  public Content removeUrlsFromIndexYaml(final TempBlob index, final AttributesMap attributes) {
    if (index == null) {
      return null;
    }

    try (InputStream inputStream = index.get()) {
      return new StreamCopier<>(outputStream -> updateUrls(inputStream, outputStream), input -> createContent(input, attributes)).read();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Nullable
  public Content removeUrlsFromIndexYaml(final Content index) {
    if (index == null) {
      return null;
    }

    try (InputStream inputStream = index.openInputStream()) {
      return new StreamCopier<>(outputStream -> updateUrls(inputStream, outputStream), input -> createContent(input, index.getAttributes())).read();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private Content createContent(final InputStream input, final AttributesMap attributes) {
    try {
      Content content = new Content(new BytesPayload(ByteStreams.toByteArray(input), YAML_CONTENT_TYPE));
      attributes.forEach(attr-> content.getAttributes().set(attr.getKey(), attr.getValue()));
      return content;
    } catch (IOException ex) {
      log.error("Error rewriting urls in index.yaml", ex);
      throw new UncheckedIOException(ex);
    }
  }
}
