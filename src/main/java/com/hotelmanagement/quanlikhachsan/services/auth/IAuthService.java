package com.hotelmanagement.quanlikhachsan.services.auth;

import com.hotelmanagement.quanlikhachsan.dto.request.auth.LoginRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.AuthResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.UserInfo;

public interface IAuthService {
    AuthResponse login(LoginRequest request);
    UserInfo getCurrentUser(String token);
}
