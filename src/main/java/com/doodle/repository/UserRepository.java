package com.doodle.repository;

import com.doodle.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer managing user core master profile data records.
 * Interacts directly with the primary natural string key index for immediate evaluations.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    /**
     * Resolves an active user profile entity leveraging their natural email address identifier.
     *
     * @param email The target email address string.
     * @return An Optional wrapper containing the matched active User entity.
     */
    Optional<UserEntity> findByEmailAndActiveTrue(String email);

    /**
     * Checks database index records to instantly assert user visibility status.
     *
     * @param email The target email address string.
     * @return True if an active user profile exists with this email address.
     */
    boolean existsByEmailAndActiveTrue(String email);
}
