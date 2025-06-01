package com.cregis.svarog;

public class Utils {
	public static String primaryKey(String sid, String topic, long src, long dst, long seq) {
		return String.format("%s-%s-%d-%d-%d", sid, topic, src, dst, seq);
	}
}
