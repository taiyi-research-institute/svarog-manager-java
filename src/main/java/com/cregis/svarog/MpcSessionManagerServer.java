package com.cregis.svarog;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.cregis.svarog.pb.MpcSessionManagerGrpc.MpcSessionManagerImplBase;
import com.cregis.svarog.pb.Svarog.EchoMessage;
import com.cregis.svarog.pb.Svarog.SessionConfig;
import com.cregis.svarog.pb.Svarog.SessionId;
import com.cregis.svarog.pb.Svarog.VecMessage;
import com.cregis.svarog.pb.Svarog.Void;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;

// 在vscode里如何生成java的方法存根?
// (1') 单击并悬浮在类名上.
// (2') 按键 "Ctrl+.", 这会呼出一个菜单.
// (3') 单击菜单中的 "Override/implement Methods...",
//  这会弹出一个悬浮窗口供我选择函数名.
// (4') 选择要实现的方法, 确认.
public class MpcSessionManagerServer {
    public static void main(String[] args) throws Exception {
        var server = new MpcSessionManagerServer(3000);
        server.start();
        server.blockUntilShutdown();
    }

    private final Server server_object;

    public MpcSessionManagerServer(int port) {
        var server_builder = Grpc.newServerBuilderForPort(
                port,
                InsecureServerCredentials.create());
        var server_impl = new MpcSessionManagerService();
        this.server_object = server_builder.addService(server_impl).build();
    }

    public void start() throws IOException {
        server_object.start();
        var th = new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown
                // hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    MpcSessionManagerServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        };
        Runtime.getRuntime().addShutdownHook(th);
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        if (server_object != null) {
            server_object
                    .shutdown()
                    .awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon
     * threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server_object != null) {
            server_object.awaitTermination();
        }
    }

    private static class MpcSessionManagerService extends MpcSessionManagerImplBase {
        @Override
        public void getSessionConfig(SessionId req, StreamObserver<SessionConfig> resp_ob) {
            // TODO Auto-generated method stub
            SessionConfig resp = SessionConfig.newBuilder()
                    .setSessionId("114514")
                    .build();
            resp_ob.onNext(resp);
            resp_ob.onCompleted();
        }

        @Override
        public void inbox(VecMessage req, StreamObserver<Void> resp_ob) {
            // TODO Auto-generated method stub
            super.inbox(req, resp_ob);
        }

        @Override
        public void newSession(SessionConfig req, StreamObserver<SessionId> resp_ob) {
            // TODO Auto-generated method stub
            super.newSession(req, resp_ob);
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
}
