package com.cregis;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.cregis.svarog.MpcSessionManagerClient;
import com.cregis.svarog.utils.*;

public class UtilsTest {
    private static final Logger logger = Logger.getLogger(MpcSessionManagerClient.class.getName());

	@Test
	public void testPrimaryKey() {
		var sid = "019729ba-da65-74fb-b46a-6bca5878783b".replaceAll("-", "");
		var topic = "testPrimaryKey";
		var pk = new PrimaryKey(sid, topic, 11, 45, 14);
		logger.info(pk.toString());
	}
}
