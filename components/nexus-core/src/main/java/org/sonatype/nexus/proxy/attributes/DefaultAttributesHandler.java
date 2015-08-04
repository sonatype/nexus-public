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
package org.sonatype.nexus.proxy.attributes;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsMetadataMaintainedAttribute;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default implementation of AttributesHandler. Does not have any assumption regarding actual AttributeStorage it
 * uses. It uses {@link StorageItemInspector} and {@link StorageFileItemInspector} components for "expansion" of core
 * (and custom) attributes (those components might come from plugins too). This class also implements some
 * "optimizations" for attribute "lastRequested", by using coarser resolution for it (saving it very n-th hour or so).
 *
 * @author cstamas
 */
@Named
@Singleton
public class DefaultAttributesHandler
    extends ComponentSupport
    implements AttributesHandler
{

  /**
   * Default value of lastRequested attribute updates resolution: 12h
   */
  private static final long LAST_REQUESTED_ATTRIBUTE_RESOLUTION_DEFAULT = 43200000L;

  /**
   * The value of lastRequested attribute updates resolution. Is enforced to be positive long. Setting it to 0 makes
   * Nexus behave in "old" (update always) way.
   */
  private static final long LAST_REQUESTED_ATTRIBUTE_RESOLUTION = Math.abs(SystemPropertiesHelper.getLong(
      "org.sonatype.nexus.proxy.attributes.DefaultAttributesHandler.lastRequested.resolution",
      LAST_REQUESTED_ATTRIBUTE_RESOLUTION_DEFAULT));

  /**
   * Flag to completely enable/disable the lastRequested attribute maintenance.
   */
  private static final boolean LAST_REQUEST_ATTRIBUTE_ENABLED = SystemPropertiesHelper.getBoolean(
      "org.sonatype.nexus.proxy.attributes.DefaultAttributesHandler.lastRequested.enabled", true);

  /**
   * Flag to completely enable/disable the lastRequested attribute maintenance for hosted repositories.
   */
  private static final boolean LAST_REQUEST_ATTRIBUTE_ENABLED_FOR_HOSTED = SystemPropertiesHelper.getBoolean(
      "org.sonatype.nexus.proxy.attributes.DefaultAttributesHandler.lastRequested.enabled.hosted",
      LAST_REQUEST_ATTRIBUTE_ENABLED);

  /**
   * Flag to completely enable/disable the lastRequested attribute maintenance for proxy repositories.
   */
  private static final boolean LAST_REQUEST_ATTRIBUTE_ENABLED_FOR_PROXY = SystemPropertiesHelper.getBoolean(
      "org.sonatype.nexus.proxy.attributes.DefaultAttributesHandler.lastRequested.enabled.proxy",
      LAST_REQUEST_ATTRIBUTE_ENABLED);

  /**
   * The actual value of lastRequest attribute's resolution. Note: is not final due to UT access, see
   * setter method that is visible for testing.
   */
  private long lastRequestedResolution = LAST_REQUESTED_ATTRIBUTE_RESOLUTION;

  /**
   * The attribute storage.
   */
  private final AttributeStorage attributeStorage;

  /**
   * The item inspector list.
   */
  private final List<StorageItemInspector> itemInspectorList;

  @Inject
  public DefaultAttributesHandler(@Named("ls") AttributeStorage attributeStorage,
                                  List<StorageItemInspector> itemInspectorList)
  {
    this.attributeStorage = checkNotNull(attributeStorage);
    this.itemInspectorList = checkNotNull(itemInspectorList);
  }

  // ==

  /**
   * Gets the attribute storage.
   *
   * @return the attribute storage
   */
  public AttributeStorage getAttributeStorage() {
    return attributeStorage;
  }

  /**
   * Gets the item inspector list.
   *
   * @return the item inspector list
   */
  public List<StorageItemInspector> getItemInspectorList() {
    return itemInspectorList;
  }

  // ======================================================================
  // AttributesHandler iface

  @Override
  public boolean deleteAttributes(final RepositoryItemUid uid)
      throws IOException
  {
    if (!isMetadataMaintained(uid)) {
      return false;
    }
    else {
      return getAttributeStorage().deleteAttributes(uid);
    }
  }

  @Override
  public void fetchAttributes(final StorageItem item)
      throws IOException
  {
    if (!isMetadataMaintained(item)) {
      return;
    }

    final Attributes attributes = getAttributeStorage().getAttributes(item.getRepositoryItemUid());

    if (attributes != null) {
      item.getRepositoryItemAttributes().overlayAttributes(attributes);
    }
    else {
      // we are fixing md if we can
      ContentLocator is = null;
      if (item instanceof StorageFileItem &&
          ((StorageFileItem) item).getContentLocator().isReusable()) {
        is = ((StorageFileItem) item).getContentLocator();
      }

      storeAttributes(item, is);
    }
  }

  @Override
  public void storeAttributes(final StorageItem item)
      throws IOException
  {
    if (!isMetadataMaintained(item)) {
      return;
    }

    getAttributeStorage().putAttributes(item.getRepositoryItemUid(), item.getRepositoryItemAttributes());
  }

  @Override
  public void storeAttributes(final StorageItem item, final ContentLocator content)
      throws IOException
  {
    if (!isMetadataMaintained(item)) {
      return;
    }

    if (content != null) {
      // resetting some important values
      if (item.getRemoteChecked() == 0) {
        item.setRemoteChecked(System.currentTimeMillis());
      }

      if (item.getLastRequested() == 0) {
        item.setLastRequested(System.currentTimeMillis());
      }

      item.setExpired(false);

      // resetting the pluggable attributes
      expandCustomItemAttributes(item, content);
    }

    storeAttributes(item);
  }

  @Override
  public void touchItemCheckedRemotely(final long timestamp, final StorageItem storageItem)
      throws IOException
  {
    if (!isMetadataMaintained(storageItem)) {
      return;
    }

    final RepositoryItemUid uid = storageItem.getRepositoryItemUid();
    final Attributes attributes = getAttributeStorage().getAttributes(uid);

    if (attributes != null) {
      attributes.setRepositoryId(uid.getRepository().getId());
      attributes.setPath(uid.getPath());
      attributes.setCheckedRemotely(timestamp);
      attributes.setExpired(false);

      getAttributeStorage().putAttributes(uid, attributes);
    }
  }

  @Override
  public void touchItemLastRequested(final long timestamp, final StorageItem storageItem)
      throws IOException
  {
    if (!isMetadataMaintained(storageItem)) {
      return;
    }

    touchItemLastRequested(timestamp, storageItem.getResourceStoreRequest(), storageItem.getRepositoryItemUid(),
        storageItem.getRepositoryItemAttributes());
  }

  // ======================================================================
  // Internal

  protected boolean isMetadataMaintained(final StorageItem item) {
    if (item instanceof StorageCollectionItem) {
      // not storing attributes of directories
      return false;
    }
    else if (item.isVirtual()) {
      // virtual items have no attributes (nor UID for that matter)
      return false;
    }
    else {
      return isMetadataMaintained(item.getRepositoryItemUid());
    }
  }

  protected boolean isMetadataMaintained(final RepositoryItemUid uid) {
    final Boolean isMetadataMaintained = uid.getAttributeValue(IsMetadataMaintainedAttribute.class);
    if (isMetadataMaintained != null) {
      return isMetadataMaintained.booleanValue();
    }
    else {
      // safest
      return true;
    }
  }

  protected void touchItemLastRequested(final long timestamp, final ResourceStoreRequest request,
                                        final RepositoryItemUid uid, final Attributes attributes)
      throws IOException
  {
    // Touch it only if this is user-originated request (request incoming over HTTP, not a plugin or "internal" one)
    // Currently, we test for IP address presence, since that makes sure it is user request (from REST API) and not
    // a request from "internals" (ie. a running task).

    // we do this only for requests originating from REST API (user initiated)
    if (request.getRequestContext().containsKey(AccessManager.REQUEST_REMOTE_ADDRESS)) {
      // if we need to do this at all... user might turn this feature completely off
      if (isTouchLastRequestedEnabled(uid.getRepository())) {
        final long diff = timestamp - attributes.getLastRequested();

        // if timestamp < storageItem.getLastRequested() => diff will be negative => DO THE UPDATE
        // ie. programatically "resetting" lastAccessTime to some past point for whatever reason
        // if timestamp == to storageItem.getLastRequested() => diff will be 0 => SKIP THE UPDATE
        // ie. trying to set to same value, just lessen the needless IO since values are already equal
        // if timestamp > storageItem.getLastRequested() => diff will be positive => DO THE UPDATE IF diff
        // bigger
        // than resolution
        // ie. the "usual" case, obey the resolution then
        if (diff < 0 || ((diff > 0) && (diff > lastRequestedResolution))) {
          attributes.setLastRequested(timestamp);

          getAttributeStorage().putAttributes(uid, attributes);
        }
      }
    }
  }

  protected boolean isTouchLastRequestedEnabled(final Repository repository)
      throws IOException
  {
    // the "default"
    boolean doTouch = LAST_REQUEST_ATTRIBUTE_ENABLED;

    final RepositoryKind repositoryKind = repository.getRepositoryKind();

    if (repositoryKind != null) {
      if (repositoryKind.isFacetAvailable(HostedRepository.class)) {
        // this is a hosted repository
        doTouch = LAST_REQUEST_ATTRIBUTE_ENABLED_FOR_HOSTED;
      }
      else if (repositoryKind.isFacetAvailable(ProxyRepository.class)) {
        // this is a proxy repository
        doTouch = LAST_REQUEST_ATTRIBUTE_ENABLED_FOR_PROXY;
      }
    }

    return doTouch;
  }

  /**
   * Method that expands core (and custom if custom inspector exists, like provided by a plugin) item attributes on
   * files having attributes maintained. This method may be called ONLY if {@link #isMetadataMaintained(StorageItem)}
   * returns {@code true} and the content locator passed in is reusable. Currently this method is invoked only from
   * {@link #storeAttributes(StorageItem, ContentLocator)} method.
   *
   * @param item    the item
   * @param content the reusable content locator
   */
  protected void expandCustomItemAttributes(final StorageItem item, final ContentLocator content) {
    for (StorageItemInspector inspector : getItemInspectorList()) {
      if (inspector.isHandled(item)) {
        try {
          inspector.processStorageItem(item);
        }
        catch (Exception ex) {
          log.warn(
              "Inspector {} throw exception during inspection of {}, continuing...", inspector.getClass(),
              item.getRepositoryItemUid(), ex);
        }
      }
    }
  }

  // ==

}
