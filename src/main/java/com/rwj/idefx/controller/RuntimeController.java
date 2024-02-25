package com.rwj.idefx.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RuntimeController {
    public static String Decompile(String classPath, String fullClassName) throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();
        try {

            Process process = new ProcessBuilder("javap", "-c", "-p", "-v", "-classpath", classPath, fullClassName).start();
            // 读取正常输出
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = inputReader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 读取错误输出
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitVal = process.waitFor();
            if (exitVal != 0) {
                return "javap exited with non-zero code " + exitVal + "\n" + output.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return output.toString();
    }
}
