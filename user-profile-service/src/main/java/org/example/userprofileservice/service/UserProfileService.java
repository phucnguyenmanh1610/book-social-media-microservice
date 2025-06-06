package org.example.userprofileservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.userprofileservice.dto.request.UserProfileChangeAvatarRequest;
import org.example.userprofileservice.dto.request.UserProfileCreationRequest;
import org.example.userprofileservice.dto.request.UserProfileUpdateRequest;
import org.example.userprofileservice.dto.response.*;
import org.example.userprofileservice.entity.UserProfile;
import org.example.userprofileservice.exception.ErrorCode;
import org.example.userprofileservice.exception.ServiceException;
import org.example.userprofileservice.mapper.HistoryRecordMapper;
import org.example.userprofileservice.mapper.UserProfileMapper;
import org.example.userprofileservice.repository.HistoryRecordClient;
import org.example.userprofileservice.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserProfileService {
    UserProfileRepository userProfileRepository;
    UserProfileMapper userProfileMapper;
    HistoryRecordClient historyRecordClient;
    HistoryRecordMapper recordMapper;

    public UserProfileCreationResponse createUserProfile(UserProfileCreationRequest request) {
        if (userProfileRepository.existsUserProfileByUserId(request.getUserId())) {
            throw new ServiceException(ErrorCode.USER_EXISTED);
        }
        UserProfile userProfile = userProfileMapper.toUserProfile(request);
        return userProfileMapper.toUserProfileCreationResponse(userProfileRepository.save(userProfile));

    }

    public UserProfileUpdateResponse updateUserProfile(UserProfileUpdateRequest request) {
        if (!userProfileRepository.existsUserProfileByUserId(request.getId())) {
            throw new ServiceException(ErrorCode.USER_NOT_EXISTED);
        }
        var userProfile = userProfileRepository.findUserProfileByUserId(request.getId());
        userProfile.setName(request.getNewName());
        userProfileRepository.save(userProfile);
        return UserProfileUpdateResponse.builder()
                .id(userProfile.getId())
                .newName(userProfile.getName())
                .build();
    }

    public UserProfileChangeAvatarResponse changeAvatar(UserProfileChangeAvatarRequest request) {
        if (!userProfileRepository.existsUserProfileByUserId(request.getId())) {
            throw new ServiceException(ErrorCode.USER_NOT_EXISTED);
        }
        var userProfile = userProfileRepository.findUserProfileByUserId(request.getId());
        userProfile.setAvatarUrl(request.getNewAvatarUrl());
        return UserProfileChangeAvatarResponse.builder()
                .newAvatarUrl(userProfile.getAvatarUrl())
                .id(userProfile.getId())
                .build();
    }

    public UserProfileResponse getUserProfileByUserId(String id) {

        if (!userProfileRepository.existsUserProfileByUserId(id)) {
            throw new ServiceException(ErrorCode.USER_NOT_EXISTED);
        }
        var userProfile = userProfileRepository.findUserProfileByUserId(id);
        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    public List<UserReadingHistory> getUserReadingHistory(String userId) {
        var readingHistoryResponse = historyRecordClient.getUserReadingHistory(userId).getResult();
        return readingHistoryResponse.stream().map(recordMapper::toUserReadingHistory).collect(Collectors.toList());
    }

    public UserProfileDeleteResponse deleteUserProfile(String userId) {
        if (!userProfileRepository.existsUserProfileByUserId(userId)) {
            throw new ServiceException(ErrorCode.USER_NOT_EXISTED);
        }

        var userProfile = userProfileRepository.findUserProfileByUserId(userId);
        userProfileRepository.delete(userProfile);

        return UserProfileDeleteResponse.builder()
                .id(userProfile.getId())
                .userId(userProfile.getUserId())
                .name(userProfile.getName())
                .avatarUrl(userProfile.getAvatarUrl())
                .build();
    }


}