package com.cregis.svarog;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import com.cregis.svarog.pb.MpcSessionManagerGrpc.MpcSessionManagerImplBase;
import com.cregis.svarog.pb.Svarog.EchoMessage;
import com.cregis.svarog.pb.Svarog.Message;
import com.cregis.svarog.pb.Svarog.SessionConfig;
import com.cregis.svarog.pb.Svarog.SessionId;
import com.cregis.svarog.pb.Svarog.VecMessage;
import com.cregis.svarog.pb.Svarog.Void;
import com.cregis.svarog.utils.PrimaryKey;
import com.github.f4b6a3.uuid.UuidCreator;
import com.google.protobuf.ByteString;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

// 在vscode里如何生成java的方法存根?
// (1') 单击并悬浮在类名上.
// (2') 按键 "Ctrl+.", 这会呼出一个菜单.
// (3') 单击菜单中的 "Override/implement Methods...",
//  这会弹出一个悬浮窗口供我选择函数名.
// (4') 选择要实现的方法, 确认.
public class MpcSessionManagerServerImpl extends MpcSessionManagerImplBase {
	private ConcurrentSkipListMap<PrimaryKey, Object> db = new ConcurrentSkipListMap<>();
	private static final Logger log = Logger.getLogger(MpcSessionManagerServer.class.getName());

	public void recycle() {
		while (true) {
			while (db.size() > 0) {
				var key = db.firstKey();
				if (key.isExpired()) {
					db.remove(key); // 删除过期条目
				} else {
					break; // 碰到第一个没过期, 那么之后的的都没过期.
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warning("recycle()线程的睡眠被中断");
			}
		}
	}

	@Override
	public void newSession(SessionConfig req, StreamObserver<SessionConfig> resp_ob) {
		var sid = UuidCreator.getTimeOrderedEpoch().toString().replace("-", "");
		var resp = SessionConfig.newBuilder(req).setSessionId(sid).build();
		var key = new PrimaryKey(sid, "session config", 0, 0, 0);
		db.put(key, resp);
		resp_ob.onNext(resp);
		resp_ob.onCompleted();
	}

	@Override
	public void getSessionConfig(SessionId req, StreamObserver<SessionConfig> resp_ob) {
		var sid = req.getValue();
		PrimaryKey key = null;
		try {
			key = new PrimaryKey(sid, "session config", 0, 0, 0);
		} catch (AssertionError e) {
			resp_ob.onError(Status.INVALID_ARGUMENT.withDescription(String.format("会话ID格式不正确")).asRuntimeException());
			return;
		}
		SessionConfig resp = (SessionConfig) db.get(key);
		if (resp == null) {
			resp_ob.onError(Status.INVALID_ARGUMENT.withDescription(String.format("会话ID不存在")).asRuntimeException());
			return;
		}
		resp_ob.onNext(resp);
		resp_ob.onCompleted();
	}

	@Override
	public void inbox(VecMessage req, StreamObserver<Void> resp_ob) {
		var msgs = req.getValuesList();
		var commit = new ConcurrentSkipListMap<PrimaryKey, Object>();
		for (var msg : msgs) {
			PrimaryKey key = null;
			try {
				key = new PrimaryKey(msg.getSessionId(), msg.getTopic(), msg.getSrc(), msg.getDst(), msg.getSeq());
			} catch (AssertionError e) {
				resp_ob.onError(
						Status.INVALID_ARGUMENT.withDescription(String.format("会话ID格式不正确")).asRuntimeException());
				return;
			}
			commit.put(key, msg.getObj());
		}
		db.putAll(commit);
		resp_ob.onNext(Void.newBuilder().build());
		resp_ob.onCompleted();
	}

	@Override
	public void outbox(VecMessage req, StreamObserver<VecMessage> resp_ob) {
		var indices = req.getValuesList();
		var msgs = new ArrayList<Message>();
		for (var idx : indices) {
			PrimaryKey key = null;
			try {
				key = new PrimaryKey(idx.getSessionId(), idx.getTopic(), idx.getSrc(), idx.getDst(), idx.getSeq());
			} catch (AssertionError e) {
				resp_ob.onError(
						Status.INVALID_ARGUMENT.withDescription(String.format("会话ID格式不正确")).asRuntimeException());
				return;
			}
			ByteString val = (ByteString) db.get(key);
			while (val == null) {
				if (key.isExpired()) {
					break;
				} else {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						resp_ob.onError(Status.ABORTED.asRuntimeException());
						return;
					}
					val = (ByteString) db.get(key);
					break;
				}
			}
			if (val == null) {
				resp_ob.onError(Status.DEADLINE_EXCEEDED
						.withDescription(String.format("未在合理时间内等到对方发送消息, 消息索引: ('%s', '%s', %d, %d, %d).",
								idx.getSessionId(), idx.getTopic(), idx.getSrc(), idx.getDst(), idx.getSeq()))
						.asRuntimeException());
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
