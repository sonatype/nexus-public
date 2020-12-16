package org.sonatype.nexus.coreui.internal.blobstore;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.coreui.internal.blobstore.BlobStoreUIResource.RESOURCE_PATH;

/**
 * @since 3.next
 */
@Named
@Singleton
@Path(RESOURCE_PATH)
public class BlobStoreUIResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "/internal/ui/blobstores";

  private final BlobStoreManager blobStoreManager;

  @Inject
  public BlobStoreUIResource(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  public List<InternalBlobStoreApiResponse> listBlobStores() {
    return stream(blobStoreManager.browse())
        .map(InternalBlobStoreApiResponse::new)
        .collect(toList());
  }
}


