package com.cregis;

import com.cregis.svarog.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class MpcSessionManagerTest {
	private static final Logger logger = Logger.getLogger(MpcSessionManagerClient.class.getName());

	@Test
	public void testServerClient() {
		// 启动服务器线程.
		var server = new MpcSessionManagerServer(65530);
		var server_thread = new Thread(() -> {
			try {
				server.start();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "启动Svarog Manager服务器时发生异常: " + e.getMessage());
				e.printStackTrace();
			}
		});
		server_thread.setDaemon(true); // 设置为守护线程，防止阻塞主线程的退出.
		assertDoesNotThrow(server_thread::start);

		// 等待服务器完全启动.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// 模拟客户端调用服务器.
		assertDoesNotThrow(() -> {
			var client = new MpcSessionManagerClient("127.0.0.1:65530");
			var ping = client.ping();
			logger.info(ping);
		});
	}

	@Test
	public void testExpiration() {
		// 启动服务器线程.
		var server = new MpcSessionManagerServer(65530);
		var server_thread = new Thread(() -> {
			try {
				server.start();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "启动Svarog Manager服务器时发生异常: " + e.getMessage());
				e.printStackTrace();
			}
		});
		server_thread.setDaemon(true); // 设置为守护线程，防止阻塞主线程的退出.
		assertDoesNotThrow(server_thread::start);

		// 等待服务器完全启动.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// 模拟客户端调用服务器.
		var client = new MpcSessionManagerClient("127.0.0.1:65530");
		var cfg = client.grpcNewSession(0, new HashMap<>(), new HashMap<>());
		var sid = cfg.getSessionId();
		logger.info(sid);
		for (int i = 1;; i++) {
			try {
				cfg = client.grpcGetSessionConfig(sid);
				logger.info("会话还在, " + i);
			} catch (io.grpc.StatusRuntimeException e) {
				logger.info("会话不在了, " + i);
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
