package com.yjw.parallel.stream.model;



public enum NodeStatus {

    RUNNING("running", "运行中"),

    COMPLETED("completed", "已完成"),

    FAILED("failed", "失败");

    String code;

    String desc;

    NodeStatus(String running, String desc) {
        this.code = running;
        this.desc = desc;
    }

}
