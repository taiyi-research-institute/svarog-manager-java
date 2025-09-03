package com.cregis.svarog;

import com.cregis.svarog.pb.MpcSessionManagerGrpc;
import com.cregis.svarog.pb.MpcSessionManagerGrpc.MpcSessionManagerBlockingStub;
import com.cregis.svarog.pb.Svarog.Message;
import com.cregis.svarog.pb.Svarog.SessionConfig;
import com.cregis.svarog.pb.Svarog.SessionId;
import com.cregis.svarog.pb.Svarog.VecMessage;
import com.cregis.svarog.pb.Svarog.Void;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.google.protobuf.ByteString;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 因为 `provide_buffer` 和 `require_buffer` 线程不安全, 所以每个client应仅在一个线程里使用.
 * 因为一个线程代表0~1个MPC参与方, 所以这种设计已经够用了.
 */
public class MpcSessionManagerClientImpl {
	private final MpcSessionManagerBlockingStub stub;
	private Map<String, ByteString> provide_buffer;
	private Map<String, Object> require_buffer;
	private static final Logger log = Logger.getLogger(MpcSessionManagerServer.class.getName());

	public MpcSessionManagerClientImpl(String hostport) {
		ManagedChannel ch = Grpc.newChannelBuilder(hostport, InsecureChannelCredentials.create()).build();
		this.stub = MpcSessionManagerGrpc.newBlockingStub(ch).withCompression("gzip")
				.withDeadlineAfter(Duration.ofSeconds(Consts.EXPIRE_SEC));
		provide_buffer = new HashMap<>();
		require_buffer = new HashMap<>();
	}

	public String grpcNewSession(long thres, Map<String, Boolean> players, Map<String, Boolean> players_reshared)
			throws io.grpc.StatusRuntimeException {
		var req = SessionConfig.newBuilder().putAllPlayers(players).putAllPlayersReshared(players_reshared)
				.setThreshold(thres).build();
		var sid = this.stub.newSession(req);
		return sid.getValue();
	}

	public SessionConfig grpcGetSessionConfig(String session_id) throws io.grpc.StatusRuntimeException {
		var req = SessionId.newBuilder().setValue(session_id).build();
		var cfg = this.stub.getSessionConfig(req);
		return cfg;
	}

	public String ping() throws io.grpc.StatusRuntimeException {
		var req = Void.newBuilder().build();
		var echo = this.stub.ping(req);

		// 不要被名字误导. getValue并不是一个通用的函数名, 而是因为在proto文件里有一个名为value的字段.
		return echo.getValue();
	}

	/**
	 * 注册将要接收的对象. 对象需正确初始化.
	 */
	public String register_recv(Object obj, String sid, String topic, long src, long dst, long seq) {
		var key = Utils.primaryKey(sid, topic, src, dst, seq);
		require_buffer.put(key, obj);
		return key;
	}

	/**
	 * 注册将要发送的对象.
	 */
	public String register_send(Object obj, String sid, String topic, long src, long dst, long seq) {
		var cbor = new ObjectMapper(new CBORFactory());
		byte[] bytes = null;
		try {
			bytes = cbor.writeValueAsBytes(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		var key = Utils.primaryKey(sid, topic, src, dst, seq);
		provide_buffer.put(key, ByteString.copyFrom(bytes));
		return key;
	}

	/**
	 * 通过Svarog Manager交换注册过的对象. 我们称 (sid, topic, src, dst, seq) 为对象的mpc地址.
	 * 各mpc参与方执行Exchange之后, 作为Require参数的obj, 会被 **改写** 为Provide参数中mpc地址相同的obj.
	 * 
	 * @throws io.grpc.StatusRuntimeException
	 *             服务端执行失败
	 * @throws AssertionError
	 *             客户端收到了并未请求的消息
	 * @throws IOException
	 *             客户端收到的消息不能解码为客户端想要的数据类型.
	 */
	public void exchange() throws io.grpc.StatusRuntimeException, AssertionError, IOException {
		var msgs_builder = VecMessage.newBuilder();
		for (var k : provide_buffer.keySet()) {
			var v = provide_buffer.get(k);
			var msg = Message.newBuilder().setTopic(k).setObj(v).build();
			msgs_builder.addValues(msg);
		}
		var msgs = msgs_builder.build();
		stub.inbox(msgs);

		var idxs_builder = VecMessage.newBuilder();
		for (var k : require_buffer.keySet()) {
			var idx = Message.newBuilder().setTopic(k).build();
			idxs_builder.addValues(idx);
		}
		var idxs = idxs_builder.build();
		var _resp_msgs = stub.outbox(idxs);
		var resp_msgs = _resp_msgs.getValuesList();

		for (var msg : resp_msgs) {
			var key = Utils.primaryKey(msg.getSessionId(), msg.getTopic(), msg.getSrc(), msg.getDst(), msg.getSeq());
			var buf = msg.getObj().toByteArray();
			var dst = require_buffer.get(key);
			assert dst != null : String.format("客户端并未请求却收到了消息`%s`", key);

			var cbor = new ObjectMapper(new CBORFactory());
			Object src = null;
			try {
				src = cbor.readValue(buf, dst.getClass());
			} catch (IOException e) {
				throw new IOException(String.format("消息`%s`解码失败", key), e);
			}

			Utils.copyFields(src, dst);
		}

		clear();
	}

	public void clear() {
		provide_buffer.clear();
		require_buffer.clear();
	}
}
