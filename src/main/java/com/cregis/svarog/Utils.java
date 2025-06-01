package com.cregis.svarog;

import org.apache.commons.lang3.reflect.FieldUtils;

public class Utils {
	public static String primaryKey(String sid, String topic, long src, long dst, long seq) {
		return String.format("%s-%s-%d-%d-%d", sid, topic, src, dst, seq);
	}

	public static void copyFields(Object src, Object dst) {
		for (var field : FieldUtils.getAllFieldsList(src.getClass())) {
			field.setAccessible(true);
			try {
				field.set(dst, field.get(src));
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
