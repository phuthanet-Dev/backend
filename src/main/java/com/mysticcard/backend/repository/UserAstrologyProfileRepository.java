package com.mysticcard.backend.repository;

import com.mysticcard.backend.entity.UserAstrologyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserAstrologyProfileRepository extends JpaRepository<UserAstrologyProfile, UUID> {
}
