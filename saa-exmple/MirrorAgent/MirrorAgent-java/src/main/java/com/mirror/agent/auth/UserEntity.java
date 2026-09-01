package com.mirror.agent.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 使用自增主键
    private Long id;

    @Column(unique = true,nullable = false,length = 50)
    private String username;

    @Column(nullable = false)
    private String password; // bcrypt 哈希

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * **实体第一次插入数据库（persist）之前，JPA 框架自动调用这个方法**。
     * 只在**新增插入**触发，**更新 update 的时候不会执行**。
     *
     * protected 推荐写法，开源 JPA 项目通用习惯 `private`：反射也可以调用，但是部分旧 JPA 实现会有兼容性小坑；
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
