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
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurer;

@Configuration
@EnableMetrics(proxyTargetClass=true)
public class MetricsConfiguration implements MetricsConfigurer {
	//TODO: Make this configurable instead of being hardcoded to our internal graphite server.
	private static final Optional<Graphite> graphite; 
	private static final Logger logger = LoggerFactory.getLogger( MetricsConfiguration.class );
	
	static {
		String host = System.getenv("graphite-server");
		int port = Integer.parseInt( Optional.fromNullable( System.getenv("graphite-port" ) ).or( "2003" ) );
		if( host == null )  {
			graphite = Optional.absent();
		} else {
			graphite = Optional.of( new Graphite( new InetSocketAddress( host , port ) ) );
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
			logger.error("Address = {}:{}" , System.getenv("graphite-server") , Integer.parseInt( System.getenv("graphite-port" ) ) );
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
