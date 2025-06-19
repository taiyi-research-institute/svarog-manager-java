package com.cregis.svarog;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import com.cregis.svarog.pb.MpcSessionManagerGrpc.MpcSessionManagerImplBase;
import com.cregis.svarog.pb.Svarog.EchoMessage;
import com.cregis.svarog.pb.Svarog.Message;
import com.cregis.svarog.pb.Svarog.SessionConfig;
import com.cregis.svarog.pb.Svarog.SessionId;
import com.cregis.svarog.pb.Svarog.VecMessage;
import com.cregis.svarog.pb.Svarog.Void;
import com.github.f4b6a3.uuid.UuidCreator;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

// 本打算用 jdk 自带的 `ConcurrentSkipListMap`, 但测试时发现其经常无法读出刚刚添加的 (K,V).
// 所以换成 `Caffeine`, 据 GPT-4.1 说是广为使用的工业级库.
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class MpcSessionManagerServerImpl extends MpcSessionManagerImplBase {
	private Cache<String, Object> db;
	private static final Logger log = Logger.getLogger(MpcSessionManagerServer.class.getName());

	public MpcSessionManagerServerImpl() {
		db = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(Consts.EXPIRE_SEC)).build();
	}

	@Override
	public void newSession(SessionConfig req, StreamObserver<SessionId> resp_ob) {
		var sid = UuidCreator.getTimeOrderedEpoch().toString().replace("-", "");
		var resp = SessionId.newBuilder().setValue(sid).build();
		var key = Utils.primaryKey(sid, "sesconf", 0, 0, 0);
		db.put(key, req);
		resp_ob.onNext(resp);
		resp_ob.onCompleted();
	}

	@Override
	public void getSessionConfig(SessionId req, StreamObserver<SessionConfig> resp_ob) {
		var sid = req.getValue();
		var key = Utils.primaryKey(sid, "sesconf", 0, 0, 0);
		SessionConfig resp = (SessionConfig) db.getIfPresent(key);
		if (resp == null) {
			resp_ob.onError(Status.NOT_FOUND.withDescription(String.format("会话ID不存在")).asRuntimeException());
			return;
		}
		resp_ob.onNext(resp);
		resp_ob.onCompleted();
	}

	@Override
	public void inbox(VecMessage req, StreamObserver<Void> resp_ob) {
		var msgs = req.getValuesList();
		for (var msg : msgs) {
			var key = Utils.primaryKey(msg.getSessionId(), msg.getTopic(), msg.getSrc(), msg.getDst(), msg.getSeq());
			db.put(key, msg.getObj());
		}
		resp_ob.onNext(Void.newBuilder().build());
		resp_ob.onCompleted();
	}

	@Override
	public void outbox(VecMessage req, StreamObserver<VecMessage> resp_ob) {
		var indices = req.getValuesList();
		var msgs = new ArrayList<Message>();
		for (var idx : indices) {
			var key = Utils.primaryKey(idx.getSessionId(), idx.getTopic(), idx.getSrc(), idx.getDst(), idx.getSeq());
			ByteString val = (ByteString) db.getIfPresent(key);
			var ddl = System.currentTimeMillis() + Consts.EXPIRE_SEC * 1000;
			while (val == null) {
				if (System.currentTimeMillis() >= ddl) {
					resp_ob.onError(Status.DEADLINE_EXCEEDED
							.withDescription(String.format("未在合理时间内等到对方发送消息, 消息索引: %s.", key))
							.asRuntimeException());
					return;
				} else {
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						resp_ob.onError(Status.ABORTED.asRuntimeException());
						return;
					}
					val = (ByteString) db.getIfPresent(key);
				}
			}
			msgs.add(Message.newBuilder(idx).setObj(val).build());
		}
		resp_ob.onNext(VecMessage.newBuilder().addAllValues(msgs).build());
		resp_ob.onCompleted();
	}

	@Override
	public void ping(Void req, StreamObserver<EchoMessage> resp_ob) {
		resp_ob.onNext(EchoMessage.newBuilder().setValue("Svarog会话管理器正在运行中.").build());
		resp_ob.onCompleted();
	}
}
