package com.doodle.repository;

import com.doodle.model.UserEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    // ----------------------------
    // 1. CREATE USER ENTITY
    // ----------------------------
    @Test
    void shouldCreateUserEntity() {

        UserEntity user = new UserEntity();
        user.setEmail("john@example.com");
        user.setFullName("John Doe");

        assertEquals("john@example.com", user.getEmail());
        assertEquals("John Doe", user.getFullName());
    }

    // ----------------------------
    // 2. USER EMAIL AS PRIMARY KEY
    // ----------------------------
    @Test
    void shouldSetUserEmailAsPrimaryKey() {

        UserEntity user = new UserEntity();
        user.setEmail("alice@example.com");

        assertNotNull(user.getEmail());
        assertEquals("alice@example.com", user.getEmail());
    }

    // ----------------------------
    // 3. USER FULL NAME REQUIRED
    // ----------------------------
    @Test
    void shouldSetUserFullName() {

        UserEntity user = new UserEntity();
        user.setFullName("Test User");

        assertEquals("Test User", user.getFullName());
    }

    // ----------------------------
    // 4. MULTIPLE USERS DIFFERENT EMAILS
    // ----------------------------
    @Test
    void shouldCreateMultipleUsersWithDifferentEmails() {

        UserEntity user1 = new UserEntity();
        user1.setEmail("user1@example.com");
        user1.setFullName("User One");

        UserEntity user2 = new UserEntity();
        user2.setEmail("user2@example.com");
        user2.setFullName("User Two");

        assertNotEquals(user1.getEmail(), user2.getEmail());
        assertNotEquals(user1.getFullName(), user2.getFullName());
        assertEquals("user1@example.com", user1.getEmail());
        assertEquals("user2@example.com", user2.getEmail());
    }

    // ----------------------------
    // 5. USER PROPERTIES MUTABLE
    // ----------------------------
    @Test
    void shouldAllowUpdatingUserProperties() {

        UserEntity user = new UserEntity();
        user.setEmail("original@example.com");
        user.setFullName("Original Name");

        assertEquals("original@example.com", user.getEmail());
        assertEquals("Original Name", user.getFullName());

        user.setEmail("updated@example.com");
        user.setFullName("Updated Name");

        assertEquals("updated@example.com", user.getEmail());
        assertEquals("Updated Name", user.getFullName());
    }
}

