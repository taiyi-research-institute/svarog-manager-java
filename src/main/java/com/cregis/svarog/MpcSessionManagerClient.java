package com.cregis.svarog;

import com.cregis.svarog.pb.MpcSessionManagerGrpc;
import com.cregis.svarog.pb.MpcSessionManagerGrpc.MpcSessionManagerBlockingStub;
import com.cregis.svarog.pb.Svarog.SessionConfig;
import com.cregis.svarog.pb.Svarog.SessionId;
import com.cregis.svarog.pb.Svarog.Void;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.NotImplementedException;

/**
 * 该客户端仅用于测试服务端的连通性. rust版的客户端能够批量收发mpc消息. 该客户端不具备前述能力. 这是因为, 既然没有java版的算法,
 * 那就不必实现完整的客户端.
 */
public class MpcSessionManagerClient {
	private final MpcSessionManagerBlockingStub stub;

	public MpcSessionManagerClient(String hostport) {
		ManagedChannel ch = Grpc.newChannelBuilder(hostport, InsecureChannelCredentials.create()).build();
		this.stub = MpcSessionManagerGrpc.newBlockingStub(ch).withCompression("gzip")
				.withDeadlineAfter(Duration.ofSeconds(Consts.EXPIRE_SEC));
	}

	public SessionConfig grpcNewSession(long thres, Map<String, Boolean> players, Map<String, Boolean> players_reshared)
			throws io.grpc.StatusRuntimeException {
		var req = SessionConfig.newBuilder().putAllPlayers(players).putAllPlayersReshared(players_reshared)
				.setThreshold(thres).build();
		var cfg = this.stub.newSession(req);
		return cfg;
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
	public void Require(Object obj, String sid, String topic, long src, long dst, long seq) {
		throw new NotImplementedException();
	}

	/**
	 * 注册将要发送的对象.
	 */
	public void Provide(Object obj, String sid, String topic, long src, long dst, long seq) {
		throw new NotImplementedException();
	}

	/**
	 * 通过Svarog Manager交换注册过的对象.
	 * 我们称 (sid, topic, src, dst, seq) 为对象的mpc地址.
	 * 各mpc参与方执行Exchange之后, 出现于Require的obj, 会被改写为出现于Provide且mpc地址相同的obj.
	 */
	public void Exchange() {
		throw new NotImplementedException();
	}
}
