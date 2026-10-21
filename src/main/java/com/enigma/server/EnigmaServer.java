package com.enigma.server;

import com.enigma.core.EnigmaMachine;
import com.enigma.core.Plugboard;
import com.enigma.core.Reflector;
import com.enigma.core.ReflectorType;
import com.enigma.core.Rotor;
import com.enigma.core.KeyPressResult;
import com.enigma.core.RotorType;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 恩尼格玛 HTTP 服务
 * 接口:
 *   GET  /              前端页面 web/index.html
 *   GET  /api/state     当前配置与转子位置
 *   POST /api/config    应用配置(重装整机)
 *   POST /api/keypress  单键加密(转子步进 + 返回信号路径)
 * 全部加解密由真实 Java 恩尼格玛组件完成,前端只做可视化。
 * 同一 EnigmaMachine 实例即物理上的"一台机器":转子位置是状态,
 * 所有按键经 synchronized 串行化,保证多标签页共享同一机械进度。
 *
 * @author echo
 * @date 2026-10-19
 */
public class EnigmaServer {

    /** 默认监听端口 */
    private static final int DEFAULT_PORT = 8080;

    /** 前端页面路径 */
    private static final String WEB_ROOT = "web";

    /** HTTP 线程池核心数 */
    private static final int HTTP_CORE_POOL = 2;

    /** HTTP 线程池最大数 */
    private static final int HTTP_MAX_POOL = 4;

    /** HTTP 队列容量 */
    private static final int HTTP_QUEUE_CAPACITY = 128;

    /** 当前日钥配置(装机时整体替换) */
    private MachineConfig config;
    /** 当前整机实例(唯一"物理机器",按键经 synchronized 串行) */
    private EnigmaMachine machine;

    /**
     * 入口:可传端口参数,默认 8080
     *
     * @param args args[0] 为监听端口(可选)
     * @throws IOException 端口占用或绑定失败
     */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new EnigmaServer().start(port);
    }

    /**
     * 启动服务:默认配置 转子 I-II-III / AAA / AAA / 反射器 B / 无插线
     *
     * @param port 监听端口
     */
    public void start(int port) throws IOException {
        applyDefaultConfig();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::route);
        server.setExecutor(buildExecutor());
        server.start();
        System.out.println("恩尼格玛服务已启动: http://localhost:" + port);
    }

    /** 装入出厂默认配置:转子 I-II-III / AAA / 反射器 B / 无插线 */
    private void applyDefaultConfig() {
        config = new MachineConfig(
                new RotorType[] {RotorType.ROTOR_I, RotorType.ROTOR_II, RotorType.ROTOR_III},
                new int[] {0, 0, 0},
                new int[] {0, 0, 0},
                ReflectorType.REFLECTOR_B,
                new int[][] {});
        machine = config.buildMachine();
    }

    /**
     * 按 URL 路由分发
     */
    private void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            switch (path) {
                case "/":
                    serveIndex(exchange);
                    break;
                case "/api/state":
                    handleState(exchange);
                    break;
                case "/api/config":
                    handleConfig(exchange);
                    break;
                case "/api/keypress":
                    handleKeyPress(exchange);
                    break;
                default:
                    serveStaticOrDefault(exchange, path);
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "BAD_PARAM", e.getMessage(), "请检查输入参数");
        } catch (Exception e) {
            sendError(exchange, 500, "SERVER_ERROR", String.valueOf(e.getMessage()), "服务内部错误");
        } finally {
            exchange.close();
        }
    }

    /**
     * 返回前端单页
     */
    private void serveIndex(HttpExchange exchange) throws IOException {
        Path index = Path.of(WEB_ROOT, "index.html");
        if (!Files.exists(index)) {
            sendError(exchange, 500, "NO_PAGE", "缺少 web/index.html", "请在项目根目录启动服务");
            return;
        }
        byte[] body = Files.readAllBytes(index);
        send(exchange, 200, "text/html; charset=utf-8", body);
    }

    /**
     * 当前配置 + 实时转子位置
     */
    private void handleState(HttpExchange exchange) throws IOException {
        String json = "{\"code\":0,\"data\":" + stateJson() + "}";
        send(exchange, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 应用新配置:整台重装,转子回到初始位置
     * 参数:rotors=I,II,III&positions=AAA&rings=AAA&reflector=B&plugs=AB,CD
     */
    private void handleConfig(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange);
        MachineConfig parsed = parseConfig(params);
        synchronized (this) {
            config = parsed;
            machine = parsed.buildMachine();
        }
        String json = "{\"code\":0,\"data\":" + stateJson() + "}";
        send(exchange, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 单键:真实机械行为 —— 先步进后通电,返回灯、位置、信号路径
     */
    private void handleKeyPress(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange);
        String letter = params.get("letter");
        if (letter == null || letter.length() != 1) {
            throw new IllegalArgumentException("letter 参数必须为单个字母");
        }
        KeyPressResult result;
        synchronized (this) {
            result = machine.pressKey(letter.charAt(0));
        }
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"code\":0,\"data\":{\"lamp\":\"").append(result.lamp())
                .append("\",\"positions\":\"").append(result.rotorPositions())
                .append("\",\"path\":[");
        for (int i = 0; i < result.path().size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"stage\":\"").append(result.path().get(i).stage())
                    .append("\",\"letter\":\"").append(result.path().get(i).letter()).append("\"}");
        }
        sb.append("]}}");
        send(exchange, 200, "application/json; charset=utf-8",
                sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析配置参数并校验
     */
    private MachineConfig parseConfig(Map<String, String> params) {
        String[] rotorNames = required(params, "rotors").split(",");
        if (rotorNames.length != 3) {
            throw new IllegalArgumentException("rotors 必须为三个转子,逗号分隔");
        }
        RotorType[] order = new RotorType[3];
        for (int i = 0; i < 3; i++) {
            RotorType type = RotorType.ofName(rotorNames[i].trim());
            if (type == null) {
                throw new IllegalArgumentException("未知转子:" + rotorNames[i]);
            }
            order[i] = type;
        }
        if (order[0] == order[1] || order[1] == order[2] || order[0] == order[2]) {
            throw new IllegalArgumentException("三个转子型号不能重复");
        }
        int[] positions = parseLetters(required(params, "positions"), "positions");
        int[] rings = parseLetters(required(params, "rings"), "rings");
        ReflectorType reflector = ReflectorType.ofName(required(params, "reflector"));
        if (reflector == null) {
            throw new IllegalArgumentException("reflector 仅支持 B 或 C");
        }
        return new MachineConfig(order, positions, rings, reflector, parsePlugs(params.get("plugs")));
    }

    /**
     * 解析三字母参数(如 "ABL")为索引数组
     */
    private int[] parseLetters(String value, String label) {
        if (value == null || value.length() != 3) {
            throw new IllegalArgumentException(label + " 必须为三个 A-Z 字母");
        }
        int[] result = new int[3];
        for (int i = 0; i < 3; i++) {
            char c = Character.toUpperCase(value.charAt(i));
            if (c < 'A' || c > 'Z') {
                throw new IllegalArgumentException(label + " 含非法字母:" + value.charAt(i));
            }
            result[i] = c - 'A';
        }
        return result;
    }

    /**
     * 解析插线参数(如 "AB,CD"),空串/缺省视为无插线
     */
    private int[][] parsePlugs(String value) {
        if (value == null || value.isBlank()) {
            return new int[][] {};
        }
        String[] tokens = value.split("[,\\s]+");
        List<int[]> pairs = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (token.length() != 2) {
                throw new IllegalArgumentException("插线对格式错误:" + token);
            }
            int a = Character.toUpperCase(token.charAt(0)) - 'A';
            int b = Character.toUpperCase(token.charAt(1)) - 'A';
            if (a < 0 || a > 25 || b < 0 || b > 25 || a == b) {
                throw new IllegalArgumentException("插线对非法:" + token);
            }
            pairs.add(new int[] {a, b});
        }
        if (pairs.size() > Plugboard.MAX_PLUG_PAIRS) {
            throw new IllegalArgumentException("插线对最多 " + Plugboard.MAX_PLUG_PAIRS + " 对");
        }
        return pairs.toArray(new int[0][]);
    }

    /**
     * 当前状态 JSON(配置 + 实时位置)
     */
    private String stateJson() {
        MachineConfig cfg;
        String positions;
        synchronized (this) {
            cfg = config;
            positions = machine.rotorPositions();
        }
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"rotors\":[");
        for (int i = 0; i < 3; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"name\":\"").append(cfg.getRotorOrder()[i].displayName())
                    .append("\",\"notch\":\"").append(cfg.getRotorOrder()[i].notch())
                    .append("\",\"ring\":\"").append((char) ('A' + cfg.getRingSettings()[i]))
                    .append("\"}");
        }
        sb.append("],\"reflector\":\"").append(cfg.getReflectorType().displayName())
                .append("\",\"positions\":\"").append(positions)
                .append("\",\"plugs\":\"").append(plugsText(cfg)).append("\"}");
        return sb.toString();
    }

    /**
     * 插线对序列化为空格分隔文本(如 "AD CN"),用于状态回显
     *
     * @param cfg 机器配置
     * @return 插线文本,无插线返回空串
     */
    private String plugsText(MachineConfig cfg) {
        StringBuilder sb = new StringBuilder();
        for (int[] pair : cfg.getPlugPairs()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append((char) ('A' + pair[0])).append((char) ('A' + pair[1]));
        }
        return sb.toString();
    }

    /**
     * 取必填参数,缺失或空白抛 IllegalArgumentException(统一 400 响应)
     *
     * @param params 参数表
     * @param key    参数名
     * @return 参数值
     */
    private String required(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少参数:" + key);
        }
        return value;
    }

    /**
     * 解析 URL 查询串为键值对(UTF-8 解码)
     */
    private Map<String, String> parseQuery(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>(8);
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * 统一响应写出:设置类型与禁缓存头,写完自动关闭流
     *
     * @param exchange    HTTP 交换
     * @param status      状态码
     * @param contentType Content-Type
     * @param body        响应体
     * @throws IOException 写出失败
     */
    private void send(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        // 页面与状态接口禁缓存,避免浏览器滞留旧版页面/旧机器状态
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    /**
     * 静态资源分发:仅放行 /js/ 与 /lib/ 前缀,其余按 404 处理
     */
    private void serveStaticOrDefault(HttpExchange exchange, String path) throws IOException {
        if (path.startsWith("/js/") || path.startsWith("/lib/")) {
            serveStatic(exchange, path);
        } else {
            sendError(exchange, 404, "NOT_FOUND", "接口不存在:" + path, "请刷新页面");
        }
    }

    /**
     * 静态资源服务(限 web/js 与 web/lib 目录)
     * 安全校验:路径规范化后必须仍位于 web 根目录内,阻断 ".." 路径穿越
     */
    private void serveStatic(HttpExchange exchange, String path) throws IOException {
        Path root = Path.of(WEB_ROOT).toAbsolutePath().normalize();
        Path file = root.resolve(path.substring(1)).normalize();
        if (!file.startsWith(root)) {
            sendError(exchange, 403, "FORBIDDEN", "非法路径", "禁止访问");
            return;
        }
        if (!Files.exists(file) || Files.isDirectory(file)) {
            sendError(exchange, 404, "NOT_FOUND", "资源不存在:" + path, "请刷新页面");
            return;
        }
        String name = file.getFileName().toString();
        String type = name.endsWith(".js") ? "text/javascript; charset=utf-8"
                : name.endsWith(".css") ? "text/css; charset=utf-8"
                : name.endsWith(".html") ? "text/html; charset=utf-8"
                : "application/octet-stream";
        send(exchange, 200, type, Files.readAllBytes(file));
    }

    /**
     * 统一错误响应:HTTP 状态码 + errorCode + errorMessage + 用户提示
     */
    private void sendError(HttpExchange exchange, int status, String errorCode,
                           String errorMessage, String userTip) throws IOException {
        String json = "{\"errorCode\":\"" + errorCode + "\",\"errorMessage\":\""
                + errorMessage + "\",\"userTip\":\"" + userTip + "\"}";
        send(exchange, status, "application/json; charset=utf-8",
                json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 固定大小线程池,线程具名便于排查
     */
    private ThreadPoolExecutor buildExecutor() {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable task) {
                return new Thread(task, "enigma-http-" + seq.getAndIncrement());
            }
        };
        return new ThreadPoolExecutor(HTTP_CORE_POOL, HTTP_MAX_POOL, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(HTTP_QUEUE_CAPACITY), factory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
