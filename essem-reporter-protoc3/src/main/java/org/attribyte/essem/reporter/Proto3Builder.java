package org.attribyte.essem.reporter;

import com.codahale.metrics.MetricRegistry;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

class Proto3Builder extends Builder {

   /**
    * Creates a builder.
    * @param uri The essem endpoint URI.
    * @param registry The registry to report.
    */
   Proto3Builder(final URI uri, final MetricRegistry registry) {
      super(uri, registry);
   }

   /**
    * Creates a builder.
    * @param props The properties.
    * @param registry The registry to report.
    * @throws IllegalArgumentException if a property is invalid.
    * @throws URISyntaxException if the report URI is invalid.
    */
   Proto3Builder(final Properties props, final MetricRegistry registry) throws IllegalArgumentException, URISyntaxException {
      super(props, registry);
   }

   @Override
   public EssemReporter build() {
      return new Proto3Reporter(uri, authValue, deflate,
              registry, clock, application, host, instance, role, description, statusSupplier, filter, rateUnit, durationUnit,
              skipUnchangedMetrics, hdrReport);
   }

   /**
    * Creates a proto3 compatible report builder from properties.
    * @param props The properties.
    * @param registry The registry.
    * @return The builder.
    * @throws IllegalArgumentException if a property is invalid.
    * @throws URISyntaxException if the report URI is invalid.
    */
   public static Builder createBuilder(final Properties props, final MetricRegistry registry) throws IllegalArgumentException, URISyntaxException {
      return new Proto3Builder(props, registry);
   }

   /**
    * Creates a proto3 compatible report builder for a URI.
    * @param uri The URI.
    * @param registry The registry.
    * @return The builder.
    */
   public static Builder createBuilder(final URI uri, final MetricRegistry registry) {
      return new Proto3Builder(uri, registry);
   }
}
