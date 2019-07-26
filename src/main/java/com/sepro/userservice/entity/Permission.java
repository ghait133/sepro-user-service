package com.sepro.userservice.entity;


import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "PERMISSION")
public class Permission extends BaseIdEntity {
    @Column(name = "NAME")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
