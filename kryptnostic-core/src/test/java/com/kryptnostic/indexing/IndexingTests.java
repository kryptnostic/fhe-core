package com.kryptnostic.indexing;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import com.kryptnostic.indexing.metadata.Metadata;
import com.kryptnostic.indexing.metadata.Metadatum;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class IndexingTests {
	private static final int TOKEN_LENGTH = 256;
	private static final int NONCE_LENGTH = 64;
	private static final int LOCATION_LENGTH = 64;
	private static final int BUCKET_SIZE = 100 ;
	private static final Logger logger = LoggerFactory.getLogger( IndexingTests.class );

	private static MetadataKeyService keyService;
	private static IndexingService indexingService;
	
	@BeforeClass
	public static void setupServices() {
		SimplePolynomialFunction indexingHashFunction= Indexes.generateRandomIndexingFunction(TOKEN_LENGTH, NONCE_LENGTH, LOCATION_LENGTH);
		keyService = new BalancedMetadataKeyService(indexingHashFunction, BUCKET_SIZE, NONCE_LENGTH );
		indexingService = new BaseIndexingService();
	}
	
	@Test
	public void testIndexingAndKeying() throws IOException {
		String document = Resources.toString( Resources.getResource("privacy.txt") , Charsets.UTF_8 );
		logger.info("Loaded privacy.txt");
		long start = System.nanoTime();
		String documentId = Hashing.sha256().hashString(document, Charsets.UTF_8).toString();
		logger.info("Hashed document of length {} in {} ms.", document.length() , TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start ) );
		
		start = System.nanoTime();
		Set<Metadatum> metadata = indexingService.index(documentId, document);
		logger.info("Indexed document of length {} in {} ms." , document.length() , TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start ) );
		Assert.assertNotNull( metadata );
		Assert.assertTrue( !metadata.isEmpty() );
		
		start = System.nanoTime();
		Metadata balancedMetadata = keyService.mapTokensToKeys(metadata);
		logger.info( "Mapped token keys in {} ms", TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start ) );
		Assert.assertNotNull( balancedMetadata );
	}
	
}
