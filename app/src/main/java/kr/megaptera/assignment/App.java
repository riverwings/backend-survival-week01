package kr.megaptera.assignment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

public class App {
    public static Long newId = 0L;
    // 왜 int가 아닌 Long으로 선언했을까? 큰 수를 표현하려고?

    public static void main(String[] args) throws IOException {
        App app = new App();
        app.run();
    }

    private void run() throws IOException {
        int port = 8080;

        Map<Long, String> tasks = new HashMap<>();

        // TODO: 요구사항에 맞게 과제를 진행해주세요.

        // 1. Listen
        ServerSocket listener = new ServerSocket(8080, 0);

        System.out.println("Listen!");

        while (true) {
            // 2. Accept
            Socket socket = listener.accept();

            System.out.println("Accept!");

            // 3. Request -> 처리 -> Response
            Reader reader = new InputStreamReader(socket.getInputStream());

            CharBuffer charBuffer = CharBuffer.allocate(1_000_000);
            reader.read(charBuffer);

            charBuffer.flip();
            String request = charBuffer.toString();
            System.out.println("request: " + request);

            // 4. Response

            String message = process(tasks, request);

            Writer writer = new OutputStreamWriter(socket.getOutputStream());
            writer.write(message);
            writer.flush();

            // 5. close

            socket.close();
        }
    }
    private String getRequestMethod(String request) {
        return request.substring(0, request.indexOf("HTTP"));
    }

    private String process(Map<Long, String> tasks, String request) {
        String method = getRequestMethod(request);

        if (method.startsWith("GET /tasks")) {
            return processGetTask(tasks);
        }

        if (method.startsWith("POST /tasks")) {
            return processPostTask(tasks, request);
        }

        if (method.startsWith("PATCH /tasks/")) {
            return processPatchTask(tasks, request, method);
        }

        if (method.startsWith("DELETE /tasks/")) {
            return processDeleteTask(tasks, method);
        }

        return generateMessage("", "400 Bad Request");
        // 오류 번호에 대해 다시 정리해보자.
    }

    private String generateMessage(String body, String statusCode) {
        byte[] bytes = body.getBytes();

        return "" +
                "HTTP/1.1 " + statusCode + "\n" +
                "Host: localhost:8080\n" +
                "Content-Length: " + bytes.length + "\n" +
                "Content-Type: application/json; charset=UTF-8\n" +
                "\n" +
                body;
    }

    private String processGetTask(Map<Long, String> tasks) {
        String content = new Gson().toJson(tasks);

        return generateMessage(content, "200 OK");
    }

    private String processPostTask(Map<Long, String> tasks, String request) {
        String task = parsePayload(request, "task");

        if (task.equals("")) {
            return generateMessage("", "400 Bad Request");
        }

        tasks.put(generateTaskId(), task);
        String content = new Gson().toJson(tasks);

        return generateMessage(content, "201 Created");
    }

    private String processPatchTask(Map<Long, String> tasks, String request, String method) {
        Long id = parseTaskId(method);

        if (!tasks.containsKey(id)) {
            return generateMessage("", "404 Not Found");
        }

        String task = parsePayload(request, "task");

        if (task.equals("")) {
            return generateMessage("", "400 Bad Request");
        }

        tasks.put(id, task);
        String content = new Gson().toJson(tasks);

        return generateMessage(content, "200 OK");
    }

    private String processDeleteTask(Map<Long, String> tasks, String method) {
        Long id = parseTaskId(method);

        if (!tasks.containsKey(id)) {
            return generateMessage("", "404 Not Found");
        }

        tasks.remove(id);
        String content = new Gson().toJson(tasks);

        return generateMessage(content, "200 OK");
    }

    private String parsePayload(String request, String value) {
        String[] lines = request.split("\n");
        String lastLine = lines[lines.length - 1];

        try {
            JsonElement jsonElement = JsonParser.parseString(lastLine);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            return jsonObject.get(value).getAsString();
        } catch (Exception e) {
            return "";
        }
    }
    // task="강 의 듣 기 " 부분

    private Long generateTaskId() {
        newId += 1;
        return newId;
    }

    private Long parseTaskId(String method) {
        String[] parts = method.split("/");

        return Long.parseLong(parts[2].trim());
    }

}
