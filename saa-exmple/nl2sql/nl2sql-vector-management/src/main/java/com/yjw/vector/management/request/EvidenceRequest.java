package com.yjw.vector.management.request;

import java.io.Serializable;

public class EvidenceRequest implements Serializable {
    private String content;
    private Integer type;



    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
