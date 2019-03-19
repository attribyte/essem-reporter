package org.attribyte.essem.reporter;

import com.codahale.metrics.MetricRegistry;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class Proto2Builder extends Builder {

   /**
    * Creates a builder.
    * @param uri The essem endpoint URI.
    * @param registry The registry to report.
    */
   Proto2Builder(final URI uri, final MetricRegistry registry) {
      super(uri, registry);
   }

   /**
    * Creates a builder.
    * @param props The properties.
    * @param registry The registry to report.
    * @throws IllegalArgumentException if a property is invalid.
    * @throws URISyntaxException if the report URI is invalid.
    */
   Proto2Builder(final Properties props, final MetricRegistry registry) throws IllegalArgumentException, URISyntaxException {
      super(props, registry);
   }

   @Override
   public EssemReporter build() {
      return new Proto2Reporter(uri, authValue, deflate,
              registry, clock, application, host, instance, role, description, statusSupplier, filter, rateUnit, durationUnit,
              skipUnchangedMetrics, hdrReport);
   }
}