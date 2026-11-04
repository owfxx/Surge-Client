package gg.surge.client.features;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DiscordRpc {
	public static final String DEFAULT_ID = "1567508779472388246";

	public record Activity(String details, String state, long since) {}

	private static volatile Activity wanted;
	private static volatile boolean enabled;
	private static Thread thread;
	private static InputStream in;
	private static OutputStream out;
	private static Runnable closer;
	private static Activity sent;
	public static volatile boolean connected;

	private DiscordRpc() {}

	public static String clientId() {
		String id = System.getProperty("surge.discordId", "").replaceAll("\\D", "");
		return id.isEmpty() ? DEFAULT_ID : id;
	}

	public static synchronized void update(boolean on, Activity a) {
		enabled = on;
		wanted = a;
		if (thread == null) {
			thread = new Thread(DiscordRpc::loop, "Surge Discord");
			thread.setDaemon(true);
			thread.start();
		}
	}

	private static void loop() {
		long wait = 0;
		while (true) {
			try {
				Thread.sleep(Math.max(1500, wait));
				wait = 0;
				String id = clientId();
				if (!enabled || id.isEmpty()) { close(); continue; }
				if (out == null && !connect(id)) { wait = 15000; continue; }
				Activity a = wanted;
				if (a != null && !a.equals(sent)) {
					send(1, activityPayload(a));
					read();
					sent = a;
				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				close();
				wait = 10000;
			}
		}
	}

	private static List<Path> unixPaths() {
		List<String> bases = new ArrayList<>();
		for (String env : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
			String v = System.getenv(env);
			if (v != null && !v.isEmpty()) bases.add(v);
		}
		bases.add("/tmp");
		List<Path> out = new ArrayList<>();
		for (String b : bases) {
			for (String sub : new String[]{"", "app/com.discordapp.Discord/", "snap.discord/"}) {
				for (int i = 0; i < 10; i++) out.add(Path.of(b, sub + "discord-ipc-" + i));
			}
		}
		return out;
	}

	private static boolean connect(String id) {
		boolean win = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
		if (win) {
			for (int i = 0; i < 10; i++) {
				try {
					RandomAccessFile f = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
					in = Channels.newInputStream(f.getChannel());
					out = Channels.newOutputStream(f.getChannel());
					closer = () -> { try { f.close(); } catch (IOException ignored) { } };
					if (handshake(id)) return true;
				} catch (Exception ignored) { close(); }
			}
			return false;
		}
		for (Path p : unixPaths()) {
			if (!Files.exists(p)) continue;
			try {
				SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
				ch.connect(UnixDomainSocketAddress.of(p));
				in = Channels.newInputStream(ch);
				out = Channels.newOutputStream(ch);
				closer = () -> { try { ch.close(); } catch (IOException ignored) { } };
				if (handshake(id)) return true;
			} catch (Exception ignored) { close(); }
		}
		return false;
	}

	private static boolean handshake(String id) throws IOException {
		JsonObject hello = new JsonObject();
		hello.addProperty("v", 1);
		hello.addProperty("client_id", id);
		send(0, hello.toString());
		String reply = read();
		connected = reply != null && reply.contains("READY");
		if (!connected) { close(); return false; }
		sent = null;
		return true;
	}

	private static String activityPayload(Activity a) {
		JsonObject act = new JsonObject();
		if (a.details() != null && !a.details().isEmpty()) act.addProperty("details", a.details());
		if (a.state() != null && !a.state().isEmpty()) act.addProperty("state", a.state());
		JsonObject ts = new JsonObject();
		ts.addProperty("start", a.since() / 1000);
		act.add("timestamps", ts);
		JsonObject assets = new JsonObject();
		assets.addProperty("large_image", "surge");
		assets.addProperty("large_text", "Surge Client");
		act.add("assets", assets);
		JsonArray buttons = new JsonArray();
		JsonObject b = new JsonObject();
		b.addProperty("label", "Get Surge Client");
		b.addProperty("url", gg.surge.client.net.SurgeNet.publicUrl());
		buttons.add(b);
		act.add("buttons", buttons);
		JsonObject args = new JsonObject();
		args.addProperty("pid", ProcessHandle.current().pid());
		args.add("activity", act);
		JsonObject cmd = new JsonObject();
		cmd.addProperty("cmd", "SET_ACTIVITY");
		cmd.add("args", args);
		cmd.addProperty("nonce", Long.toHexString(System.nanoTime()));
		return cmd.toString();
	}

	private static void send(int op, String json) throws IOException {
		byte[] body = json.getBytes(StandardCharsets.UTF_8);
		ByteBuffer head = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		head.putInt(op).putInt(body.length);
		out.write(head.array());
		out.write(body);
		out.flush();
	}

	private static String read() throws IOException {
		DataInputStream d = new DataInputStream(in);
		byte[] head = new byte[8];
		d.readFully(head);
		ByteBuffer hb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
		int op = hb.getInt();
		int len = hb.getInt();
		if (len < 0 || len > 1 << 20) throw new IOException("bad frame");
		byte[] body = new byte[len];
		d.readFully(body);
		if (op == 2) throw new IOException("closed");
		return new String(body, StandardCharsets.UTF_8);
	}

	private static void close() {
		if (closer != null) closer.run();
		closer = null;
		in = null;
		out = null;
		sent = null;
		connected = false;
	}
}
