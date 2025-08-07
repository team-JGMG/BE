package org.bobj.fcm.service;

import org.bobj.fcm.dto.request.FcmRequestDto;

import java.io.IOException;
import java.util.List;

public interface FcmService {
    void sendMessageTo(FcmRequestDto fcmRequestDto) throws IOException;

    void sendMulticast(List<String> tokens, String title, String body) throws IOException;
}

