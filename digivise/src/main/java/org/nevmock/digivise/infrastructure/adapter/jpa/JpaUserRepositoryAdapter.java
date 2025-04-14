package org.nevmock.digivise.infrastructure.adapter.jpa;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.domain.model.User;
import org.nevmock.digivise.domain.port.out.UserRepository;
import org.nevmock.digivise.infrastructure.jpa.UserJpaRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public void save(User user) {
        jpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return jpaRepository.findById(userId);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID userId) {
        jpaRepository.deleteById(userId);
    }
}
