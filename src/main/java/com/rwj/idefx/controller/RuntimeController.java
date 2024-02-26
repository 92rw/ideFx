package com.rwj.idefx.controller;

import com.rwj.idefx.model.ExecutionResult;
import com.rwj.idefx.model.OSType;

import java.io.*;

import java.util.function.Consumer;


public class RuntimeController {
    private static OSType os = getOperatingSystem();
    private static Process currentProcess = null;


    private static OSType getOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OSType.WINDOWS;
        } else{
            return OSType.UNIX;
        }
    }

    public static String deCompile(String classFilePath) {
        Process process = runCommand("javap", "-c", classFilePath);
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

        Process process = runCommand(command);
        int exitCode = process.waitFor();
        if (os == OSType.WINDOWS) {
            runCommand("cmd", "/C", "del", projectPath + "/.list");
        }
        return exitCode == 0;
    }

    public static Process runCommand(String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            return processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static ExecutionResult executeCode(String projectPath, String javaCommand, String mainClass, Consumer<String> redirect){
        try {
            currentProcess = runCommand(javaCommand, "-cp", projectPath + "/out", mainClass);

            if(currentProcess == null) return new ExecutionResult(-1, "未知错误");
            InputStreamReader reader = new InputStreamReader(currentProcess.getInputStream());
            char[] chars = new char[1024];
            int len;
            while ((len = reader.read(chars)) > 0)
                redirect.accept(new String(chars, 0, len));
            int code = currentProcess.waitFor();
            return new ExecutionResult(code, streamToString(currentProcess.getErrorStream()));
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            synchronized (RuntimeController.class) {    //多线程控制，防止并发修改
                currentProcess = null;
            }
        }
        return new ExecutionResult(-1, "未知错误");
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
                OutputStream stream = currentProcess.getOutputStream();
                try {
                    stream.write(input.getBytes());
                    stream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private static String streamToString(InputStream inputStream){
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

}
