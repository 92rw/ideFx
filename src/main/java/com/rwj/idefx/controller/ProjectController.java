package com.rwj.idefx.controller;

import com.rwj.idefx.model.ProjectConfig;

public class ProjectController {
    public static boolean createProject(ProjectConfig configure){
//        File dir = new File(configure.getPath() + "/" +configure.getName());
//        if(dir.exists() || dir.mkdirs()) {
//            File src = new File(dir.getAbsolutePath() + "/src");
//            createProjectConfigure(configure);
//            ProjectManager.createProject(configure.getName(), configure.getPath());
//            if(!src.mkdir()) return false;
//            if(configure.hasDefaultCode()) {
//                File defaultCodeFile = new File(dir.getAbsolutePath() + "/src/Main.java");
//                try(FileWriter writer = new FileWriter(defaultCodeFile)) {
//                    writer.write(defaultMainCode());
//                    return true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return false;
//                }
//            }
//            return true;
//        } else {
//            return false;
//        }
        System.out.println(configure.toString());
        return true;
    }
}
