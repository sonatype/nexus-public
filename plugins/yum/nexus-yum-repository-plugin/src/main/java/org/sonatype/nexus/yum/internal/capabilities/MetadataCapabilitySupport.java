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
package org.sonatype.nexus.yum.internal.capabilities;

import java.io.InputStream;
import java.util.Set;

import javax.inject.Inject;

import org.sonatype.nexus.capability.support.CapabilitySupport;
import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.nexus.plugins.capabilities.Tag;
import org.sonatype.nexus.plugins.capabilities.Taggable;
import org.sonatype.nexus.plugins.capabilities.support.condition.RepositoryConditions;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.yum.Yum;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.internal.YumConfigContentGenerator;

import com.google.common.base.Throwables;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.plugins.capabilities.Tag.repositoryTag;
import static org.sonatype.nexus.plugins.capabilities.Tag.tags;
import static org.sonatype.nexus.yum.internal.capabilities.MetadataCapabilityConfigurationSupport.REPOSITORY_ID;

/**
 * @since yum 3.0
 */
public abstract class MetadataCapabilitySupport<C extends MetadataCapabilityConfigurationSupport>
    extends CapabilitySupport<C>
    implements Taggable
{

  private final YumRegistry yumRegistry;

  private final RepositoryRegistry repositoryRegistry;

  @Inject
  public MetadataCapabilitySupport(final YumRegistry yumRegistry,
                                   final RepositoryRegistry repositoryRegistry)
  {
    this.yumRegistry = checkNotNull(yumRegistry);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
  }

  @Override
  public String renderDescription() {
    if (isConfigured()) {
      try {
        return repositoryRegistry.getRepository(getConfig().repository()).getName();
      }
      catch (NoSuchRepositoryException e) {
        return getConfig().repository();
      }
    }
    return null;
  }

  @Override
  public void onUpdate(final C config)
      throws Exception
  {
    final Yum yum = yumRegistry.get(config.repository());
    // yum is not present when repository is changed
    if (yum != null) {
      configureYum(yum, config);
    }
  }

  @Override
  public void onActivate(final C config) {
    try {
      final Repository repository = repositoryRegistry.getRepository(config.repository());
      configureYum(yumRegistry.register(repository.adaptToFacet(MavenRepository.class)), config);
    }
    catch (NoSuchRepositoryException e) {
      // TODO
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void onPassivate(final C config) {
    yumRegistry.unregister(config.repository());
  }

  @Override
  public Condition activationCondition() {
    return conditions().logical().and(
        conditions().capabilities().capabilityOfTypeActive(YumCapabilityDescriptor.TYPE),
        conditions().repository().repositoryIsInService(new RepositoryConditions.RepositoryId()
        {
          @Override
          public String get() {
            return isConfigured() ? getConfig().repository() : null;
          }
        }),
        conditions().capabilities().passivateCapabilityWhenPropertyChanged(REPOSITORY_ID)
    );
  }

  @Override
  public Condition validityCondition() {
    return conditions().repository().repositoryExists(new RepositoryConditions.RepositoryId()
    {
      @Override
      public String get() {
        return isConfigured() ? getConfig().repository() : null;
      }
    });
  }

  @Override
  public String renderStatus() {
    if (isConfigured()) {
      try {
        final Repository repository = repositoryRegistry.getRepository(getConfig().repository());
        final StorageItem storageItem = repository.retrieveItem(
            new ResourceStoreRequest(YumConfigContentGenerator.configFilePath(repository.getId()), true)
        );
        if (storageItem instanceof StorageFileItem) {
          try (InputStream in = ((StorageFileItem) storageItem).getInputStream()) {
            return
                "<b>Example Yum configuration file:</b><br/><br/>"
                    + "<pre>"
                    + IOUtils.toString(in)
                    + "</pre>";
          }
        }
      }
      catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  void configureYum(final Yum yum, C config) {
    // template method
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + (isConfigured() ? "{repository=" + getConfig().repository() + "}" : "");
  }

  @Override
  public Set<Tag> getTags() {
    return tags(repositoryTag(renderDescription()));
  }

}
