package de.unileipzig.irpsim.server.data.stammdaten;

import org.junit.Assert;
import org.junit.Test;

import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;

public class StammdatumEqualsTest {

	@Test
	public void testEquals() {
		final Stammdatum arg1 = new Stammdatum();
		arg1.setBezugsjahr(2015);
		arg1.setTyp("#asd");
		arg1.setName("asd");
		Assert.assertFalse(arg1.equals(null));

		final Stammdatum arg2 = new Stammdatum();
		Assert.assertFalse(arg1.equals(arg2));

		arg2.setBezugsjahr(2015);
		Assert.assertFalse(arg1.equals(arg2));
	}
}
