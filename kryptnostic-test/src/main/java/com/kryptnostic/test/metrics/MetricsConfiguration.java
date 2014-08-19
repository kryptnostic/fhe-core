package com.kryptnostic.test.metrics;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Optional;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurer;

@Configuration
@EnableMetrics(proxyTargetClass=true)
public class MetricsConfiguration implements MetricsConfigurer {
	//TODO: Make this configurable instead of being hardcoded to our internal graphite server.
	private static final Optional<Graphite> graphite; 
	private static final Logger logger = LoggerFactory.getLogger( MetricsConfiguration.class );
	private static final String GRAPHITE_SERVER_VAR = "graphite_server";
	private static final String GRAPHITE_PORT_VAR = "graphite_port";
	private static final String GRAPHITE_SERVER;
	private static final int GRAPHITE_PORT;
	
	static {
		GRAPHITE_SERVER  = System.getenv( GRAPHITE_SERVER_VAR) ;
		GRAPHITE_PORT = Integer.parseInt( Optional.fromNullable( System.getenv( GRAPHITE_PORT_VAR ) ).or( "2003" ) );
		if( GRAPHITE_SERVER == null )  {
			graphite = Optional.absent();
		} else {
			graphite = Optional.of( new Graphite( new InetSocketAddress( GRAPHITE_SERVER , GRAPHITE_PORT ) ) );
		}
	}
	
	@Override
	public void configureReporters(MetricRegistry metricRegistry) {
		if( graphite.isPresent() ) {
			GraphiteReporter reporter = 
					GraphiteReporter
						.forRegistry( metricRegistry )
						.prefixedWith( getHostName() )
						.convertDurationsTo( TimeUnit.MILLISECONDS )
						.convertRatesTo( TimeUnit.MILLISECONDS )
						.build( graphite.get() );
			reporter.start( 1 , TimeUnit.SECONDS );
			logger.info("Address = {}:{}" , GRAPHITE_SERVER , GRAPHITE_PORT );
		} else {
			ConsoleReporter
				.forRegistry( metricRegistry )
				.convertDurationsTo( TimeUnit.MILLISECONDS )
				.convertRatesTo( TimeUnit.MILLISECONDS )
				.build();
		}
	}
	
	
	@Override
	public MetricRegistry getMetricRegistry() {
		return new MetricRegistry(); 
	}

	@Override
	public HealthCheckRegistry getHealthCheckRegistry() {
		return new HealthCheckRegistry();
	}
	
	private String getHostName() {
	    try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            logger.error( "Unable to resolve host name, defaulting to empty prefix." );
            return "";
        }
	}
}
