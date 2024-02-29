package com.rwj.idefx.controller;

import com.rwj.idefx.model.ExecutionResult;
import com.rwj.idefx.model.FileModel;
import com.rwj.idefx.model.OSType;
import javafx.application.Platform;


import java.io.*;

import java.util.Scanner;
import java.util.function.Consumer;


public class RuntimeController {
    private static OSType os = getOperatingSystem();
    private static Process currentProcess = null;

    private static String javaPath = "java";


    private static OSType getOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OSType.WINDOWS;
        } else{
            return OSType.UNIX;
        }
    }

    public static String getJVMInfo() {
        Process process = runCommand(true,javaPath, "-version");
        if (process == null) return "Fail to load JVM Info, please check your Project Config";
        return streamToString(process.getInputStream());
    }

    public static String deCompile(String classFilePath) {
        Process process = runCommand(false,"javap", "-c", classFilePath);
        if (process == null) return "";
        return streamToString(process.getInputStream());
    }

    public static boolean buildProject(String projectPath) throws InterruptedException {
        String[] command;
        if (os == OSType.WINDOWS) {
            command = new String[]{
                    "cmd", "/C",
                    "cd", projectPath, "&",
                    "dir", "*.java", "/s", "/b", ">", projectPath + "/.list",
                    "&", "javac", "-s", projectPath, "-d", projectPath + "/out", "@" + projectPath + "/.list"
            };
        } else {
            command = new String[]{
                    "bash", "-c",
                    "find", projectPath + "/src", "-name", "'*.java'", "|",
                    "xargs", "javac", "-s", projectPath, "-d", projectPath + "/out"
            };
        }

        Process process = runCommand(false,command);
        int exitCode = process.waitFor();
        if (os == OSType.WINDOWS) {
            runCommand(false,"cmd", "/C", "del", projectPath + "/.list");
        }
        return exitCode == 0;
    }

    private static Process runCommand(boolean mergeErrorStream, String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(mergeErrorStream);
            return processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static ExecutionResult executeCode(String projectPath, FileModel mainClass, Consumer<String> redirect){
        String filename = mainClass.fileName();
        if (!filename.endsWith(".java")) return new ExecutionResult(-1, "Only can run java file");

        File file = new File(mainClass.filePath());
        String packageName = null;
        try {
            packageName = getPackageName(file);
        } catch (IOException e) {
            return new ExecutionResult(-1, "Fail to read java file");
        }
        String className = packageName.isEmpty() ? filename.substring(0, filename.length() - ".java".length())
                : packageName + "." + mainClass.fileName().replace(".java", "");

        String classPath = projectPath + "/out";

        currentProcess = runCommand(true, javaPath, "-cp", classPath , className);
        new Thread(() -> {
            try (Scanner scanner = new Scanner(currentProcess.getInputStream())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Platform.runLater(() -> redirect.accept(line + "\n"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> redirect.accept("Error reading process output: " + e.getMessage() + "\n"));
            }
        }).start();

        try {
            int exitCode = currentProcess.waitFor();
            return new ExecutionResult(exitCode, "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Properly handle thread interruption
            return new ExecutionResult(-1, "Execution was interrupted");
        } finally {
            synchronized (RuntimeController.class) {
                // Ensure the reference to the current process is cleared
                currentProcess = null;
            }
        }
    }

    public static void stopProcess(){
        synchronized (RuntimeController.class) {   //多线程控制，防止并发修改
            if(currentProcess != null)
                currentProcess.destroyForcibly();
        }
    }
    public static void redirectToProcess(String input){
        synchronized (RuntimeController.class) {   //多线程控制，防止并发修改
            if(currentProcess != null) {
                OutputStream outputStream = currentProcess.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);
                try {
                    writer.println(input); // 写入输入，确保包含换行符
                    writer.flush(); // 刷新流，确保数据被发送
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private static String streamToString(InputStream inputStream){
        StringBuilder stringBuilder = new StringBuilder();
        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine()).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private static String getPackageName(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("package")) {
                    // 格式通常为 "package com.example.package;"
                    return line.substring(8, line.length() - 1).trim(); // 移除"package "和";"
                }
            }
        }
        return ""; // 如果没有找到包声明，默认包（无包名）
    }

}
