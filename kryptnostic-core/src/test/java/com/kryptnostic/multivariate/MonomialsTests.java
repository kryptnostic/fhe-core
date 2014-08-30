package com.kryptnostic.multivariate;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.kryptnostic.multivariate.Monomials;
import com.kryptnostic.multivariate.gf2.Monomial;

public class MonomialsTests {
	private static final Logger logger = LoggerFactory.getLogger(MonomialsTests.class);
	private static final Random r = new Random( 0 );
	
	@Test 
	public void sortAndGetIndexTest() {
		Stopwatch watch;
		watch = Stopwatch.createStarted();
		List<Monomial> monomials = Lists.newArrayList(Monomials.allMonomials( 256 , 2));
		watch.stop();
		logger.info("Monomial creation time: " + watch);
		
		watch = Stopwatch.createStarted();
		Monomials.sort(monomials);
		watch.stop();
		logger.info("Monomial list sort time: " + watch);
		
		double timeSum = 0;
		long n = 8000;
		
		for (Integer i = 0; i < n; i++) {
			// pick a random index to lookup, so hardware doesn't improve perf. with prediction.
			Integer index = r.nextInt(monomials.size());
			
			Monomial m = monomials.get(index);
			watch = Stopwatch.createStarted();
			Assert.assertEquals(index, Monomials.indexOfSorted(monomials, m));
			watch.stop();
			timeSum += watch.elapsed(TimeUnit.MICROSECONDS);
			
		}
		logger.info("index lookup time: " + (timeSum / n) + " microseconds.");
	}
	
	@Test
    public void allMonomialsTest() {
    	Set<Monomial> allMonomials = Sets.newHashSet();
    	allMonomials.add( new Monomial( 4 ));
    	allMonomials.add( new Monomial( 4 ).chainSet(0) );
    	allMonomials.add( new Monomial( 4 ).chainSet(1) );
    	allMonomials.add( new Monomial( 4 ).chainSet(2) );
    	allMonomials.add( new Monomial( 4 ).chainSet(3) );
    	allMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(1) );
    	allMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(2) );
    	allMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(3) );
    	allMonomials.add( new Monomial( 4 ).chainSet(1).chainSet(2) );
    	allMonomials.add( new Monomial( 4 ).chainSet(1).chainSet(3) );
    	allMonomials.add( new Monomial( 4 ).chainSet(2).chainSet(3) );	
    	allMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(1).chainSet(2) );
    	allMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(1).chainSet(3) );
    	allMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(2).chainSet(3) );
    	allMonomials.add( new Monomial( 4 ).chainSet(1).chainSet(2).chainSet(3) );
    	allMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(1).chainSet(2).chainSet(3) );
    	
    	Set<Monomial> order2AndLessMonomials = Sets.newHashSet();
    	order2AndLessMonomials.add( new Monomial( 4 ));
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(0) );
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(1) );
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(2) );
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(3) );
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(1) );
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(2) );
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(0).chainSet(3) );
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(1).chainSet(2) );
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(1).chainSet(3) );
    	order2AndLessMonomials.add( new Monomial( 4 ).chainSet(2).chainSet(3) );
    	
    	Assert.assertEquals( allMonomials, Monomials.allMonomials(4, 4));
    	Assert.assertEquals( order2AndLessMonomials, Monomials.allMonomials(4, 2));
    	
    	Set<Monomial> monomials = Monomials.allMonomials(128, 2);
		Assert.assertEquals(monomials.size(), 8257);
    }
}