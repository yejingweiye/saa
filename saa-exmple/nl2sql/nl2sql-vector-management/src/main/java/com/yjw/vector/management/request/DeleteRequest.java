
package com.yjw.vector.management.request;


import java.io.Serializable;

public class DeleteRequest implements Serializable {
    private String id;
    private String vectorType;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVectorType() {
        return vectorType;
    }

    public void setVectorType(String vectorType) {
        this.vectorType = vectorType;
    }


}
