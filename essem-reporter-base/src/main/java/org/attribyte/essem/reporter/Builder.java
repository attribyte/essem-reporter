package org.attribyte.essem.reporter;

import com.codahale.metrics.Clock;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class Builder {


   /**
    * The target URI property name ('{@value}').
    */
   public static final String URI_PROPERTY = "uri";

   /**
    * The reported host property name ('{@value}').
    */
   public static final String HOST_PROPERTY = "host";

   /**
    * The application name property ('{@value}').
    */
   public static final String APPLICATION_PROPERTY = "application";

   /**
    * The instance name property ('{@value}').
    */
   public static final String INSTANCE_PROPERTY = "instance";

   /**
    * The deflate flag property ('{@value}').
    */
   public static final String DEFLATE_PROPERTY = "deflate";

   /**
    * The description property ('{@value}').
    */
   public static final String DESCRIPTION_PROPERTY = "description";

   /**
    * The role name property ('{@value}').
    */
   public static final String ROLE_PROPERTY = "role";

   /**
    * The rate unit property ('{@value}').
    */
   public static final String RATE_UNIT_PROPERTY = "rateUnit";

   /**
    * The duration unit property {@value}.
    */
   public static final String DURATION_UNIT_PROPERTY = "durationUnit";

   /**
    * The skip unchanged metrics flag property ('{@value}').
    */
   public static final String SKIP_UNCHANGED_PROPERTY = "skipUnchanged";

   /**
    * The Basic auth username property ('{@value}').
    */
   public static final String USERNAME_PROPERTY = "username";

   /**
    * The Basic auth password property ('{@value}').
    */
   public static final String PASSWORD_PROPERTY = "password";

   /**
    * The HDR Histogram report configuration property ('{@value}').
    * <p>
    *    Allowed values NONE, SNAPSHOT (the default) or TOTAL.
    * </p>
    */
   public static final String HDR_REPORT_PROPERTY = "hdrReport";

   /**
    * Determine if the minimum required properties are available.
    * @param props The properties.
    * @return Are the minimum required available?
    */
   public static boolean hasMinimumProperties(final Properties props) {

      if(!props.containsKey(URI_PROPERTY)) {
         return false;
      }

      if(!props.containsKey(HOST_PROPERTY)) {
         return false;
      }

      return true;
   }

   /**
    * Determine if properties are valid.
    * @param props The properties.
    * @throws IllegalArgumentException if any properties are invalid.
    * @return The input properties.
    */
   public static Properties validateProperties(final Properties props) throws IllegalArgumentException, URISyntaxException {

      if(!props.containsKey(URI_PROPERTY)) {
         throw new IllegalArgumentException("A 'uri' is required");
      }

      URI uri = new URI(props.getProperty(URI_PROPERTY));

      if(!props.containsKey(HOST_PROPERTY)) {
         throw new IllegalArgumentException("A 'host' is required");
      }

      String rateUnit = props.getProperty(RATE_UNIT_PROPERTY, "").trim();
      if(!rateUnit.isEmpty()) {
         if(!rateUnit.endsWith("s")) {
            rateUnit = rateUnit + "s";
         }
         TimeUnit.valueOf(rateUnit.toUpperCase());
      }

      String durationUnit = props.getProperty(DURATION_UNIT_PROPERTY, "").trim();
      if(!durationUnit.isEmpty()) {
         if(!durationUnit.endsWith("s")) {
            durationUnit = durationUnit + "s";
         }
         TimeUnit.valueOf(durationUnit.toUpperCase());
      }

      String hdrReportStr = props.getProperty(HDR_REPORT_PROPERTY, "").trim();
      if(!hdrReportStr.isEmpty()) {
         EssemReporter.HdrReport.valueOf(hdrReportStr.toUpperCase());
      }

      return props;
   }

   /**
    * Adds values to the builder from properties.
    * @param props The properties.
    * @return The input properties.
    */
   private Properties addProperties(final Properties props) {

      String username = props.getProperty(USERNAME_PROPERTY, "").trim();
      String password = props.getProperty(PASSWORD_PROPERTY, "").trim();
      if(!username.isEmpty()) {
         withBasicAuthorization(username, password);
      }

      String host = props.getProperty(HOST_PROPERTY, "").trim();
      if(!host.isEmpty()) {
         forHost(host);
      }

      String application = props.getProperty(APPLICATION_PROPERTY, "").trim();
      if(!application.isEmpty()) {
         forApplication(application);
      }

      String instance = props.getProperty(INSTANCE_PROPERTY, "").trim();
      if(!instance.isEmpty()) {
         forInstance(instance);
      }

      String role = props.getProperty(ROLE_PROPERTY, "").trim();
      if(!role.isEmpty()) {
         withRole(role);
      }

      String description = props.getProperty(DESCRIPTION_PROPERTY, "").trim();
      if(!description.isEmpty()) {
         withDescription(role);
      }

      String rateUnit = props.getProperty(RATE_UNIT_PROPERTY, "").trim();
      if(!rateUnit.isEmpty()) {
         if(!rateUnit.endsWith("s")) {
            rateUnit = rateUnit + "s";
         }

         TimeUnit unit = TimeUnit.valueOf(rateUnit.toUpperCase());
         convertRatesTo(unit);
      }

      String durationUnit = props.getProperty(DURATION_UNIT_PROPERTY, "").trim();
      if(!durationUnit.isEmpty()) {
         if(!durationUnit.endsWith("s")) {
            durationUnit = durationUnit + "s";
         }

         TimeUnit unit = TimeUnit.valueOf(durationUnit.toUpperCase());
         convertDurationsTo(unit);
      }

      boolean deflate = props.getProperty(DEFLATE_PROPERTY, "false").equalsIgnoreCase("true");
      withDeflate(deflate);

      boolean skipUnchanged = props.getProperty(SKIP_UNCHANGED_PROPERTY, "false").equalsIgnoreCase("true");
      skipUnchangedMetrics(skipUnchanged);

      String hdrReportStr = props.getProperty(HDR_REPORT_PROPERTY, "").trim();
      if(!hdrReportStr.isEmpty()) {
         setHdrReport(EssemReporter.HdrReport.valueOf(hdrReportStr.toUpperCase()));
      }

      return props;
   }

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
    * Creates a builder from properties.
    * @param props The properties.
    * @param registry The registry to report.
    * @throws IllegalArgumentException on invalid property.
    * @throws URISyntaxException if the report URI is invalid.
    */
   protected Builder(final Properties props, final MetricRegistry registry) throws IllegalArgumentException, URISyntaxException {
      this(new URI(validateProperties(props).getProperty(URI_PROPERTY)), registry);
      addProperties(props);
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
    * Sets a role.
    * @param role The role name.
    * @return A self-reference.
    */
   public Builder withRole(final String role) {
      this.role = role;
      return this;
   }

   /**
    * Sets the description.
    * @param description The description.
    * @return A self-reference.
    */
   public Builder withDescription(final String description) {
      this.description = description;
      return this;
   }

   /**
    * Sets a supplier for status
    * @param statusSupplier The status supplier.
    * @return A self-reference.
    */
   public Builder withStatus(final Supplier<String> statusSupplier) {
      if(statusSupplier != null) {
         this.statusSupplier = statusSupplier;
      }
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
   protected String role;
   protected String description;
   protected Supplier<String> statusSupplier = () -> null;

   protected boolean deflate;
   protected TimeUnit rateUnit;
   protected TimeUnit durationUnit;
   protected boolean skipUnchangedMetrics = false;
   protected MetricFilter filter;
   protected EssemReporter.HdrReport hdrReport = EssemReporter.HdrReport.SNAPSHOT;
}
