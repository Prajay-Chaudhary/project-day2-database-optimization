package com.exo1.exo1.service;

import com.exo1.exo1.dto.UserDto;
import com.exo1.exo1.entity.User;
import com.exo1.exo1.mapper.UserMapper;
import com.exo1.exo1.repository.ProjetRepository;
import com.exo1.exo1.repository.TaskRepository;
import com.exo1.exo1.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.Collections;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjetRepository projetRepository;
    private final UserMapper userMapper;

    public Page<UserDto> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toDto);
    }

    public UserDto findById(long id) {
        return userMapper.toDto(userRepository.findByIdWithTask(id).orElse(null));
    }

    private void setupUserProjectsAndTasks(User user) {
        user.getProjets().forEach(projet -> {
            projet.setUsers(Collections.singleton(user));
            projet.getTasks().forEach(task -> task.setProjet(projet));
        });
    }

    public UserDto save(UserDto userDto) {
        User user = userMapper.toEntity(userDto);
        setupUserProjectsAndTasks(user);
        return userMapper.toDto(userRepository.save(user));
    }

    @CacheEvict(value = "users", key = "#user.id")
    public UserDto update(Long id, UserDto userDto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id " + id));
        userDto.setId(existingUser.getId());
        User userUpdated = userMapper.toEntity(userDto);

        userUpdated.getProjets().forEach(projet -> {
            projetRepository.findById(projet.getId()).ifPresent(existingProjet -> {
                existingProjet.setUsers(Collections.singleton(userUpdated));
                existingProjet.getTasks().forEach(task -> {
                    taskRepository.findById(task.getId()).ifPresent(t -> t.setProjet(existingProjet));
                });
            });
        });

        return userMapper.toDto(userRepository.save(userUpdated));
    }

    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}