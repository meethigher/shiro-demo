package com.example.demo.entity;

import lombok.Data;

@Data
public class Account {
    private Integer id;
    private String username;
    private String password;
    private String perms;
    private String role;
}
