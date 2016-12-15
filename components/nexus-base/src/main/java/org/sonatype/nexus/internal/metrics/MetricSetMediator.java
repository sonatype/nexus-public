package org.sonatype.nexus.internal.metrics;

import javax.inject.Named;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.sonatype.goodies.common.ComponentSupport;

/**
 * Manages {@link MetricSet} registrations via Sisu component mediation.
 *
 * @since 3.2
 */
@Named
public class MetricSetMediator
    extends ComponentSupport
    implements Mediator<Named, MetricSet, MetricRegistry>
{
  // FIXME: Sort out out if there is maybe a more sane way to register/unregister w/o overloading sisu-@Named value for metric name?

  public void add(final BeanEntry<Named, MetricSet> entry, final MetricRegistry registry) throws Exception {
    log.debug("Registering: {}", entry);
    registry.register(entry.getKey().value(), entry.getValue());
  }

  public void remove(final BeanEntry<Named, MetricSet> entry, final MetricRegistry registry) throws Exception {
    log.debug("Un-registering: {}", entry);
    registry.remove(entry.getKey().value());
  }
}

