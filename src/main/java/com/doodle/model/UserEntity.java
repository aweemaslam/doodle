package com.doodle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Core user identity master-data entity.
 * Uses a natural primary key string to streamline join lookups across scheduling tables.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity extends BaseEntity {

    @Id
    @Column(name = "email", nullable = false, length = 50)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "default_timezone", nullable = false, length = 64)
    private String defaultTimezone;
}