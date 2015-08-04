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
package org.sonatype.nexus.plugins.ruby.hosted;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.sonatype.nexus.plugins.ruby.NexusRubygemsFacade;
import org.sonatype.nexus.plugins.ruby.RubyRepository;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.walker.AbstractWalkerProcessor;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemFile;
import org.sonatype.nexus.ruby.GemspecHelper;
import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.SpecsHelper;
import org.sonatype.nexus.ruby.SpecsIndexType;

import org.jruby.runtime.builtin.IRubyObject;
import org.slf4j.Logger;

public class RecreateIndexRubygemsWalkerProcessor
    extends AbstractWalkerProcessor
{

  private RubyRepository repository;

  private final Logger logger;

  private final RubygemsGateway rubygems;

  private final SpecsHelper specs;
  
  private final ByteArrayInputStream[] index = new ByteArrayInputStream[SpecsIndexType.values().length];

  private final NexusRubygemsFacade facade;
  
  public RecreateIndexRubygemsWalkerProcessor(Logger logger, RubygemsGateway rubygems, NexusRubygemsFacade facade) {
    this.logger = logger;
    this.rubygems = rubygems;
    this.specs = rubygems.newSpecsHelper();
    for (SpecsIndexType type: SpecsIndexType.values()) {
      this.index[type.ordinal()] = specs.createEmptySpecs();
    }
    this.facade = facade;
  }

  @Override
  public void beforeWalk(WalkerContext context) throws Exception {
    repository = context.getRepository() instanceof RubyRepository ? (RubyRepository) context.getRepository() : null;
    setActive(repository != null);
  }

  @Override
  public void processItem(WalkerContext context, StorageItem item) {
    if (item instanceof StorageFileItem) {
      RubygemsFile file = facade.file(item.getPath());
      try {
        switch (file.type()) {
          case DEPENDENCY:
            logger.debug("deleting :: {}", file.storagePath());
            repository.deleteItem(true, new ResourceStoreRequest(item));
            break;
          case GEM:
            GemspecHelper gemspecHelper;
            try (InputStream is = ((StorageFileItem) item).getInputStream()) {
              gemspecHelper = rubygems.newGemspecHelperFromGem(is);
            }
            logger.debug("recreating :: {}", gemspecHelper.filename());
            try(InputStream is = gemspecHelper.getRzInputStream()){
              repository.storeItem(false, newStorageItem(((GemFile) file).gemspec(), is));
            }
            IRubyObject spec = gemspecHelper.gemspec();
            for (SpecsIndexType type : SpecsIndexType.values()) {
              logger.debug("adding :: {} to {}", spec, type);
              index[type.ordinal()].reset();
              ByteArrayInputStream result = specs.addSpec(spec, index[type.ordinal()], type);
              if (result != null && result.available() > 0) {
                this.index[type.ordinal()] = result;
              }
            }
            break;
          default:
        }
      }
      catch (Exception e) {
        logger.warn("Error occurred while processing item '" + item.getPath() + "'.", e);
      }
    }
  }
  
  public void storeIndex() {
    for (SpecsIndexType type : SpecsIndexType.values()) {
      try {
        index[type.ordinal()].reset();
        repository.storeItem(false, newStorageItem(facade.file(type.filepathGzipped()), 
            IOUtil.toGzipped(index[type.ordinal()])));
      }
      catch (Exception e) {
        logger.warn("Error occurred while processing item '" + type.filepathGzipped() + "'.", e);
      }
    }
  }
  
  public StorageItem newStorageItem(RubygemsFile file, InputStream is){
    ResourceStoreRequest request = new ResourceStoreRequest(file.storagePath());
    ContentLocator contentLocator = new PreparedContentLocator(is, file.type().mime(), ContentLocator.UNKNOWN_LENGTH);
    return new DefaultStorageFileItem(repository, request,
        true, true, contentLocator);
  }
}