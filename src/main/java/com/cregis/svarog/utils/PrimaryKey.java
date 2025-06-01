package com.cregis.svarog.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import org.bouncycastle.crypto.digests.Blake2bDigest;

public class PrimaryKey {
	private final byte[] data;

	public PrimaryKey(String sid, String topic, int src, int dst, int seq) throws AssertionError {
		this.data = new byte[32];

		// 确保: sid非空, 由连续32个hex digit组成.
		assert sid != null && sid.matches("^[0-9A-Fa-f]{32}$");
		byte[] uuid_bytes = null;
		try {
			uuid_bytes = Hex.decodeHex(sid);
		} catch (DecoderException _e) {
			// should never encounter an exception
		}

		System.arraycopy(uuid_bytes, 0, data, 0, 16);

		var ha = new Blake2bDigest(128);
		var buf = topic.getBytes();
		ha.update(buf, 0, buf.length);
		buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(src).array();
		ha.update(buf, 0, buf.length);
		buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dst).array();
		ha.update(buf, 0, buf.length);
		buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(seq).array();
		ha.update(buf, 0, buf.length);
		ha.doFinal(this.data, 16);
	}

	@Override
	public String toString() {
		return Hex.encodeHexString(this.data);
	}
}
