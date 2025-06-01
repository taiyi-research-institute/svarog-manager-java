package com.cregis;

import java.util.concurrent.ConcurrentSkipListMap;
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
		var pk1 = new PrimaryKey(sid, topic, 11, 45, 14);
		logger.info(pk1.toString());

		sid = "01972b0f-54cf-7513-a06f-d594635b5df4".replaceAll("-", "");
		var pk2 = new PrimaryKey(sid, topic, 19, 19, 810);
		logger.info(pk2.toString());

		sid = "01972b0c-c81e-7a29-b7f1-8b57b6ebf0c8".replaceAll("-", "");
		var pk3 = new PrimaryKey(sid, topic, 1, 2, 3);
		logger.info(pk3.toString());

		var db = new ConcurrentSkipListMap<PrimaryKey, String>();
		db.put(pk3, "pk3");
		db.put(pk1, "pk1");
		db.put(pk2, "pk2");
		logger.info(db.toString());
	}
}
