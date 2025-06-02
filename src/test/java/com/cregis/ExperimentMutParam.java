package com.cregis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.cregis.svarog.MpcSessionManagerClient;
import com.cregis.svarog.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

public class ExperimentMutParam {
	private static final Logger logger = Logger.getLogger(MpcSessionManagerClient.class.getName());

	@Test
	public void testMutableParameter() {
		assertDoesNotThrow(() -> {
			var p0 = new Point(1919, 810, new Curve(37, 19));
			fnWithMutParam(p0);
			logger.info("p0, " + p0.toString());
		});
	}

	public void fnWithMutParam(Object mutParam) throws Exception {
		var mapper = new ObjectMapper(new CBORFactory());
		var p = new Point(114, 514, new Curve(31, 17));
		var buf = mapper.writeValueAsBytes(p);
		var p2 = mapper.readValue(buf, mutParam.getClass());
		logger.info("p2, " + p2.toString());
		Utils.copyFields(p2, mutParam);
		logger.info("mutParam, " + mutParam.toString());
	}
}

class Point {
	public int x;
	public int y;
	public Curve cc;

	public Point() {
	}

	public Point(int x, int y, Curve cc) {
		this.x = x;
		this.y = y;
		this.cc = cc;
	}

	@Override
	public String toString() {
		return String.format("(%d, %d) %s", x, y, cc.toString());
	}
}

class Curve {
	public int n;
	public int p;

	public Curve() {
		n = 0;
		p = 0;
	}

	public Curve(int n, int p) {
		this.n = n;
		this.p = p;
	}

	@Override
	public String toString() {
		return String.format("[fp %d, mod %d]", p, n);
	}
}