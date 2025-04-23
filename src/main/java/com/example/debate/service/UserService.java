package com.example.debate.service;

import com.example.debate.dto.KafkaUserDto;
import com.example.debate.entity.User;
import com.example.debate.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics="updateUser", groupId = "eum")
    public void updateUser(String message){
        KafkaUserDto kafkaUserDto;
        try{
            kafkaUserDto = objectMapper.readValue(message ,KafkaUserDto.class);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        User user = userRepository.findById(kafkaUserDto.getUserId()).get();
        user.setName(kafkaUserDto.getName());
        user.setNation(kafkaUserDto.getNation());
        user.setLanguage(kafkaUserDto.getLanguage());
        user.setRole(kafkaUserDto.getRole());
        user.setAddress(kafkaUserDto.getAddress());
        userRepository.save(user);
    }

    @KafkaListener(topics="updateLanguage", groupId = "eum")
    public void updateLanguage(String message){
        KafkaUserDto kafkaUserDto;
        try{
            kafkaUserDto = objectMapper.readValue(message ,KafkaUserDto.class);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        User user = userRepository.findById(kafkaUserDto.getUserId()).get();
        user.setLanguage(kafkaUserDto.getLanguage());
        userRepository.save(user);
    }

    @KafkaListener(topics="createUser", groupId = "eum")
    public void createUser(String message){
        KafkaUserDto kafkaUserDto;
        try{
            kafkaUserDto = objectMapper.readValue(message ,KafkaUserDto.class);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }

        User user = new User();
        user.setUserId(kafkaUserDto.getUserId());
        user.setName(kafkaUserDto.getName());
        user.setNation(kafkaUserDto.getNation());
        user.setLanguage(kafkaUserDto.getLanguage());
        user.setRole(kafkaUserDto.getRole());
        user.setAddress(kafkaUserDto.getAddress());
        userRepository.save(user);
    }

    @Transactional
    @KafkaListener(topics="deleteUser", groupId = "eum")
    public void deleteUser(String message){
        KafkaUserDto kafkaUserDto;
        try{
            kafkaUserDto = objectMapper.readValue(message ,KafkaUserDto.class);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        User user = userRepository.findById(kafkaUserDto.getUserId()).get();
        userRepository.delete(user);
    }
}
