package com.mysticcard.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.mysticcard.backend.dto.auth.AuthResponse;
import com.mysticcard.backend.entity.User;
import com.mysticcard.backend.entity.UserAstrologyProfile;
import com.mysticcard.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public AuthResponse verifyTokenAndLoginOrRegister(String firebaseTokenString) {
        try {
            // Verify token with Firebase Admin
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseTokenString);

            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();

            log.info("Verified user from Firebase: UID={}, Email={}", uid, email);

            // Check if user exists
            Optional<User> existingUser = userRepository.findByFirebaseUid(uid);

            if (existingUser.isPresent()) {
                // User already exists
                User user = existingUser.get();
                // Optionally update avatar or name if they changed
                boolean updated = false;
                if (name != null && !name.equals(user.getDisplayName())) {
                    user.setDisplayName(name);
                    updated = true;
                }
                if (picture != null && !picture.equals(user.getAvatarUrl())) {
                    user.setAvatarUrl(picture);
                    updated = true;
                }

                if (updated) {
                    user = userRepository.save(user);
                }

                // Check if profile is missing
                boolean needsProfile = (user.getFirstName() == null || user.getFirstName().trim().isEmpty() || user.getAstrologyProfile() == null);

                return AuthResponse.builder()
                        .isNewUser(false)
                        .needsProfile(needsProfile)
                        .user(user)
                        .build();
            } else {
                // New User Registration
                log.info("Registering new user: UID={}", uid);
                User newUser = User.builder()
                        .firebaseUid(uid)
                        .email(email)
                        .displayName(name != null ? name : email.split("@")[0])
                        .avatarUrl(picture)
                        .role("USER")
                        .preferredLanguage("th")
                        .coinBalance(0)
                        .build();

                User savedUser = userRepository.save(newUser);

                return AuthResponse.builder()
                        .isNewUser(true)
                        .needsProfile(true) // Always true for new users
                        .user(savedUser)
                        .build();
            }

        } catch (FirebaseAuthException e) {
            log.error("Firebase Auth Error: ", e);
            throw new RuntimeException("Invalid Firebase Token", e);
        }
    }

    @Transactional
    public UserAstrologyProfile saveProfile(String firebaseTokenString, com.mysticcard.backend.dto.auth.ProfileRequest request) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseTokenString);
            String uid = decodedToken.getUid();

            User user = userRepository.findByFirebaseUid(uid)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update User entity
            if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
            if (request.getLastName() != null) user.setLastName(request.getLastName());
            userRepository.save(user);

            // Update or Create Astrology Profile
            UserAstrologyProfile profile = user.getAstrologyProfile();
            if (profile == null) {
                profile = new UserAstrologyProfile();
                profile.setUser(user);
                // JPA mapsId will set the ID automatically based on user
            }

            if (request.getBirthDate() != null) profile.setBirthDate(request.getBirthDate());
            if (request.getBirthTime() != null) profile.setBirthTime(request.getBirthTime());
            if (request.getZodiac() != null) profile.setZodiacSign(request.getZodiac());
            if (request.getElement() != null) profile.setElement(request.getElement());

            // Save via user cascade or explicitly (both work, explicit is safer here if not managed)
            user.setAstrologyProfile(profile);
            return profile; // cascade save will handle it on commit
        } catch (FirebaseAuthException e) {
            log.error("Firebase Auth Error during profile save: ", e);
            throw new RuntimeException("Invalid Firebase Token", e);
        }
    }
}
