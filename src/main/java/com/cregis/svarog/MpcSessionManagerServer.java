package com.cregis.svarog;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MpcSessionManagerServer {
	public static void main(String[] args) throws Exception {
		var server = new MpcSessionManagerServer(3000);
		server.start();
		server.blockUntilShutdown();
	}

	private final Server server_object;
	private final int port;
	private static final Logger logger = Logger.getLogger(MpcSessionManagerServer.class.getName());

	public MpcSessionManagerServer(int port) {
		this.port = port;
		var server_builder = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create());
		var impl = new MpcSessionManagerServerImpl();
		this.server_object = server_builder.addService(impl).build();
	}

	public void start() throws IOException {
		server_object.start();
		logger.info("SvarogManager started listening on " + this.port);
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
			server_object.shutdown().awaitTermination(10, TimeUnit.SECONDS);
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
}
