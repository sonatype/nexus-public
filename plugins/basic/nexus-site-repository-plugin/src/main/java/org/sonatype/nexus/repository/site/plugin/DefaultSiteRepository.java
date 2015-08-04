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
package org.sonatype.nexus.repository.site.plugin;

import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractWebSiteRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.repository.WebSiteRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default Site Repository.
 *
 * @author cstamas
 */
@Named(SiteRepository.ID)
public class DefaultSiteRepository
    extends AbstractWebSiteRepository
    implements SiteRepository, WebSiteRepository
{

  private final ContentClass contentClass;

  private final Configurator repositoryConfigurator;

  @Inject
  public DefaultSiteRepository(final @Named(SiteRepository.ID) ContentClass contentClass,
                               final @Named(SiteRepository.ID) Configurator repositoryConfigurator)
  {

    this.contentClass = checkNotNull(contentClass);
    this.repositoryConfigurator = checkNotNull(repositoryConfigurator);
  }

  private RepositoryKind repositoryKind;

  public ContentClass getRepositoryContentClass() {
    return contentClass;
  }

  public RepositoryKind getRepositoryKind() {
    if (repositoryKind == null) {
      repositoryKind = new DefaultRepositoryKind(SiteRepository.class, null);
    }

    return repositoryKind;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<DefaultSiteRepositoryConfiguration> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<DefaultSiteRepositoryConfiguration>()
    {
      public DefaultSiteRepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
        return new DefaultSiteRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  public Configurator getConfigurator() {
    return repositoryConfigurator;
  }

  @Override
  public void storeItem(ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException, AccessDeniedException
  {
    // collapse any '.' or '..' segments from the path
    request.pushRequestPath(normalize(request.getRequestPath()));
    try {
      super.storeItem(request, is, userAttributes);
    }
    finally {
      request.popRequestPath();
    }
  }

  @Override
  public void storeItem(boolean fromTask, StorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
  {
    // collapse any '.' or '..' segments from the path
    if (AbstractStorageItem.class.isAssignableFrom(item.getClass())) {
      String normalizedPath = normalize(item.getPath());
      AbstractStorageItem fileItem = (AbstractStorageItem) item;
      fileItem.setPath(normalizedPath);
    }

    super.storeItem(fromTask, item);
  }

  /**
   * Normalize a path.
   * Eliminates "/../" and "/./" in a string. Returns <code>null</code> if the ..'s went past the
   * root.
   * Eg:
   * <pre>
   * /foo//               -->     /foo/
   * /foo/./              -->     /foo/
   * /foo/../bar          -->     /bar
   * /foo/../bar/         -->     /bar/
   * /foo/../bar/../baz   -->     /baz
   * //foo//./bar         -->     /foo/bar
   * /../                 -->     null
   * </pre>
   *
   * @param path the path to normalize
   * @return the normalized String, or <code>null</code> if too many ..'s.
   */
  private static String normalize(final String path) {
    String normalized = path;
    // Resolve occurrences of "//" in the normalized path
    while (true) {
      int index = normalized.indexOf("//");
      if (index < 0) {
        break;
      }
      normalized = normalized.substring(0, index) + normalized.substring(index + 1);
    }

    // Resolve occurrences of "/./" in the normalized path
    while (true) {
      int index = normalized.indexOf("/./");
      if (index < 0) {
        break;
      }
      normalized = normalized.substring(0, index) + normalized.substring(index + 2);
    }

    // Resolve occurrences of "/../" in the normalized path
    while (true) {
      int index = normalized.indexOf("/../");
      if (index < 0) {
        break;
      }
      if (index == 0) {
        return null;  // Trying to go outside our context
      }
      int index2 = normalized.lastIndexOf('/', index - 1);
      normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
    }

    // Return the normalized path that we have completed
    return normalized;
  }

}
