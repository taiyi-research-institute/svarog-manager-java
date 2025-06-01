package com.cregis.svarog.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.digests.Blake2bDigest;

import com.github.f4b6a3.uuid.UuidCreator;

public class PrimaryKey implements Comparable<PrimaryKey> {
	private final byte[] data;
	private static final long SESSION_EXPIRE_MS = 300_000;

	private PrimaryKey(byte[] data) {
		this.data = data;
	}

	public PrimaryKey(String sid, String topic, long src, long dst, long seq) throws AssertionError {
		this.data = new byte[32];

		// 确保: sid非空, 由连续32个hex digit组成.
		assert sid != null && sid.matches("^[0-9A-Fa-f]{32}$");
		byte[] uuid_bytes = null;
		try {
			uuid_bytes = Hex.decodeHex(sid);
		} catch (DecoderException _e) {
			// 永远执行不到这里.
		}

		System.arraycopy(uuid_bytes, 0, data, 0, 16);

		var ha = new Blake2bDigest(128);
		var buf = topic.getBytes();
		ha.update(buf, 0, buf.length);
		buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(src).array();
		ha.update(buf, 0, buf.length);
		buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(dst).array();
		ha.update(buf, 0, buf.length);
		buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(seq).array();
		ha.update(buf, 0, buf.length);
		ha.doFinal(this.data, 16);
	}

	@Override
	public String toString() {
		return Hex.encodeHexString(this.data);
	}

	@Override
	public int compareTo(PrimaryKey o) {
		return Arrays.compare(this.data, o.data);
	}

	public boolean isExpired() {
		var data = new byte[32];
		Arrays.fill(data, (byte) 0);

		// UUID-v7中的时间戳是48-bit unix毫秒
		var t = System.currentTimeMillis() - SESSION_EXPIRE_MS;
		var tb = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(t).array();
		System.arraycopy(tb, 2, data, 0, 6);

		// 若时间戳早于当前时间减去会话时长(SESSION_EXPIRE_MS), 则判为过期.
		return Arrays.compare(this.data, data) <= 0;
	}
}
