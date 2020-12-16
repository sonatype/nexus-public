package org.sonatype.nexus.coreui.internal.blobstore;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;

@Named
@Singleton
public class BlobStoreStateContributor
    extends ComponentSupport
    implements StateContributor
{
  private final Map<String, Object> state;

  @Inject
  public BlobStoreStateContributor(@Named("${nexus.react.blobstores:-false}") Boolean featureFlag) {
    state = ImmutableMap.of("nexus.react.blobstores", featureFlag);
  }

  @Override
  public Map<String, Object> getState() {
    return state;
  }
}
