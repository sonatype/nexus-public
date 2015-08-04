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
package org.sonatype.nexus.content.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ItemNotFoundException.ItemNotFoundInRepositoryReason;
import org.sonatype.nexus.proxy.ItemNotFoundException.ItemNotFoundReason;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.attributes.internal.DefaultAttributes;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageCompositeItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsHiddenAttribute;
import org.sonatype.nexus.proxy.repository.GroupItemNotFoundException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.nexus.web.TemplateRenderer;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link ContentRenderer} using Apache Velocity.
 *
 * @since 2.8
 */
@Singleton
@Named
public class VelocityContentRenderer
    extends ComponentSupport
    implements ContentRenderer
{
  private final TemplateRenderer templateRenderer;

  private final String applicationVersion;

  @Inject
  public VelocityContentRenderer(final TemplateRenderer templateRenderer,
                                 final ApplicationStatusSource applicationStatusSource)
  {
    this.templateRenderer = checkNotNull(templateRenderer);
    this.applicationVersion = checkNotNull(applicationStatusSource).getSystemStatus().getVersion();
  }

  @Override
  public void renderCollection(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final StorageCollectionItem coll,
                               final Collection<StorageItem> children)
      throws IOException
  {
    final Set<String> uniqueNames = Sets.newHashSetWithExpectedSize(children.size());
    final List<CollectionEntry> entries = Lists.newArrayListWithCapacity(children.size());
    
    // use request URL (it does not contain any parameters) as the base URL of collection entries
    final String collUrl = BaseUrlHolder.get() + request.getServletPath() + request.getPathInfo();
    for (StorageItem child : children) {
      if (child.isVirtual() || !child.getRepositoryItemUid().getBooleanAttributeValue(IsHiddenAttribute.class)) {
        if (!uniqueNames.contains(child.getName())) {
          final boolean isCollection = child instanceof StorageCollectionItem;
          final String name = isCollection ? child.getName() + "/" : child.getName();
          final CollectionEntry entry = new CollectionEntry(name, isCollection, collUrl + name,
              new Date(child.getModified()), StorageFileItem.class.isAssignableFrom(child
              .getClass()) ? ((StorageFileItem) child).getLength() : -1, "");
          entries.add(entry);
          uniqueNames.add(child.getName());
        }
      }
    }

    Collections.sort(entries, new CollectionEntryComparator());

    final Map<String, Object> dataModel = createBaseModel();
    dataModel.put("requestPath", coll.getPath());
    dataModel.put("listItems", entries);
    templateRenderer.render(templateRenderer.template("/org/sonatype/nexus/content/internal/repositoryContentHtml.vm",
        getClass().getClassLoader()), dataModel, response);
  }

  @Override
  public void renderRequestDescription(final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       final ResourceStoreRequest resourceStoreRequest,
                                       final StorageItem item,
                                       final Exception exception)
      throws IOException
  {
    final Map<String, Object> dataModel = createBaseModel();
    dataModel.put("req", resourceStoreRequest);
    if (item != null) {
      dataModel.put("item", item);
      dataModel.put("itemContext", filterItemContext(item.getItemContext()).flatten());
      dataModel.put("itemAttributes", filterItemAttributes(item.getRepositoryItemAttributes()).asMap());
      if (item instanceof StorageCompositeItem) {
        final StorageCompositeItem compositeItem = (StorageCompositeItem) item;
        final List<String> sources = Lists.newArrayList();
        for (StorageItem source : compositeItem.getSources()) {
          if (!source.isVirtual()) {
            sources.add(source.getRepositoryItemUid().toString());
          }
          else {
            sources.add(source.getPath());
          }
        }
        dataModel.put("compositeSources", sources);
      }
    }
    dataModel.put("exception", exception);
    final Reasoning reasoning = buildReasoning(exception);
    if (reasoning != null) {
      dataModel.put("reasoning", reasoning);
    }
    templateRenderer.render(templateRenderer.template("/org/sonatype/nexus/content/internal/requestDescriptionHtml.vm",
        getClass().getClassLoader()), dataModel, response);
  }

  // ==

  private RequestContext filterItemContext(final RequestContext itemContext) {
    final RequestContext filtered = new RequestContext();
    filtered.setParentContext(itemContext);
    return filtered;
  }

  private Attributes filterItemAttributes(final Attributes itemAttributes) {
    final DefaultAttributes filtered = new DefaultAttributes();
    filtered.overlayAttributes(itemAttributes);
    final String remoteUrl = filtered.get("storageItem-remoteUrl");
    
    // strip params from secure central remote urls
    if (remoteUrl != null && remoteUrl.startsWith("https://secure.central.sonatype.com/")) {
      int qpIdx = remoteUrl.indexOf("?");
      if (qpIdx > -1) {
        filtered.setRemoteUrl(remoteUrl.substring(0, qpIdx) + "?truncated");
      }
    }
    return filtered;
  }

  private Reasoning buildReasoning(final Throwable ex) {
    if (ex instanceof ItemNotFoundException) {
      final ItemNotFoundReason reason = ((ItemNotFoundException) ex).getReason();
      if (reason instanceof ItemNotFoundInRepositoryReason) {
        return buildReasoning(((ItemNotFoundInRepositoryReason) reason).getRepository().getId(), ex);
      }
    }
    return null;
  }

  private Reasoning buildReasoning(final String repositoryId, final Throwable ex) {
    final Reasoning result = new Reasoning(repositoryId, ex.getMessage());
    if (ex instanceof GroupItemNotFoundException) {
      final GroupItemNotFoundException ginfex = (GroupItemNotFoundException) ex;
      for (Entry<Repository, Throwable> memberReason : ginfex.getMemberReasons().entrySet()) {
        result.getMembers().add(buildReasoning(memberReason.getKey().getId(), memberReason.getValue()));
      }
    }
    return result;
  }

  /**
   * This class is public only for Velocity access only, as it's used in "describe" page template to render
   * "reasoning", see methods above {@link #buildReasoning(Throwable)} and {@link #buildReasoning(String, Throwable)}.
   */
  public static class Reasoning
  {
    private final String repositoryId;

    private final String reason;

    private final List<Reasoning> members;

    public Reasoning(final String repositoryId, final String reason) {
      this.repositoryId = checkNotNull(repositoryId);
      this.reason = checkNotNull(reason);
      this.members = Lists.newArrayList();
    }

    public String getRepositoryId() {
      return repositoryId;
    }

    public String getReason() {
      return reason;
    }

    public List<Reasoning> getMembers() {
      return members;
    }
  }

  // ==

  private Map<String, Object> createBaseModel() {
    final Map<String, Object> dataModel = Maps.newHashMap();
    dataModel.put("nexusRoot", BaseUrlHolder.get());
    dataModel.put("nexusVersion", applicationVersion);
    return dataModel;
  }

  // =

  private static class CollectionEntryComparator
      implements Comparator<CollectionEntry>
  {
    @Override
    public int compare(final CollectionEntry o1, final CollectionEntry o2) {
      if (o1.isCollection()) {
        if (o2.isCollection()) {
          // 2 directories, do a path compare
          return o1.getName().compareTo(o2.getName());
        }
        else {
          // first item is a dir, second is a file, dirs always win
          return -1;
        }
      }
      else if (o2.isCollection()) {
        // first item is a file, second is a dir, dirs always win
        return 1;
      }
      else {
        // 2 files, do a path compare
        return o1.getName().compareTo(o2.getName());
      }
    }
  }

  /**
   * Entry exposed to template for rendering.
   */
  //@TemplateAccessible
  public static class CollectionEntry
  {
    private final String name;

    private final boolean collection;

    private final String resourceUri;

    private final Date lastModified;

    private final long size;

    private final String description;

    public CollectionEntry(final String name,
                           final boolean collection,
                           final String resourceUri,
                           final Date lastModified,
                           final long size,
                           final String description)
    {
      this.name = checkNotNull(name);
      this.collection = collection;
      this.resourceUri = checkNotNull(resourceUri);
      this.lastModified = checkNotNull(lastModified);
      this.size = size;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public boolean isCollection() {
      return collection;
    }

    public String getResourceUri() {
      return resourceUri;
    }

    public Date getLastModified() {
      return lastModified;
    }

    public long getSize() {
      return size;
    }

    public String getDescription() {
      return description;
    }
  }
}
