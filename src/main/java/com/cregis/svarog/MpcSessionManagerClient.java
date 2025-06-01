package com.cregis.svarog;

import com.cregis.svarog.pb.MpcSessionManagerGrpc;
import com.cregis.svarog.pb.MpcSessionManagerGrpc.MpcSessionManagerBlockingStub;
import com.cregis.svarog.pb.Svarog.SessionConfig;
import com.cregis.svarog.pb.Svarog.SessionId;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import java.util.logging.Logger;

public class MpcSessionManagerClient {
	private final MpcSessionManagerBlockingStub stub;
	private static final Logger logger = Logger.getLogger(MpcSessionManagerClient.class.getName());

	public MpcSessionManagerClient(String hostport) {
		ManagedChannel ch = Grpc.newChannelBuilder(hostport, InsecureChannelCredentials.create()).build();
		this.stub = MpcSessionManagerGrpc.newBlockingStub(ch);
	}

	public SessionConfig getSessionConfig(String sid) throws Exception {
		// TODO
		var req = SessionId.newBuilder().setValue(sid).build();
		SessionConfig resp = this.stub.getSessionConfig(req);
		return resp;
	}
}
