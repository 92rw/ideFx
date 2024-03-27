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
        Process process = null;
        try {
            process = runCommand(true,javaPath, "-version");
        } catch (IOException e) {
            return "Fail to load JVM Info, please check your Project Config";
        }
        String currentJavaFile = javaPath.equals("java") ?  "%JAVA_HOME%\\bin\\java.exe" : javaPath + ".exe";
        return streamToString("Current java File Config: " + currentJavaFile + "\n", process.getInputStream());
    }

    public static String deCompile(String classFilePath) {
        Process process = null;
        try {
            process = runCommand(false,javaPath + "p", "-c", classFilePath);
        } catch (IOException e) {
            return "Can't have access to \"javap\" in your configured javaPath, please check and retry.";
        }
        return streamToString("", process.getInputStream());
    }

    public static ExecutionResult buildProject(String projectPath) {
        String[] command;
        if (isWindowsOS()) {
            command = new String[]{
                    "cmd", "/C",
                    "cd", projectPath, "&",
                    "dir", "*.java", "/s", "/b", ">", projectPath + "/.list",
                    "&", javaPath + "c", "-s", projectPath, "-d", projectPath + "/out", "@" + projectPath + "/.list"
            };
        } else {
            command = new String[]{
                    "bash", "-c",
                    "find", projectPath + "/src", "-name", "'*.java'", "|",
                    "xargs",javaPath + "c", "-s", projectPath, "-d", projectPath + "/out"
            };
        }

        Process process = null;
        try {
            process = runCommand(false,command);
        } catch (IOException e) {
            return new ExecutionResult(-1, "Can't have access to \"javac\" in your configured javaPath, please check and retry.");
        }

        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            return new ExecutionResult(-1, "InterruptedException happens when handling your files.");
        }

        if (isWindowsOS()) {
            try {
                runCommand(false,"cmd", "/C", "del", projectPath + "/.list");
            } catch (IOException ignored) {}
        }
        return new ExecutionResult(exitCode,"");
    }

    private static Process runCommand(boolean mergeErrorStream, String... command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(mergeErrorStream);
        return processBuilder.start();
    }
    public static ExecutionResult executeCode(String projectPath, FileModel mainClass, Consumer<String> redirect){
        String filename = mainClass.fileName();
        if (!filename.endsWith(".java")) return new ExecutionResult(-1, "Only can run java file");

        File file = new File(mainClass.filePath());
        String packageName;
        try {
            packageName = getPackageName(file);
        } catch (IOException e) {
            return new ExecutionResult(-1, "Fail to read java file");
        }
        String className = packageName.isEmpty() ? filename.substring(0, filename.length() - ".java".length())
                : packageName + "." + mainClass.fileName().replace(".java", "");

        String classPath = projectPath + "/out";

        try {
            currentProcess = runCommand(true, javaPath, "-cp", classPath , className);
        } catch (IOException e) {
            return new ExecutionResult(-1, "IOException happens with your javaPath configuration");
        }
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
                } catch (Exception ignored) {
                }
            }
        }
    }
    private static String streamToString(String defaultString, InputStream inputStream){
        StringBuilder stringBuilder = new StringBuilder(defaultString);
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


    public static void setJavaPath(String path) {
        javaPath = path;
    }

    public static boolean isWindowsOS() {
        return os == OSType.WINDOWS;
    }

    public static File getJavaExecutable(File file) {
        if (isWindowsOS()) {
            // 在 Windows 上,我们需要检查文件是否为 java.exe
            if (file.getName().equals("java.exe")) {
                return file;
            }
        } else {
            // 在 Unix 系统上,我们只需要检查文件是否可执行
            if (file.canExecute()) {
                return file;
            }
        }
        return null;
    }

}
