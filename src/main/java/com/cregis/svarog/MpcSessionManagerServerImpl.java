package com.cregis.svarog;

import com.cregis.svarog.pb.MpcSessionManagerGrpc.MpcSessionManagerImplBase;
import com.cregis.svarog.pb.Svarog.EchoMessage;
import com.cregis.svarog.pb.Svarog.SessionConfig;
import com.cregis.svarog.pb.Svarog.SessionId;
import com.cregis.svarog.pb.Svarog.VecMessage;
import com.cregis.svarog.pb.Svarog.Void;
import com.github.f4b6a3.uuid.UuidCreator;
import io.grpc.stub.StreamObserver;

// 在vscode里如何生成java的方法存根?
// (1') 单击并悬浮在类名上.
// (2') 按键 "Ctrl+.", 这会呼出一个菜单.
// (3') 单击菜单中的 "Override/implement Methods...",
//  这会弹出一个悬浮窗口供我选择函数名.
// (4') 选择要实现的方法, 确认.
public class MpcSessionManagerServerImpl extends MpcSessionManagerImplBase {
	@Override
	public void newSession(SessionConfig req, StreamObserver<SessionConfig> resp_ob) {
		String sid = req.getSessionId();
		if (sid == null || sid.equals("")) {
			sid = UuidCreator.getTimeOrderedEpoch().toString().replace("-", "");
		}
		super.newSession(req, resp_ob);
	}

	@Override
	public void getSessionConfig(SessionId req, StreamObserver<SessionConfig> resp_ob) {
		// TODO Auto-generated method stub
		SessionConfig resp = SessionConfig.newBuilder().setSessionId("114514").build();
		resp_ob.onNext(resp);
		resp_ob.onCompleted();
	}

	@Override
	public void inbox(VecMessage req, StreamObserver<Void> resp_ob) {
		// TODO Auto-generated method stub
		super.inbox(req, resp_ob);
	}

	@Override
	public void outbox(VecMessage req, StreamObserver<VecMessage> resp_ob) {
		// TODO Auto-generated method stub
		super.outbox(req, resp_ob);
	}

	@Override
	public void ping(Void req, StreamObserver<EchoMessage> resp_ob) {
		// TODO Auto-generated method stub
		super.ping(req, resp_ob);
	}
}
