package com.doodle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity extends BaseEntity implements Serializable {

    @Id
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "default_timezone", nullable = false)
    private String defaultTimezone;
}