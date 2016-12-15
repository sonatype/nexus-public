package org.sonatype.nexus.graphite.internal

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.net.SocketFactory

import org.sonatype.goodies.common.Time
import org.sonatype.goodies.i18n.I18N
import org.sonatype.goodies.i18n.MessageBundle
import org.sonatype.goodies.i18n.MessageBundle.DefaultMessage
import org.sonatype.nexus.capability.CapabilityConfigurationSupport
import org.sonatype.nexus.capability.CapabilityDescriptorSupport
import org.sonatype.nexus.capability.CapabilitySupport
import org.sonatype.nexus.capability.CapabilityType
import org.sonatype.nexus.capability.Capability
import org.sonatype.nexus.capability.Condition
import org.sonatype.nexus.common.template.TemplateParameters
import org.sonatype.nexus.formfields.FormField
import org.sonatype.nexus.formfields.StringTextFormField

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter
import com.codahale.metrics.graphite.GraphiteSender
import com.codahale.metrics.graphite.GraphiteUDP
import com.codahale.metrics.graphite.PickledGraphite
import com.google.common.base.Charsets
import com.google.common.base.Supplier
import groovy.transform.PackageScope
import groovy.transform.ToString
import org.codehaus.plexus.interpolation.MapBasedValueSource
import org.codehaus.plexus.interpolation.StringSearchInterpolator

import static com.google.common.base.Preconditions.checkArgument
import static org.sonatype.nexus.capability.CapabilityType.capabilityType

/**
 * Graphite Reporter {@link Capability}.
 *
 * @since 3.2
 */
@Named(GraphiteReporterCapability.TYPE_ID)
class GraphiteReporterCapability
  extends CapabilitySupport<Configuration> {
  public static final String TYPE_ID = 'graphite.reporter'

  public static final CapabilityType TYPE = capabilityType(TYPE_ID)

  private static interface Messages
    extends MessageBundle
  {
    @DefaultMessage('Graphite Reporter')
    String name()

    @DefaultMessage('Server')
    String serverLabel()

    @DefaultMessage('URI of Graphite server to report to; ie: tcp://hostname:port; supported schemes: tcp, udp, tcp-pickle')
    String serverHelp()

    @DefaultMessage('Prefix')
    String prefixLabel()

    @DefaultMessage('Prefix for reported metrics data; ie: servers.${host-name}.dropwizard')
    String prefixHelp()

    @DefaultMessage('Frequency')
    String frequencyLabel()

    @DefaultMessage('How often metrics data should be reported; ie: 1min')
    String frequencyHelp()

    @DefaultMessage('%s every %s')
    String description(String server, Time frequency)
  }

  @PackageScope
  static final Messages messages = I18N.create(Messages.class)

  @Inject
  MetricRegistry metricsRegistry

  @Nullable
  GraphiteSender graphiteSender

  @Nullable
  GraphiteReporter graphiteReporter

  @Nullable
  String prefix

  @Override
  protected Configuration createConfig(final Map<String, String> properties) {
    return new Configuration(properties)
  }

  @Override
  protected String renderDescription() {
    return messages.description(config.server as String, config.frequency)
  }

  @Override
  Condition activationCondition() {
    return conditions().capabilities().passivateCapabilityDuringUpdate()
  }

  @Override
  protected void onActivate(final Configuration config) {
    log.info("Reporting to: {}", config.server)

    prefix = interpolate(config.prefix)
    log.info("Using prefix: $prefix")

    graphiteSender = createSender(config.server)
    log.info('Using sender: {}', graphiteSender)

    // FIXME: Sort out metric name format/normalization; impl is not presently flexible to customize with overrides :-(

    graphiteReporter = GraphiteReporter.forRegistry(metricsRegistry)
      .prefixedWith(prefix)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphiteSender)

    graphiteReporter.start(config.frequency.value(), config.frequency.unit())
  }

  /**
   * Create a sender from the given URI.
   */
  private static GraphiteSender createSender(final URI uri) {
    // parse URI query string into map of parameters
    Map<String,String> params = [:]
    if (uri.query) {
      uri.query.split('&').each { kv ->
        def (key, value) = kv.split('=', 2)
        params[key] = value
      }
    }

    // decode optional charset
    def charset = Charsets.UTF_8
    params['charset']?.with { charset = Charset.forName(it) }

    switch (uri.scheme) {
      case 'tcp':
        return new Graphite(uri.host, uri.port, SocketFactory.default, charset)

      case 'udp':
        return new GraphiteUDP(uri.host, uri.port)

      case 'tcp-pickle':
        // decode optional batchSize
        int batchSize = 100
        params['batchSize']?.with { batchSize = it as int }
        return new PickledGraphite(uri.host, uri.port, SocketFactory.default, charset, batchSize)
    }
    throw new RuntimeException("Unsupported scheme: ${uri.scheme}")
  }

  /**
   * Supplier of 'prefix' replacement tokens.
   */
  private static Supplier<Map<String,Object>> tokens = new Supplier<Map<String, Object>>() {
    @Override
    Map<String, Object> get() {
      def localHost = InetAddress.localHost
      def hostName = localHost.hostName
      def canonicalHostName = localHost.canonicalHostName

      return [
        'hostName': hostName,
        'host-name': hostName.replace('.', '-'),
        'host_name': hostName.replace('.', '_'),
        'canonicalHostName': canonicalHostName,
        'canonical-host-name': canonicalHostName.replace('.', '-'),
        'canonical_host_name': canonicalHostName.replace('.', '_')
      ]
    }
  }

  /**
   * Helper to interpolate given input.
   */
  private static String interpolate(final String input) {
    def interpolator = new StringSearchInterpolator()
    interpolator.addValueSource(new MapBasedValueSource(tokens.get()))
    return interpolator.interpolate(input)
  }

  @Override
  protected void onPassivate(final Configuration config) {
    try {
      graphiteReporter?.stop()
    }
    catch (Exception e) {
      log.warn('Failed to stop reporter; ignoring', e)
    }
    graphiteReporter = null

    try {
      graphiteSender?.close()
    }
    catch (Exception e) {
      log.warn('Failed to stop sender; ignoring', e)
    }
    graphiteSender = null

    prefix = null
  }

  @Override
  protected String renderStatus() throws Exception {
    if (context().enabled) {
      return render("${TYPE_ID}-status.vm", [
        prefix: prefix,
        graphiteSender: graphiteSender,
        graphiteReporter: graphiteReporter
      ])
    }
    return null
  }

  //
  // Configuration
  //

  private static final String P_SERVER = 'server'

  /**
   * Supported configuration schemes.
   *
   * Keep in sync with {@link Messages#serverHelp} and {@link #createSender}
   */
  private static final List<String> SCHEMES = [ 'tcp', 'udp', 'tcp-pickle' ]

  private static final String P_PREFIX = 'prefix'

  private static final String P_FREQUENCY = 'frequency'

  // TODO: filter predicate || all

  @ToString(includePackage = false, includeNames = true)
  static class Configuration
    extends CapabilityConfigurationSupport
  {
    URI server

    String prefix

    Time frequency

    // FIXME: Sort out how to properly validate this configuration to expose this information the the UI
    // FIXME: ... appears ASIS this is not functioning as it did in NX2?

    Configuration(final Map<String, String> properties) {
      server = parseUri(properties[P_SERVER])
      checkArgument(server.scheme in SCHEMES, 'Server scheme must be one of %s; not: %s', SCHEMES, server.scheme)
      // sanity check that host and port are present; and not misinterpreted by missing scheme
      checkArgument(server.host != null, 'Missing server hostname')
      checkArgument(server.port != -1, 'Missing serer port')
      prefix = properties[P_PREFIX]
      frequency = Time.parse(properties[P_FREQUENCY])
    }
  }

  //
  // Descriptor
  //

  @Named(GraphiteReporterCapability.TYPE_ID)
  @Singleton
  static class Descriptor
    extends CapabilityDescriptorSupport
  {
    private final FormField server

    private final FormField prefix

    private final FormField frequency

    Descriptor() {
      this.exposed = true
      this.hidden = false

      this.server = new StringTextFormField(
        P_SERVER,
        messages.serverLabel(),
        messages.serverHelp(),
        FormField.MANDATORY
      )

      this.prefix = new StringTextFormField(
        P_PREFIX,
        messages.prefixLabel(),
        messages.prefixHelp(),
        FormField.MANDATORY
      )

      this.frequency = new StringTextFormField(
        P_FREQUENCY,
        messages.frequencyLabel(),
        messages.frequencyHelp(),
        FormField.MANDATORY
      )
    }

    @Override
    CapabilityType type() {
      return TYPE
    }

    @Override
    String name() {
      return messages.name()
    }

    @Override
    List<FormField> formFields() {
      return [server, prefix, frequency]
    }

    @Override
    protected String renderAbout() {
      // include tokens to show real values
      return render("$TYPE_ID-about.vm", new TemplateParameters(tokens.get()))
    }
  }
}
