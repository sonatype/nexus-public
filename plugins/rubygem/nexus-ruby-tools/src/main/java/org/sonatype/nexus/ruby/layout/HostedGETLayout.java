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
package org.sonatype.nexus.ruby.layout;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.sonatype.nexus.ruby.DependencyFile;
import org.sonatype.nexus.ruby.DependencyHelper;
import org.sonatype.nexus.ruby.GemFile;
import org.sonatype.nexus.ruby.GemspecFile;
import org.sonatype.nexus.ruby.GemspecHelper;
import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.SpecsHelper;
import org.sonatype.nexus.ruby.SpecsIndexFile;
import org.sonatype.nexus.ruby.SpecsIndexType;
import org.sonatype.nexus.ruby.SpecsIndexZippedFile;

/**
 * this hosted layout for HTTP GET will ensure that the zipped version of the specs.4.8
 * do exists before retrieving the unzipped ones. it also creates missing gemspec and dependency
 * files if missing.
 *
 * @author christian
 */
public class HostedGETLayout
    extends GETLayout
{
  public HostedGETLayout(RubygemsGateway gateway, Storage store) {
    super(gateway, store);
  }

  @Override
  protected void retrieveZipped(SpecsIndexZippedFile specs) {
    super.retrieveZipped(specs);
    if (specs.notExists()) {
      try (InputStream content = gateway.newSpecsHelper().createEmptySpecs()) {
        // just update in case so no need to deal with concurrency
        // since once the file is there no update happen again
        store.update(IOUtil.toGzipped(content), specs);
        store.retrieve(specs);
      }
      catch (IOException e) {
        specs.setException(e);
      }
    }
  }

  @Override
  public GemspecFile gemspecFile(String name, String version, String platform) {
    GemspecFile gemspec = super.gemspecFile(name, version, platform);

    if (gemspec.notExists()) {
      createGemspec(gemspec);
    }

    return gemspec;
  }

  @Override
  public GemspecFile gemspecFile(String filename) {
    GemspecFile gemspec = super.gemspecFile(filename);

    if (gemspec.notExists()) {
      createGemspec(gemspec);
    }

    return gemspec;
  }

  /**
   * create the gemspec from the stored gem file. if the gem file does not
   * exists, the <code>GemspecFile</code> gets makred as NOT_EXISTS.
   */
  protected void createGemspec(GemspecFile gemspec) {
    GemFile gem = gemspec.gem();
    if (gem.notExists()) {
      gemspec.markAsNotExists();
    }
    else {
      try(InputStream is = store.getInputStream(gemspec.gem())) {
        GemspecHelper spec = gateway.newGemspecHelperFromGem(is);

        // just update in case so no need to deal with concurrency
        // since once the file is there no update happen again
        store.update(spec.getRzInputStream(), gemspec);

        store.retrieve(gemspec);
      }
      catch (IOException e) {
        gemspec.setException(e);
      }
    }
  }

  @Override
  public DependencyFile dependencyFile(String name) {
    DependencyFile file = super.dependencyFile(name);
    store.retrieve(file);
    if (file.notExists()) {
      createDependency(file);
    }

    return file;
  }

  /**
   * create the <code>DependencyFile</code> for the given gem name
   */
  protected void createDependency(DependencyFile file) {
    try {
      SpecsIndexFile specs = specsIndexFile(SpecsIndexType.RELEASE);
      store.retrieve(specs);
      if (specs.hasException()) {
        file.setException(specs.getException());
        return;
      }
      List<String> versions;
      SpecsHelper specsHelper = gateway.newSpecsHelper();
      try (InputStream is = store.getInputStream(specs)) {
        versions = specsHelper.listAllVersions(file.name(), is);
      }
      specs = specsIndexFile(SpecsIndexType.PRERELEASE);
      store.retrieve(specs);
      try (InputStream is = store.getInputStream(specs)) {
        versions.addAll(specsHelper.listAllVersions(file.name(), is));
      }

      DependencyHelper gemspecs = gateway.newDependencyHelper();
      for (String version : versions) {
        // ruby platform is not part of the gemname
        GemspecFile gemspec = gemspecFile(file.name() + "-" + version.replaceFirst("-ruby$", ""));
        try (InputStream is = store.getInputStream(gemspec)) {
          gemspecs.addGemspec(is);
        }
      }

      try (InputStream is = gemspecs.getInputStream(false)) {
        store.update(is, file);
      }
      store.retrieve(file);
    }
    catch (IOException e) {
      file.setException(e);
    }
  }
}
