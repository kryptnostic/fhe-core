package com.kryptnostic.test.metrics;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Optional;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurer;

@Configuration
@EnableMetrics(proxyTargetClass=true)
public class MetricsConfiguration implements MetricsConfigurer {
    private static final Optional<Graphite> graphite; 
    private static final Logger logger = LoggerFactory.getLogger( MetricsConfiguration.class );
    private static final String GRAPHITE_SERVER_VAR = "graphite_server";
    private static final String GRAPHITE_PORT_VAR = "graphite_port";
    private static final String GRAPHITE_SERVER;
    private static final int GRAPHITE_PORT;
    private static final MetricRegistry singletonMetricRegistry = new NameCorrectingMetricRegistry();  
    private static final HealthCheckRegistry singletonHealthCheckRegistry = new HealthCheckRegistry();
    private static final ScheduledReporter reporter;

    static {
        GRAPHITE_SERVER  = System.getenv( GRAPHITE_SERVER_VAR) ;
        GRAPHITE_PORT = Integer.parseInt( Optional.fromNullable( System.getenv( GRAPHITE_PORT_VAR ) ).or( "2003" ) );

        if( GRAPHITE_SERVER == null )  {
            graphite = Optional.absent();
        } else {
            graphite = Optional.of( new Graphite( new InetSocketAddress( GRAPHITE_SERVER , GRAPHITE_PORT ) ) );
        }


        if( graphite.isPresent() ) {
            reporter = 
                    GraphiteReporter
                    .forRegistry( singletonMetricRegistry )
                    .prefixedWith( getHostName() )
                    .convertDurationsTo( TimeUnit.MILLISECONDS )
                    .convertRatesTo( TimeUnit.MILLISECONDS )
                    .build( graphite.get() );
            reporter.start( 5 , TimeUnit.SECONDS );
            logger.info("Address = {}:{}" , GRAPHITE_SERVER , GRAPHITE_PORT );

        } else {
            reporter = 
                    ConsoleReporter
                    .forRegistry( singletonMetricRegistry )
                    .convertDurationsTo( TimeUnit.MILLISECONDS )
                    .convertRatesTo( TimeUnit.MILLISECONDS )
                    .build();
        }
    }

    @Override
    public void configureReporters(MetricRegistry metricRegistry) {}

    @Bean
    public ScheduledReporter reporter() {
        return reporter;
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        return singletonMetricRegistry;
    }

    @Override
    public HealthCheckRegistry getHealthCheckRegistry() {
        return singletonHealthCheckRegistry;
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            logger.error( "Unable to resolve host name, defaulting to empty prefix." );
            return "";
        }
    }

    private static class NameCorrectingMetricRegistry extends MetricRegistry {
        @Override
        public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
            String cleanName = cleanupName( name );
            Map<String, Metric> existingMetrics = getMetrics();
            int i = 0;
            String freeName = cleanName;
            while( existingMetrics.containsKey( freeName ) ) {
                freeName = cleanName + Integer.toString( i++ );
            }
            return super.register( freeName , metric );
        }

        private static String cleanupName( String name ) {
            int endIndex =  name.indexOf( "$" );
            int periodStart = name.indexOf( "." , endIndex );

            if( endIndex > 0  ) {
                return name.substring( 0 , endIndex ) + name.substring( periodStart );
            } else { 
                return name;
            }
        }
    }
}
