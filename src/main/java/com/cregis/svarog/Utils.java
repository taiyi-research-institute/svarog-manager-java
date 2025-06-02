package com.cregis.svarog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bouncycastle.jcajce.provider.digest.Blake2b;

public class Utils {
	public static String primaryKey(String sid, String topic, long src, long dst, long seq) {
		var ha = new Blake2b.Blake2b160();
		ha.update(sid.getBytes());
		ha.update(topic.getBytes());

		// 此处有坑. ByteBuffer必须刻意转成byte[], 否则更新到ha上的是固定值.
		ha.update(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(src).array());
		ha.update(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(dst).array());
		ha.update(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(seq).array());

		return Hex.encodeHexString(ha.digest());
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
