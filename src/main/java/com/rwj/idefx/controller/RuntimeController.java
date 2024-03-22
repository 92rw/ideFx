package com.rwj.idefx.controller;

import com.rwj.idefx.model.ExecutionResult;
import com.rwj.idefx.model.FileModel;
import com.rwj.idefx.model.OSType;
import javafx.application.Platform;


import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;


public class RuntimeController {
    private static OSType os = getOperatingSystem();
    private static Process currentProcess = null;
    private static ProcessBuilder processBuilder = new ProcessBuilder();
    private static String javaPath = "java";

    public static OSType getOperatingSystem() {
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
        List<String> command;
        if (os == OSType.WINDOWS) {
            command = Arrays.asList(
                    "cmd", "/C",
                    "cd", projectPath, "&",
                    "dir", "*.java", "/s", "/b", ">", projectPath + "/.list",
                    "&", javaPath, "-s", projectPath, "-d", projectPath + "/out", "@" + projectPath + "/.list"
            );
        } else {
            command = Arrays.asList(
                    "bash", "-c",
                    "find", projectPath + "/src", "-name", "'*.java'", "|",
                    "xargs", javaPath, "-s", projectPath, "-d", projectPath + "/out"
            );
        }

        Process process = runCommand(false, command);

        int exitCode = process.waitFor();
        if (os == OSType.WINDOWS) {
            runCommand(false,"cmd", "/C", "del", projectPath + "/.list");
        }
        return exitCode == 0;
    }

    private static Process runCommand(boolean mergeErrorStream, String... command) {
        try {
            List<String> fullCommand = new ArrayList<>(command.length + 1);
            fullCommand.add(javaPath);
            fullCommand.addAll(Arrays.asList(command));

            processBuilder.command(fullCommand);
            processBuilder.redirectErrorStream(mergeErrorStream);
            return processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Process runCommand(boolean mergeErrorStream, List<String> fullCommand) {
        try {
            processBuilder.command(fullCommand);
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

//        String className = packageName.isEmpty() ? filename.substring(0, filename.length() - ".java".length())
//                : packageName + "." + mainClass.fileName().replace(".java", "");
        String relativePath = file.getAbsolutePath().substring(projectPath.length() + 1);
        String classFileName = relativePath.substring(0, relativePath.length() - ".java".length()).replace('\\', '/');

        String className;
        if (packageName.isEmpty()) {
            className = classFileName;
        } else {
            className = packageName + "." + classFileName;
        }
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
                    return line.substring(8, line.length() - 1).trim();
                }
            }
        }
        return "";
    }

    public static void setJavaPath(String path) {
        javaPath = path;
    }
}
