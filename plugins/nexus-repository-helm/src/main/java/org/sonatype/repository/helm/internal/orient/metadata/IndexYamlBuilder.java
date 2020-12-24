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
package org.sonatype.repository.helm.internal.orient.metadata;

import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.thread.io.StreamCopier;
import org.sonatype.repository.helm.internal.HelmFormat;
import org.sonatype.repository.helm.internal.metadata.ChartIndex;
import org.sonatype.repository.helm.internal.util.YamlParser;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.28
 */
@Named
public class IndexYamlBuilder
{
  private final YamlParser yamlParser;

  @Inject
  public IndexYamlBuilder(final YamlParser yamlParser) {
    this.yamlParser = checkNotNull(yamlParser);
  }

  public TempBlob build(final ChartIndex index, final StorageFacet storageFacet) {
    return new StreamCopier<>(os -> readIntoYaml(os, index), is -> createTempBlob(is, storageFacet)).read();
  }

  private void readIntoYaml(final OutputStream os, final ChartIndex index) {
    yamlParser.write(os, index);
  }

  private TempBlob createTempBlob(final InputStream inputStream, final StorageFacet storageFacet) {
    return storageFacet.createTempBlob(inputStream, HelmFormat.HASH_ALGORITHMS);
  }
}
