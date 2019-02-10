package org.attribyte.essem.reporter;

import com.codahale.metrics.Clock;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public abstract class Builder {

   /**
    * Creates a builder.
    * @param uri The essem endpoint URI.
    * @param registry The registry to report.
    */
   protected Builder(final URI uri, final MetricRegistry registry) {
      this.uri = uri;
      this.registry = registry;
      this.clock = Clock.defaultClock();
      this.rateUnit = TimeUnit.SECONDS;
      this.durationUnit = TimeUnit.MILLISECONDS;
      this.filter = MetricFilter.ALL;
      this.application = null;
      this.host = null;
      this.instance = null;
   }

   /**
    * Configures the clock.
    * @param clock The clock.
    * @return A self-reference.
    */
   public Builder withClock(final Clock clock) {
      this.clock = clock;
      return this;
   }

   /**
    * Sets the application name.
    * @param application The application name.
    * @return A self-reference.
    */
   public Builder forApplication(final String application) {
      this.application = application;
      return this;
   }

   /**
    * Sets the host.
    * @param host The host.
    * @return A self-reference.
    */
   public Builder forHost(final String host) {
      this.host = host;
      return this;
   }

   /**
    * Sets the instance name.
    * @param instance The instance name.
    * @return A self-reference.
    */
   public Builder forInstance(final String instance) {
      this.instance = instance;
      return this;
   }

   /**
    * Configures the rate conversion. Default is seconds.
    * @param rateUnit The rate unit.
    * @return A self-reference.
    */
   public Builder convertRatesTo(final TimeUnit rateUnit) {
      this.rateUnit = rateUnit;
      return this;
   }

   /**
    * Configures the duration conversion. Default is milliseconds.
    * @param durationUnit The duration unit.
    * @return A self-reference.
    */
   public Builder convertDurationsTo(final TimeUnit durationUnit) {
      this.durationUnit = durationUnit;
      return this;
   }

   /**
    * Applies a filter to the registry before reporting.
    * @param filter The filter.
    * @return A self-reference.
    */
   public Builder filter(final MetricFilter filter) {
      this.filter = filter;
      return this;
   }

   /**
    * Sets a value to be sent as the value of the <code>Authorization</code> header.
    * @param authValue The authorization header value.
    * @return A self-reference.
    */
   public Builder withAuthorization(final String authValue) {
      this.authValue = authValue;
      return this;
   }

   /**
    * Configures HTTP Basic auth.
    * @param username The username.
    * @param password The password.
    * @return A self-reference.
    */
   public Builder withBasicAuthorization(final String username, final String password) {
      if(username != null && password != null) {
         byte[] up = (username + ":" + password).getBytes(Charsets.UTF_8);
         return withAuthorization("Basic " + BaseEncoding.base64().encode(up));
      } else {
         return this;
      }
   }

   /**
    * Configures 'deflate' of sent reports.
    * @param deflate Should deflate be used?
    * @return A self-reference.
    */
   public Builder withDeflate(final boolean deflate) {
      this.deflate = deflate;
      return this;
   }

   /**
    * Configures the reporter to skip reports for metrics unchanged
    * since the last report. Default is 'true'.
    * @param skipUnchangedMetrics Should unchanged metrics be skipped?
    * @return A self-reference.
    */
   public Builder skipUnchangedMetrics(final boolean skipUnchangedMetrics) {
      this.skipUnchangedMetrics = skipUnchangedMetrics;
      return this;
   }

   /**
    * Sets the HDR histogram report mode. Default is {@code SNAPSHOT}.
    * @param hdrReport The HDR report mode.
    * @return A self-reference.
    */
   public Builder setHdrReport(final EssemReporter.HdrReport hdrReport) {
      this.hdrReport = hdrReport;
      return this;
   }

   /**
    * Builds a reporter instance.
    * @return The reporter.
    */
   public abstract EssemReporter build();

   protected final URI uri;
   protected final MetricRegistry registry;
   protected String authValue;

   protected Clock clock;
   protected String application;
   protected String host;
   protected String instance;
   protected boolean deflate;
   protected TimeUnit rateUnit;
   protected TimeUnit durationUnit;
   protected boolean skipUnchangedMetrics = false;
   protected MetricFilter filter;
   protected EssemReporter.HdrReport hdrReport = EssemReporter.HdrReport.SNAPSHOT;
}
