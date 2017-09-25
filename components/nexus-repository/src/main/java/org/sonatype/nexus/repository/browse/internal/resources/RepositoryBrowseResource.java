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
package org.sonatype.nexus.repository.browse.internal.resources;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.encoding.EncodingUtil;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.internal.model.BrowseListItem;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.BrowseNode;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * @since 3.6
 */
@Named
@Singleton
@Path(RepositoryBrowseResource.RESOURCE_URI)
@Produces(TEXT_HTML)
public class RepositoryBrowseResource
    extends ComponentSupport
    implements Resource
{
  private static final String REPOSITORY_PATH_SEGMENT = "{noop: (/)?}{repositoryPath: ((?<=/).*)?}";

  public static final String RESOURCE_URI = "/repository/browse/{repositoryName}" + REPOSITORY_PATH_SEGMENT;

  private static final String TEMPLATE_RESOURCE = "browseContentHtml.vm";

  private final RepositoryManager repositoryManager;

  private final BrowseNodeStore browseNodeStore;

  private final TemplateHelper templateHelper;

  private final SecurityHelper securityHelper;

  private final URL template;

  @Inject
  public RepositoryBrowseResource(final RepositoryManager repositoryManager,
                                  final BrowseNodeStore browseNodeStore,
                                  final TemplateHelper templateHelper,
                                  final SecurityHelper securityHelper)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.browseNodeStore = checkNotNull(browseNodeStore);
    this.templateHelper = checkNotNull(templateHelper);
    this.securityHelper = checkNotNull(securityHelper);
    this.template = getClass().getResource(TEMPLATE_RESOURCE);
    checkNotNull(template);
  }

  @GET
  public Response getHtml(@PathParam("repositoryName") final String repositoryName,
                          @PathParam("repositoryPath") final String repositoryPath, @Context final UriInfo uriInfo)
  {
    log.debug("Get HTML directory listing for repository {} on path {}", repositoryName, repositoryPath);

    if (!uriInfo.getAbsolutePath().toString().endsWith("/")) {
      log.debug("Request does include a trailing slash, redirecting to include it");
      return Response.seeOther(UriBuilder.fromUri(uriInfo.getAbsolutePath()).path("/").build()).build();
    }

    Repository repository = repositoryManager.get(repositoryName);

    if (repository == null || !securityHelper.allPermitted(new RepositoryViewPermission(repository, BROWSE))) {
      log.debug("Requested repository could not be located or user does not have permission: {} ",repositoryName);
      throw new WebApplicationException("Repository not found", NOT_FOUND);
    }

    List<String> pathSegments = new ArrayList<>();

    if (!isRoot(repositoryPath)) {
      pathSegments = asList(EncodingUtil.urlDecode(repositoryPath.split("/")));
    }

    Iterable<BrowseNode> browseNodes = browseNodeStore.getChildrenByPath(repository, pathSegments, null);

    if (browseNodes == null) {
      log.debug("Requested path {} could not be located in repository {}", repositoryPath, repositoryName);
      throw new WebApplicationException("Path not found", NOT_FOUND);
    }

    return Response.ok(templateHelper.render(template,
        initializeTemplateParameters(repositoryPath, toListItems(browseNodes, repository, repositoryPath)))).build();
  }

  private List<BrowseListItem> toListItems(final Iterable<BrowseNode> browseNodes, final Repository repository, final String path) {
    List<BrowseListItem> listItems = new ArrayList<>();

    if (browseNodes != null) {
      for (BrowseNode browseNode : sort(browseNodes)) {
        String size = null;
        String lastModified = null;
        String listItemPath;
        if (browseNode.getAssetId() != null) {
          Asset asset = Transactional.operation.withDb(repository.facet(StorageFacet.class).txSupplier()).call(() -> {
            StorageTx tx = UnitOfWork.currentTx();
            return tx.findAsset(browseNode.getAssetId(), tx.findBucket(repository));
          });

          if (asset == null) {
            log.error("Could not find expected asset (id): {}/{} ({}) in repository: {}", path, browseNode.getPath(),
                browseNode.getAssetId().toString(), repository.getName());
            //something bad going on here, move along to the next
            continue;
          }

          size = String.valueOf(asset.size());
          lastModified = String.valueOf(asset.blobUpdated());
          listItemPath = getListItemPath(repository, browseNode, asset);
        }
        else {
          listItemPath = getListItemPath(repository, browseNode, null);
        }

        listItems.add(
            new BrowseListItem(listItemPath, browseNode.getPath(), browseNode.getAssetId() == null, lastModified, size,
                ""));
      }
    }

    return listItems;
  }

  private Iterable<BrowseNode> sort(final Iterable<BrowseNode> nodes) {
    List<BrowseNode> sortedBrowseNodes = new ArrayList<>();
    nodes.forEach(sortedBrowseNodes::add);

    sortedBrowseNodes.sort((o1, o2) -> {
      if (o1.getAssetId() == null && o2.getAssetId() != null) {
        return -1;
      }
      else if (o2.getAssetId() == null && o1.getAssetId() != null) {
        return 1;
      }
      return Strings2.lower(o1.getPath()).compareTo(Strings2.lower(o2.getPath()));
    });

    return sortedBrowseNodes;
  }

  private TemplateParameters initializeTemplateParameters(final String path, final List<BrowseListItem> listItems) {
    TemplateParameters templateParameters = templateHelper.parameters();

    if (isRoot(path)) {
      templateParameters.set("root", true);
    }

    templateParameters.set("requestPath", "/" + path);
    templateParameters.set("listItems", listItems);

    return templateParameters;
  }

  private String getListItemPath(final Repository repository, final BrowseNode browseNode, final Asset asset) {
    if (asset == null) {
      return EncodingUtil.urlEncode(browseNode.getPath()) + "/";
    }

    return repository.getUrl() + "/" + asset.name();
  }

  private boolean isRoot(final String path) {
    return Strings2.isBlank(path);
  }
}
