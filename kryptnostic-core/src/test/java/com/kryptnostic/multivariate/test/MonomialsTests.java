package com.kryptnostic.multivariate.test;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.kryptnostic.multivariate.Monomials;
import com.kryptnostic.multivariate.gf2.Monomial;

public class MonomialsTests {
	
	@Test 
	public void sortAndGetIndexTest() {
		List<Monomial> monomials = Lists.newArrayList(Monomials.allMonomials( 128 , 2));
		Monomials.sort(monomials);
		
		for (Integer i = 0; i < 50; i++) {
			Monomial m = monomials.get(i);
			Assert.assertEquals(i, Monomials.indexOfSorted(monomials, m));
		}
	}
	
	@Test
	public void allMonomialsTest() {
		Set<Monomial> monomials = Monomials.allMonomials(128, 2);
		Assert.assertEquals(monomials.size(), 8257);
	}
}