package org.attribyte.essem.reporter;

import com.codahale.metrics.MetricRegistry;

import java.net.URI;

class Proto3Builder extends Builder {

   /**
    * Creates a builder.
    * @param uri The essem endpoint URI.
    * @param registry The registry to report.
    */
   Proto3Builder(final URI uri, final MetricRegistry registry) {
      super(uri, registry);
   }

   @Override
   public EssemReporter build() {
      return new Proto3Reporter(uri, authValue, deflate,
              registry, clock, application, host, instance, role, description, statusSupplier, filter, rateUnit, durationUnit,
              skipUnchangedMetrics, hdrReport);
   }
}
