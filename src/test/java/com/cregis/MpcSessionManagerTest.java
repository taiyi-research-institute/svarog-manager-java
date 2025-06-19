package com.cregis;

import com.cregis.svarog.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class MpcSessionManagerTest {
	private static final Logger logger = Logger.getLogger(MpcSessionManagerClient.class.getName());

	public void startServer() {
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
	}

	@Test
	public void testServerClient() {
		startServer();

		// 模拟客户端调用服务器.
		assertDoesNotThrow(() -> {
			var client = new MpcSessionManagerClient("127.0.0.1:65530");
			var ping = client.ping();
			logger.info(ping);
		});
	}

	@Test
	public void testExpiration() {
		startServer();

		// 模拟客户端调用服务器.
		var client = new MpcSessionManagerClient("127.0.0.1:65530");
		var sid = client.grpcNewSession(0, new HashMap<>(), new HashMap<>());
		logger.info(sid);
		for (int i = 1;; i++) {
			try {
				var cfg = client.grpcGetSessionConfig(sid);
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

	@Test
	public void testCommunication() {
		startServer();

		var cl = new MpcSessionManagerClient("127.0.0.1:65530");

		// 测试Exchange能否修改vec中的元素.
		List<Curve> vec = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			cl.mpcProvide(new Curve(i * 2, 1 + i * 2), "通讯测试", "修改vec元素", 0, i, 0);
			var recv = new Curve();
			vec.add(recv);

			cl.mpcRequire(recv, "通讯测试", "修改vec元素", 0, i, 0);
		}
		assertDoesNotThrow(() -> {
			cl.mpcExchange();
		});

		// 测试收到的vec和发出的是否一致.
		for (int i = 0; i < 5; i++) {
			var x0 = new Curve(i * 2, 1 + i * 2).toString();
			var x1 = vec.get(i).toString();
			assert x0.equals(x1);
		}

		// 测试Exchange能否修改Map元素.
		Map<String, Curve> map = new HashMap<>();
		for (int i = 0; i < 5; i++) {
			var key = cl.mpcProvide(new Curve(i * 2, 1 + i * 2), "通讯测试", "修改map元素", 0, i, 0);

			var recv = new Curve();
			map.put(key, recv);

			cl.mpcRequire(recv, "通讯测试", "修改map元素", 0, i, 0);
		}
		assertDoesNotThrow(() -> {
			cl.mpcExchange();
		});

		// 测试收到的map和发出的是否一致.
		for (int i = 0; i < 5; i++) {
			var x0 = new Curve(i * 2, 1 + i * 2).toString();
			var key = Utils.primaryKey("通讯测试", "修改map元素", 0, i, 0);
			var x1 = map.get(key).toString();
			assert x0.equals(x1);
		}
	}
}
